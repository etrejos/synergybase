package com.coredump.synergybase.spatial

import co.paralleluniverse.spacebase.SpatialToken

import akka.actor.{ FSM, ActorRef, Props, ActorLogging, Stash }
import com.coredump.synergybase.spatial.SpatialStore.{ AddToken,
  MutateAabb, Serialize, Delete, SpatialMessage, Ignore, Bound,
  ConfirmLocation }

/** Supported States. */
object SpatialConnection {
  /** Determines presence of spatial characteristics. */
  sealed trait SpatialCondition
  /** Undefined bounds. */
  case object BoundFree extends SpatialCondition
  /** The bounds of the object are known. */
  case object Bounded extends SpatialCondition
  /** Object is inside the database. */
  case object Located extends SpatialCondition
}

/** Actor that responds to spatial operations. */
class SpatialConnection
  extends FSM[SpatialConnection.SpatialCondition, SpatialMessage]
  with ActorLogging with Stash {

  import SpatialConnection._

  startWith(BoundFree, Ignore)

  when(BoundFree) {
    case Event(b: Bound, _) => {
      goto(Bounded) using b
    }
    case Event(c: ConfirmLocation, _) => {
      goto(Located) using c
    }
  }

  when(Bounded) {
    case Event(AddToken(t: SpatialToken), Bound(aabb, gc)) => {
      goto(Located) using ConfirmLocation(aabb, gc, t)
    }
    case Event(MutateAabb(newAabb: Aabb), Bound(aabb, gc)) => {
      goto(Bounded) using Bound(newAabb, gc)
    }
    case Event(Serialize, Bound(aabb, gc)) =>
      sender ! serializeAabb(aabb)
      stay
  }

  when(Located) {
    case Event(MutateAabb(newAabb: Aabb), ConfirmLocation(aabb, gc, token)) =>
      goto(Located) using ConfirmLocation(newAabb, gc, token)
    case Event(Serialize, ConfirmLocation(aabb, gc, token)) =>
      sender ! serializeAabb(aabb)
      stay
  }

  /** Serializes an AABB */
  private def serializeAabb(aabb: Aabb): String = {
    var chr = 'x'
    var str = new StringBuilder(30)
    str ++= "{"
    for(d <- 0 to aabb.dims - 1) {
      str ++= chr + ": " + aabb.min(d) + ", "
      chr = (chr + 1).toChar
    }
    str = str.dropRight(2)
    str ++= "}"
    str.toString
  }

  onTermination {
    case StopEvent(FSM.Normal, Located,
                   ConfirmLocation(aabb, gc, token)) =>
      gc ! Delete(token)
    case StopEvent(FSM.Shutdown, Located,
                   ConfirmLocation(aabb, gc, token)) =>
      gc ! Delete(token)
    case StopEvent(FSM.Failure(cause), Located,
                   ConfirmLocation(aabb, gc, token)) =>
      gc ! Delete(token)
  }

  initialize()

}
