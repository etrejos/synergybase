package com.coredump.synergybase.config

import akka.actor.{ Actor, ActorRef, Props }

/** Companion object for the Concrete */
object ConcreteActorFactory {

  /** Props of the class.
    * @return props
    */
  def props: Props = Props(classOf[ConcreteActorFactory])

}

/** Abstract factory for actors. */
class ConcreteActorFactory extends AbstractActorFactory {

  import AbstractActorFactory._

  def receive = {
    case CreateActor(p, n) => createActor(p, n)
  }

}
