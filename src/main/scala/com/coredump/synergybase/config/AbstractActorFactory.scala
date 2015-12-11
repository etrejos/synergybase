package com.coredump.synergybase.config

import akka.actor.{ Actor, ActorRef, Props }

/** Companion object for the AbstractActorFactory */
object AbstractActorFactory {

  sealed trait FactoryMsg
  case class CreateActor(props: Props, name: String)

  /** Props of the class.
    * @return props
    */
  def props: Props = Props(classOf[AbstractActorFactory])

}

/** Abstract factory for actors. */
trait AbstractActorFactory extends Actor {

  import AbstractActorFactory._

  /** Creates an actor.
    * @param p props of the actor
    * @param n name of the actor
    * @return new actor
    */
  def createActor(p: Props, n: String): ActorRef = context.actorOf(p, n)

  /**  */
  def processFactoryMsg(m: FactoryMsg): PartialFunction[Any, Unit] = {
    case CreateActor(p, n) => createActor(p, n)
  }

}
