package com.coredump.synergybase

/** Package global type aliases. */
package object entity {

  /** Map indexing actors. */
  type IndexedActor = scala.collection.immutable.Map[Long, akka.actor.ActorRef]
}
