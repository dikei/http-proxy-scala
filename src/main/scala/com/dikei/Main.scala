package com.dikei

import akka.actor.{ActorSystem, Actor}

object Main extends App {

  val system = ActorSystem("scala-proxy")

  val masterActor = system.actorOf(MasterActor.props())

  masterActor ! Start

  sys.addShutdownHook {
    masterActor ! Shutdown
  }
}