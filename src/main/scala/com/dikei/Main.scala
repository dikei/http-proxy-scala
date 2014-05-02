package com.dikei

import akka.actor.{ActorSystem, Actor}
import java.io.{FileInputStream, FileOutputStream, IOException}
import scala.util.{Try, Success, Failure}
import java.util.Properties
import scala.collection.immutable.Range.Int
import dispatch.Http
import akka.event.Logging
import org.slf4j.LoggerFactory

object Main extends App {

  val DEFAULT_PORT = "55555"

  val properties = Try {
    val configFile = new FileInputStream("config.properties")
    val properties = new Properties()
    properties.load(configFile)
    properties
  }

  val port = properties match {
    case Success(p) =>
      p.getProperty("port", DEFAULT_PORT)
    case _ =>
      DEFAULT_PORT
  }

  val logger = LoggerFactory.getLogger(this.getClass)
  
  val system = ActorSystem("scala-proxy")

  val masterActor = system.actorOf(MasterActor.props(port.toInt))
  masterActor ! Start

  sys.addShutdownHook {
    logger.info("Starting cleanup process")
    Http.shutdown()
    system.shutdown()
    logger.info("Shutdown complete")
  }
}