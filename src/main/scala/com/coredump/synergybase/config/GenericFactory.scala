package com.coredump.synergybase.config

import akka.actor.{ ActorRef, Props }
import com.coredump.synergybase.entity.GenericIdentifiable
import com.coredump.synergybase.spatial.Aabb
import com.coredump.synergybase.spatial.SpatialStore.Serialize

/** Companion object for the Factory. */
object GenericFactory {

  import GenericIdentifiable.GenericMap

  /** Message understood by a generic factory. */
  sealed trait GenericMsg

  /** Request to create a generic. */
  case class CreateGeneric(
      dimension: Int,
      genericType: String,
      id: Long,
      data: GenericMap,
      aabb: Aabb)
    extends GenericMsg

  /** Props of the class. */
  def props(genericType: String, stores: Map[Int, ActorRef]): Props =
    Props(classOf[GenericFactory], genericType, stores)
}

/** Factory of generic identifiable entities. */
class GenericFactory(genericType: String, stores: Map[Int, ActorRef])
  extends AbstractActorFactory {

  // Types
  import GenericIdentifiable.{ GenericData, GenericMap }
  // Messages
  import AbstractActorFactory._
  import GenericFactory._
  import GenericAdmin._
  import com.coredump.synergybase.spatial.SpatialStore.CreateAndLocate

  /** Creates a generic identifiable object
    * @param dimension dimension for the spatial object
    * @param genericType type of the object
    * @param id unique identifier
    * @param data data for the object
    * @param aabb bounds for the spatial object
    * @return new generic identifiable
    */
  def createGeneric(
      dimension: Int,
      genericType: String,
      id: Long,
      data: GenericMap,
      aabb: Aabb) = {
    val generic: ActorRef = createActor(
      GenericIdentifiable.props(id = id, genericType = genericType), id.toString)
    generic ! GenericData(data)
    stores(dimension) ! CreateAndLocate(s"${genericType}_$id", aabb, generic)
    generic
  }

  /** Sends a message to a generic uniquely identifiable actor
    * @param id unique id
    * @param msg message to send
    */
  def tellGeneric(id: Long, msg: Any): Unit =
    context.actorSelection(id.toString) ! msg

  private def getGeneric(id: Long) = context.actorSelection(id.toString)

  def receive = {
    case f: FactoryMsg => processFactoryMsg(f)
    case GenericAdmin.CreateGeneric(dim, clazz, id, data, aabb) =>
      createGeneric(dim, clazz, id, data, aabb)
    case Msg2Generic(t, id, msg) => tellGeneric(id, msg)
    case GetGeneric(t, id) => getGeneric(id) forward Serialize
  }
}
