package com.dikei

import akka.actor.{Props, Actor}
import java.net.Socket
import java.io._
import akka.event.Logging

case object StartProcessing

object Forwarder {
  def props(clientSocket: Socket) = Props(classOf[Forwarder], clientSocket)
}

/**
 * Forwarder receive request from receiver to process
 */
class Forwarder(val clientSocket: Socket) extends Actor{

  val logger = Logging(context.system, this)

  override def receive: Receive = {
    case StartProcessing =>
      process()
  }

  def process() = {
    logger.info("Start processing")
    val reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
    val writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream))
    var input = reader.readLine()

    while(input != null) {
      logger.info(input)
      writer.write(input + "\n")
      writer.flush()
      input = reader.readLine()
    }
    context.stop(self)
  }
}
