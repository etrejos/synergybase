package com.coredump.synergybase.entity

import com.coredump.synergybase.spatial.SpatialStore.LocatableMessage

import akka.actor.{ Actor, ActorRef, Props, FSM }

/** Generic type for transitional locatable actors. */
trait LocationState
/** Represents an actor locatable in the store. */
case object Located extends LocationState
/** Represents an actor without an spatial reference. */
case object Unreachable extends LocationState

/** Initial value for FSM locatable actors. */
case object Uninitialized extends LocatableMessage

/** Generic type corresponding to FSM locatable actors. */
trait Locatable extends FSM[LocationState, LocatableMessage]
