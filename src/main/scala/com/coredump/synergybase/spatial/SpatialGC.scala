package com.coredump.synergybase.spatial

import com.coredump.synergybase.spatial.SpatialStore.{ SpatialMessage, Delete,
  Ignore, StoreRef }

import akka.actor.{ FSM, Props, ActorLogging }

sealed trait StoreConnection
case object Disconnected extends StoreConnection
case object Connected extends StoreConnection

/** Spatial garbage collector. */
object SpatialGC {
  def props = Props[SpatialGC]
}

/** Actor that responds to spatial operations. */
class SpatialGC extends FSM[StoreConnection, SpatialMessage] with ActorLogging {

  startWith(Disconnected, Ignore)

  when(Disconnected) {
    case Event(s: StoreRef, _) => {
      goto(Disconnected) using s
    }
  }

  when(Connected) {
    case Event(d: Delete, StoreRef(store)) => {
      store ! d
      stay
    }
  }

  initialize()
}
