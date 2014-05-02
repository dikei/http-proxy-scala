package com.dikei

import akka.actor.{Props, Actor}
import akka.event.Logging
import dispatch.Http

object Janitor {
  def props() = Props(classOf[Janitor])
}

/**
 * Janitor actor to cleanup and shutdown the program
 */
class Janitor extends Actor {

  val logger = Logging(context.system, this)

  override def receive: Receive = {
    case Shutdown =>
      logger.info("Starting cleanup process")
      context.system.shutdown()
      Http.shutdown()
      logger.info("Shutdown complete")
  }
}
