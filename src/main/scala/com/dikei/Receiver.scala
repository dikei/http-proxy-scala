package com.dikei

import akka.actor.{Props, Actor}
import akka.event.Logging
import java.net.{InetSocketAddress, SocketAddress, Socket, ServerSocket}
import java.io.{InputStreamReader, BufferedReader}

case object StartListening
case object StopListening

object Receiver {
  def props(port: Int) = Props(classOf[Receiver], port)
}

/**
 * Receiver actor listen on a socket
 */
class Receiver(val port: Int) extends Actor {

  val logger = Logging(context.system, this)

  val socket = new ServerSocket()

  override def receive: Receive = {
    case StartListening =>
      logger.info("Start listening on: {}", port)
      listen()
    case StopListening =>
      logger.info("Stop listening on: {}", port)
      cleanup()

  }

  def listen() = {
    socket.bind(new InetSocketAddress(port))
    val clientSocket = socket.accept()
    val forwarder = context.actorOf(Forwarder.props(clientSocket))
    forwarder ! StartProcessing
  }

  def cleanup() = {
    socket.close()
    context.parent ! Shutdown
  }
}
