package com.coredump.synergybase.spatial

import scala.collection.JavaConversions._
import java.util.Set
import com.coredump.synergybase.util.BeanUtils
import co.paralleluniverse.spacebase.{ SpatialToken, SpaceBase, ElementUpdater,
  SpatialQueries, SpatialSetVisitor, SpatialQuery }
import akka.actor.{ ActorRef, Props, Actor }

/** SpatialStore companion object. */
object SpatialStore {

  /** Understood messages. */

  /** Messages that a locatable object can understand. */
  trait LocatableMessage

  /** Messages that a spatial object can understand. */
  trait SpatialMessage extends LocatableMessage

  /** Request to Insert into the store.
    * @tparam A any subclass of "locatable" ActorRef. They speak to spatial actors
    * @param locatable Locatable actor
    * @param aabb Axis-aligned bounding box
    */
  case class Insert[A <: ActorRef](locatable: A, aabb: Aabb) extends SpatialMessage

  /** Request to Insert into the store some spatial object and notifying it.
    * @tparam A any subclass of "locatable" ActorRef. They speak to spatial actors
    */
  case class InsertAndLocate[A <: ActorRef](
      spatial: A,
      aabb: Aabb,
      locatable: ActorRef)
    extends SpatialMessage

  /** Request to Update the position of some spatial object. */
  case class Update(token: SpatialToken, newAabb: Aabb) extends SpatialMessage

  /** Request some spatial object from the store. */
  case class Delete(token: SpatialToken) extends SpatialMessage

  /** Request some spatial object from the store. */
  case class AddToken(token: SpatialToken) extends SpatialMessage

  /** Select(DB) like messages */
  /** Tell message to those whose bounding box is equal to one provided. */
  case class TellEquals(aabb: Aabb, message: SpatialMessage) extends SpatialMessage

  /** Tell message to those that intersect some area. */
  case class TellIntersect(aabb: Aabb, message: SpatialMessage) extends SpatialMessage

  /** Tell message to those contained in some area. */
  case class TellContained(aabb: Aabb, message: SpatialMessage) extends SpatialMessage

  /** Tell message to those in a range. */
  case class TellRange(aabb: Aabb, ratio :Double, message: SpatialMessage) extends SpatialMessage

  /** Tell message to those that contain some area. */
  case class TellContain(aabb: Aabb, message: SpatialMessage) extends SpatialMessage

  /** Tell message to everyone. */
  case class Broadcast(message: SpatialMessage) extends SpatialMessage

  /** Request to create some actor from some Props. */
  case class CreateFromProps(actorName: String, aabb: Aabb, props: Props)  extends SpatialMessage

  /** Request to create some actor and associate it an spatial reference. */
  case class CreateAndLocate(actorName: String, aabb: Aabb, locatable: ActorRef) extends SpatialMessage

  /** Messages sent by the class. */
  /** Request to change the AABB. */
  case class MutateAabb(newAabb: Aabb) extends SpatialMessage

  /** Request to associate an spatial reference. */
  case class Locate(spatial: ActorRef) extends SpatialMessage

  /** Request to serialize itself. */
  case object Serialize extends SpatialMessage

  /** Store reference. */
  case class StoreRef(store: ActorRef) extends SpatialMessage

  /** Ignore message. */
  case object Ignore extends SpatialMessage

  /** Bound spatial. */
  case class Bound(aabb: Aabb, gc: ActorRef) extends SpatialMessage

  /** Confirm registraion in store. */
  case class ConfirmLocation(aabb: Aabb, gc: ActorRef, token: SpatialToken) extends SpatialMessage

}

/** Spatial store, interface to SpaceBase.
  * Usage: store ! SpatialMessage
  *
  * @param db SpaceBase instance
  * @param gc spatial garbage collector
  * @see com.coredump.synergybase.spatial.SpatialStore.SpatialMessage
  * @see co.paralleluniverse.spacebase.SpaceBase
  */
