package com.dikei

import akka.actor.{Props, Actor}
import akka.event.Logging

case object Start
case object Shutdown

object MasterActor {
  def props(port: Int) = Props(classOf[MasterActor], port)
}

/**
 * The top level actor of the program
 */
class MasterActor(val port: Int) extends Actor{

  val logger = Logging(context.system, this)

  val receiver = context.actorOf(Receiver.props(port))

  override def receive: Receive = {
    case Start =>
      logger.info("scala-proxy started")
      receiver ! StartListening
  }

}
