package com.dikei

import akka.actor.{Props, Actor}
import akka.event.Logging

case object Start
case object Shutdown

object MasterActor {
  def props() = Props(classOf[MasterActor])
}

/**
 * The top level actor of the program
 */
class MasterActor extends Actor{

  val logger = Logging(context.system, this)

  val janitor = context.actorOf(Janitor.props())

  val port = 55555
  val receiver = context.actorOf(Receiver.props(port))

  override def receive: Receive = {
    case Start =>
      logger.info("scala-proxy started")
      receiver ! StartListening
    case Shutdown =>
      janitor ! Shutdown
  }

}
