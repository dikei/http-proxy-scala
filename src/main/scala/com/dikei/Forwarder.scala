package com.dikei

import akka.actor.{Props, Actor}
import java.net.Socket
import akka.event.Logging
import scala.util.{Failure, Success, Try}
import dispatch._, Defaults._
import org.apache.http.message.BasicHttpResponse
import scala.collection.JavaConversions._
import org.apache.http._
import org.apache.http.impl.{DefaultBHttpClientConnection, DefaultBHttpServerConnection}
import scala.util.Success
import scala.util.Failure
import org.apache.http.entity.InputStreamEntity
import com.ning.http.client.Response
import com.ning.http.client.generators.InputStreamBodyGenerator
import java.io.{BufferedInputStream, FileInputStream}

case object StartProcessing

object Forwarder {
  def props(clientSocket: Socket) = Props(classOf[Forwarder], clientSocket)
}

/**
 * Forwarder receive request from receiver to process
 */
class Forwarder(val clientSocket: Socket) extends Actor{

  val logger = Logging(context.system, this)

  val bufferSize = 64 * 1024
  val connection = new DefaultBHttpServerConnection(bufferSize)
  connection.bind(clientSocket)

  //content-length header is blacklisted because we'll use chunked encoding
  val blackListHeader = Set("content-length")

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
        val method = r.getRequestLine.getMethod
        logger.info("Request url: {}", r.getRequestLine.getUri)
        logger.info("Request method with entity: {}", method)
        connection.receiveRequestEntity(r)
        entityProxy(r)
      case Success(r: HttpRequest) =>
        val method = r.getRequestLine.getMethod
        logger.info("Request url: {}", r.getRequestLine.getUri)
        logger.info("Request method without entity: {}", method)
        nonEntityProxy(r)
      case Failure(e) =>
        logger.debug("Invalid request")
        context.stop(self)
    }

  }

  private def nonEntityProxy(r: HttpRequest) {
    val requestLine = r.getRequestLine
    val headers = r.getAllHeaders

    var builder = url(requestLine.getUri)
    for (header <- headers) {
      if(!blackListHeader.contains(header.getName.toLowerCase)) {
        builder = builder.setHeader(header.getName, header.getValue)
      }
    }
    builder = builder.setMethod(requestLine.getMethod.toUpperCase)
    builder = builder.setFollowRedirects(followRedirects = true)
    val result = Http(builder)
    result.onComplete {
      case Success(resp) =>
        writeToClient(resp, requestLine.getProtocolVersion)
      case Failure(e) =>
        context.stop(self)
    }
  }

  private def entityProxy(r: HttpEntityEnclosingRequest) = {
    val requestLine = r.getRequestLine
    val headers = r.getAllHeaders

    var builder = url(requestLine.getUri)
    builder = builder.setMethod(requestLine.getMethod.toUpperCase)
    builder = builder.setMethod(requestLine.getMethod.toUpperCase)
    builder = builder.underlying {
      _.setBody(new InputStreamBodyGenerator(r.getEntity.getContent))
    }

    for (header <- headers) {
      if(!blackListHeader.contains(header.getName.toLowerCase)) {
        builder = builder.setHeader(header.getName, header.getValue)
      }
    }

    val result = Http(builder)
    result.onComplete {
      case Success(resp) =>
        writeToClient(resp, requestLine.getProtocolVersion)
      case Failure(e) =>
        context.stop(self)
    }
  }

  private def writeToClient(resp: Response, protocol: ProtocolVersion) = {
    logger.info("Download completed: {}", resp.getUri)

    val response = new BasicHttpResponse(protocol, resp.getStatusCode, resp.getStatusText)
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
    context.stop(self)
  }

  override def postStop() {
    connection.close()
  }
}
