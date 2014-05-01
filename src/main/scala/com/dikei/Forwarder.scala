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
import org.apache.http.{HttpRequest, Header, RequestLine}

case object StartProcessing

object Forwarder {
  def props(clientSocket: Socket) = Props(classOf[Forwarder], clientSocket)
}

/**
 * Forwarder receive request from receiver to process
 */
class Forwarder(val clientSocket: Socket) extends Actor{

  val logger = Logging(context.system, this)
  val parserFactory = new DefaultHttpRequestParserFactory()
  val writerFactory = new DefaultHttpResponseWriterFactory()
  val bufferSize = 10 * 1024 * 1024
  
  val outBuffer = new SessionOutputBufferImpl(new HttpTransportMetricsImpl, bufferSize)
  outBuffer.bind(clientSocket.getOutputStream)

  val inBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl, bufferSize)
  inBuffer.bind(clientSocket.getInputStream)
  val parser = parserFactory.create(inBuffer, MessageConstraints.DEFAULT)


  override def receive: Receive = {
    case StartProcessing =>
      process()
  }

  def process() = {
    val request = Try {
      parser.parse()
    }

    request match {
      case Success(r) =>
        val headers = r.getAllHeaders
        val requestLine = r.getRequestLine
        val method = requestLine.getMethod.toLowerCase

        logger.info("Request method: {}", method)
        method match {
          case "get" =>
            getProxy(requestLine, headers)
          case _ =>
            logger.info("Unsupported method: {}", method)
            context.stop(self)
        }

      case Failure(e) =>
        logger.debug("Invalid request")
        context.stop(self)
    }

  }

  private def getProxy(requestLine: RequestLine, headers: Array[Header]) {
    val writer = Try {
      writerFactory.create(outBuffer)
    }

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

        writer.foreach { w =>
          //Write the header
          w.write(response)
          //Write the body of the response
          outBuffer.write(resp.getResponseBodyAsBytes)
          context.stop(self)
        }
      case Failure(e) =>
        writer.foreach { w =>
          w.write(new BasicHttpResponse(requestLine.getProtocolVersion, 500, "Unable to proxy request"))
        }
        context.stop(self)
    }
  }

  override def postStop() {
    outBuffer.flush()
    clientSocket.close()
  }
}
