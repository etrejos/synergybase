package com.coredump.synergybase.config

import akka.actor.{ ActorSystem, ActorRef, Props, Actor }
import akka.event.Logging
import com.typesafe.config.ConfigFactory

/** Common global configuration resources. */
object Central {

  /** App wide configuration */
  val config = ConfigFactory.load()

  /** App wide actor system */
  val actorSystem = ActorSystem(config.getString("spatialbase.actor-system.name"))
}
