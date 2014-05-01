package com.dikei

import akka.actor.{Props, Actor}
import java.net.Socket
import akka.event.Logging
import org.apache.http.impl.io._
import org.apache.http.config.MessageConstraints
import scala.util.{Failure, Success, Try}
import dispatch._, Defaults._
import org.apache.http.message.BasicHttpResponse
import scala.collection.JavaConversions._
import org.apache.http._
import org.apache.http.impl.{DefaultBHttpServerConnection, DefaultBHttpClientConnection}
import scala.util.Success
import scala.util.Failure
import org.apache.http.entity.InputStreamEntity

case object StartProcessing

object Forwarder {
  def props(clientSocket: Socket) = Props(classOf[Forwarder], clientSocket)
}

/**
 * Forwarder receive request from receiver to process
 */
class Forwarder(val clientSocket: Socket) extends Actor{

  val logger = Logging(context.system, this)

  val bufferSize = 512 * 1024
  val connection = new DefaultBHttpServerConnection(bufferSize)
  connection.bind(clientSocket)

  override def receive: Receive = {
    case StartProcessing =>
      process()
  }

  def process() = {
    val request = Try {
      connection.receiveRequestHeader()
    }

    request match {
      case Success(r: HttpEntityEnclosingRequest) =>
        val method = r.getRequestLine.getMethod.toLowerCase
        logger.info("Request url: {}", r.getRequestLine.getUri)
        logger.info("Request method with entity: {}", method)
        context.stop(self)
      case Success(r: HttpRequest) =>
        val method = r.getRequestLine.getMethod.toLowerCase
        logger.info("Request url: {}", r.getRequestLine.getUri)
        logger.info("Request method without entity method: {}", method)
        nonEntityProxy(r)
      case Failure(e) =>
        logger.debug("Invalid request")
        context.stop(self)
    }

  }

  private def nonEntityProxy(r: HttpRequest) {
    val requestLine = r.getRequestLine
    val headers = r.getAllHeaders

    val request = url(requestLine.getUri)
    for (header <- headers) {
      request.setHeader(header.getName, header.getValue)
      request.setMethod(requestLine.getMethod)
    }
    request.setFollowRedirects(followRedirects = true)

    logger.info("Downloading: {}", requestLine.getUri)
    val result = Http(request)

    result.onComplete {
      case Success(resp) =>
        logger.info("Download completed: {}", resp.getUri)

        val response = new BasicHttpResponse(requestLine.getProtocolVersion, resp.getStatusCode, resp.getStatusText)
        for {
          key <- resp.getHeaders.keySet
          value = resp.getHeaders.getFirstValue(key)
        } {
          response.setHeader(key, value)
        }
        response.setEntity(new InputStreamEntity(resp.getResponseBodyAsStream))

        //Write the header
        connection.sendResponseHeader(response)
        connection.sendResponseEntity(response)
      case Failure(e) =>
        context.stop(self)
    }
  }

  override def postStop() {
    connection.close()
  }
}
