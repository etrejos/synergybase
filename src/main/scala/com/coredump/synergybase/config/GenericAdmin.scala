package com.coredump.synergybase.config

import com.coredump.synergybase.entity.GenericIdentifiable.GenericMap
import com.coredump.synergybase.spatial.Aabb
import scala.collection.parallel.immutable.ParHashMap
import akka.actor.{ActorRef, FSM, Props, ActorLogging}


/** Administrator of generics (not an admin generic). */
object GenericAdmin {

  /** Messages supported by the generic admin. */
  sealed trait AdminMsg

  /** Create a generic.
    * @param genericType generic type name
    * @param dimension Dimension in which the generic will be created
    * @param genericId Generic's unique identifier
    * @param data hashmap containing generic data
    * @param aabb Axis-aligned bounding box
    */
  case class CreateGeneric(
      dimension: Int,
      genericType: String,
      genericId: Long,
      data: GenericMap,
      aabb: Aabb)
    extends AdminMsg

  /** Send message to generic.
    * @param genericType name of the generic type
    * @param genericId generic's unique identifier
    * @param msg message to send
    */
  case class Msg2Generic(
      genericType: String,
      genericId: Long,
      msg: Any) extends AdminMsg

  /** Request to get a generic.
    * @param genericType type
    * @param id unique identifier
    */
  case class GetGeneric(genericType: String, id: Long) extends AdminMsg

  /** Request to add some factory.
    * @param name name of the factory
    */
  case class AddFactory(name: String) extends AdminMsg

  /** Ignore message. */
  case object Ignore extends AdminMsg

  /** Start the admin. */
  case class Start(stores: Map[Int, ActorRef])

  /** Send message to generic.
    * @param m Generic factories
    */
  case class Factories(m: ParHashMap[String, ActorRef]) extends AdminMsg

  /** State of administrators. */
  sealed trait AdminState
  //case object Idle extends AdminState

  /** Working state. */
  case object Working extends AdminState

  /** Default Props */
  def props(stores: Map[Int, ActorRef]): Props =
    Props(classOf[GenericAdmin], stores)
}

/** Generic administrator actor. */
class GenericAdmin(stores: Map[Int, ActorRef])
  extends FSM[GenericAdmin.AdminState, GenericAdmin.AdminMsg]
  with ActorLogging {

  import GenericAdmin._

  startWith(Working, Factories(ParHashMap()))

  private def createFactory(genericType: String) =
    context.actorOf(GenericFactory.props(genericType, stores),
      genericType)

  private def msg2Generic(genericType: String, genericId: Long, msg: Any) = {
    log.debug(s"Request to get generic: $genericType$genericId")
    context.actorSelection(s"$genericType") forward msg
  }

  when(Working) {
    case Event(c: CreateGeneric, Factories(map)) =>
      log.info(s"Request to create ${c.genericType} clazz: ${c.genericId}")
      c.dimension match {
        case 2 | 3 => {
          if (!map.contains(c.genericType)) {
            self ! AddFactory(c.genericType)
            self ! c
          } else {
            map(c.genericType)
          }
        }
        case x => log.error(s"Dimension: $x is not supported.")
      }
      stay

    case Event(AddFactory(name), Factories(map)) =>
      val f = context.actorOf(GenericFactory.props(name, stores))
      goto(Working) using Factories(map + (name, f))
  }

  whenUnhandled {
    case Event(Msg2Generic(t, id, msg), _) =>
      msg2Generic(t, id, msg)
      stay
  }

  initialize()
}