class SpatialStore[A <: ActorRef](val db: SpaceBase[A],
                                  val gc: ActorRef) extends Actor {
  import SpatialStore._

  protected type SpatialAction = (SpatialMessage, A) => Unit

  override def postStop(): Unit = {
    val productName = "SpaceBase"
    BeanUtils.removeMBean(
      s"co.paralleluniverse:type=$productName,name=${db.getName}," +
      "monitor=performance")
  }

  /** Factory methods */

  protected def createActor(props: Props,
                            actorName: String): ActorRef  = {
    context.actorOf(props, actorName)
  }

  protected def createSpatial(aabb: Aabb,
                              actorName: String): ActorRef = {
    createActor(Props(classOf[SpatialConnection]), actorName)
  }

  protected def visit(action: SpatialAction): SpatialSetVisitor[A] =
    new SpatialSetVisitor[A]() {
      def visit(result: java.util.Set[A],
                resForUpdate: java.util.Set[ElementUpdater[A]]): Unit =
        result.foreach(elem => action)
    }

  protected def query(q: SpatialQuery[Object])(action: SpatialAction): Unit =
    db.query(q, visit(action))

  /** Helpers */

  protected def tellMessage(message: SpatialMessage,
                            actorRef: ActorRef): Unit =
    actorRef ! message


  /** Database operations */

  protected def insert[B <: ActorRef](locatable: B,
                                      aabb: Aabb): Unit = {
    self ! Insert(locatable, aabb)
    locatable ! MutateAabb(aabb)
  }

  protected def equals(aabb: Aabb,
                       message: SpatialMessage)
                      (action: SpatialAction): Unit =
    query(SpatialQueries.equals(aabb))(action)

  protected def intersect(aabb: Aabb,
                          message: SpatialMessage)
                         (action: SpatialAction): Unit =
    query(SpatialQueries.intersect(aabb))(action)

  protected def contained(aabb: Aabb,
                          message: SpatialMessage)
                         (action: SpatialAction): Unit =
    query(SpatialQueries.contained(aabb))(action)

  protected def range(aabb: Aabb,
                      ratio: Double,
                      message: SpatialMessage)
                     (action: SpatialAction): Unit =
    query(SpatialQueries.range(aabb, ratio))(action)

  protected def contain(aabb: Aabb,
                        message: SpatialMessage)
                       (action: SpatialAction): Unit =
    query(SpatialQueries.contain(aabb))(action)

  protected def all(message: SpatialMessage)
                   (action: SpatialAction): Unit =
    query(SpatialQueries.ALL_QUERY)(action)

  def receive = {
    case i: Insert[A] => {
      val token: SpatialToken = db.insert(i.locatable, i.aabb)
      i.locatable ! ConfirmLocation(i.aabb, gc, token)
    }

    case i: InsertAndLocate[a] => {
      i.locatable ! Locate(i.spatial)
      self ! Insert(i.locatable, i.aabb)
    }

    case Update(token, newAabb) => {
      db.update(token, newAabb)
      sender() ! MutateAabb(newAabb)
    }

    case Delete(token) => db.delete(token)

    case CreateFromProps(actorName, aabb, props) => {
      val spatial = createActor(props, actorName)
      insert(spatial, aabb)
    }

    case CreateAndLocate(actorName, aabb, locatable) => {
      val spatial = createSpatial(aabb, actorName)
      self ! InsertAndLocate(spatial, aabb, locatable)
    }

    case TellEquals(aabb, message) => equals(aabb, message)(tellMessage _)

    case TellIntersect(aabb, message) => intersect(aabb, message)(tellMessage _)

    case TellContained(aabb, message) => contained(aabb, message)(tellMessage _)

    case TellRange(aabb, ratio, message) =>
      range(aabb, ratio, message)(tellMessage _)

    case TellContain(aabb, message) => contain(aabb, message)(tellMessage _)

    case Broadcast(message) => all(message)(tellMessage _)

  }
}
