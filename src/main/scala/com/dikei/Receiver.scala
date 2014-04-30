package com.dikei

import akka.actor.{Props, Actor}
import akka.event.Logging
import java.net.{InetSocketAddress, SocketAddress, Socket, ServerSocket}
import java.io.{InputStreamReader, BufferedReader}
import scala.concurrent.Future
import scala.util.{Success, Failure}

case object StartListening

object Receiver {
  def props(port: Int) = Props(classOf[Receiver], port)
}

/**
 * Receiver actor listen on a socket
 */
class Receiver(val port: Int) extends Actor {
  import context.dispatcher

  val logger = Logging(context.system, this)

  val socket = new ServerSocket()

  override def receive: Receive = {
    case StartListening =>
      logger.info("Start listening on: {}", port)
      socket.bind(new InetSocketAddress(port))
      listen()
  }

  def listen(): Unit = {
    val futureSocket = Future {
      socket.accept()
    }

    futureSocket.onComplete {
      case Success(s) =>
        val forwarder = context.actorOf(Forwarder.props(s))
        forwarder ! StartProcessing
        listen()
      case Failure(e) =>
        context.stop(self)
    }
  }

  def cleanup() = {
    socket.close()
  }

  override def postStop() = {
    cleanup()
  }

}
