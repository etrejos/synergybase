package com.coredump.synergybase.entity

import com.coredump.synergybase.spatial.SpatialStore.{ SpatialMessage, Locate,
  Serialize, LocatableMessage }
import akka.actor.{ Actor, ActorRef, Props, FSM, Stash }
import scala.collection.parallel.mutable.ParHashSet
import java.util.UUID

/** Companion object for spatial Tags. */
object Tag {
  /** Messages understood by tags. */
  trait TagMessage extends LocatableMessage
  /** Determines if it should show itself. */
  case class Expose(userId: Int) extends TagMessage
  /** Action to allow another user to see the tag.
    * @param userId Unique identifier
    */
  case class AddWatcher(userId: Int) extends TagMessage
  /** Action to remove user from list of allowed watchers.
    * @param userId Unique identifier
    */
  case class RemoveWatcher(userId: Int) extends TagMessage
  /** Action to locate a shareable Tag.
    * @param spatial Spatial reference
    * @param watchers Users allowed to see the Tag
    */
  case class LocateShareable(spatial: ActorRef, watchers: ParHashSet[Int])
    extends LocatableMessage

  /**  */
  sealed trait Visibility extends LocationState
  /** Visible just to the creator. */
  case object Private extends Visibility
  /** Share with every one. */
  case object Public extends Visibility
  /** Allow sharing and unregistering. */
  case object Shareable extends Visibility


  /** Gets a random UUID string */
  def randomUUID = UUID.randomUUID.toString

  /** Gets the Props of the class Tag
   * @param ownerId Unique identifier of the owner
   * @param title Some title for the tag
   * @return props of the class
   */
  def props(ownerId: Int,
            title: String,
            shareMode: Visibility) = {
    Props(classOf[Tag], randomUUID, ownerId, title, shareMode)
  }
}

/** Tag created by a user
 * @param uuid Unique identifier
 * @param ownerId Unique identifier of the creator
 * @param title Some title for the tag
 * @param shareMode Rule for sharing
 */
class Tag(private val uuid: String,
          val ownerId: Int,
          val title: String,
          shareMode: LocationState) extends Locatable with Stash {
  import Tag._

  private val watchers = new ParHashSet[Int]
  watchers += ownerId

  startWith(Unreachable, Uninitialized)

  /** Called after an actor has been stopped */
  override def postStop: Unit = log.warning(s"Tag: $ownerId-$title-$uuid stopped!")

  when(Unreachable) {
    case Event(l: Locate, _) => goto(Located) using l
    case Event(Serialize, _) => {
      sender ! s"{uuid: $uuid, ownerId: $ownerId, title: $title}"
      stay
    }
  }

  when(Located) {
    case Event(s: SpatialMessage, Locate(spatial)) => {
      spatial ! s
      stay
    }
    case Event(Serialize, Locate(spatial)) => {
      sender ! s"{uuid: $uuid, ownerId: $ownerId, title: $title," +
               s" position: $spatial}"
      stay
    }
  }

  // when(Public) {}

  // when(Private) {}

  // when(Shareable) {}

  whenUnhandled {
    case Event(Expose(userId), _) => {
      val exposed: Option[ActorRef] =
        if (watchers.contains(userId)) Option(self) else None
      sender ! exposed
      stay
    }
    case Event(AddWatcher(userId), _) => {
      watchers += userId
      stay
    }
    case Event(RemoveWatcher(userId), _) => {
      watchers -= userId
      stay
    }
  }

  onTransition {
    case Unreachable -> Located =>
      stateData match {
        case l: Locate => {
          shareMode match {
            case Public => goto(Public) using l
            case Private => goto(Private) using l
            case Shareable => goto(Shareable) using l
          }
        }
      }
  }

  initialize()
}
