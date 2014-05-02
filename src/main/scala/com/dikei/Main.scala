package com.dikei

import akka.actor.{ActorSystem, Actor}
import java.io.{FileInputStream, FileOutputStream, IOException}
import scala.util.{Try, Success, Failure}
import java.util.Properties
import scala.collection.immutable.Range.Int

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

  val system = ActorSystem("scala-proxy")

  val masterActor = system.actorOf(MasterActor.props(port.toInt))

  masterActor ! Start

  sys.addShutdownHook {
    masterActor ! Shutdown
  }
}