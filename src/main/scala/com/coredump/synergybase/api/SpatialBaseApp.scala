package com.coredump.synergybase.api

import scala.util.{Failure, Success}

import scala.language.postfixOps

object SynergyBaseApp extends SpatialApi {
  import scala.concurrent.ExecutionContext.Implicits.global
  def main(args: Array[String]) {
    bind.onComplete {
      case Success(b) =>
        sys.addShutdownHook {
          b.unbind()
          system.terminate()
        }
      case Failure(e) =>
        sys.addShutdownHook {
          system.terminate()
        }
    }
  }
/*
    val lat = 9.944249
    val long = -84.042528
    val userId = 1
    val dimensions = 2

    // val inbox = Inbox.create(system)
    implicit val timeout = Timeout(2 second)
    inbox.send(lab, SpatialLab.Default)

    lab.tell(SpatialLab.GetAdmin, ActorRef.noSender)
    val adm = inbox.receive(3 seconds).asInstanceOf[ActorRef]

    adm ! GenericAdmin.CreateGeneric(
      dimension = 2,
      genericType = "User",
      genericId = 1,
      data = Map("username", "peter"),
      aabb = Aabb(long, lat))
  }
*/

}
