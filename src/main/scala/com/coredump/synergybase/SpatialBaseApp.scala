package com.coredump.synergybase

import com.coredump.synergybase.spatial._
import com.coredump.synergybase.config._
import com.coredump.synergybase.config.GenericAdmin._


import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


import scala.concurrent.duration._
import scala.language.postfixOps
import akka.pattern.ask

import akka.util.Timeout
import akka.testkit.{ TestKit, ImplicitSender }
import akka.actor.{ ActorRef, ActorIdentity, ActorSystem, Identify, Inbox,
 PoisonPill }
import akka.actor.ActorDSL._

import akka.http.Http
import akka.http.model.{ HttpEntity, HttpResponse, HttpRequest, Uri, MediaTypes,
  ResponseEntity }
import akka.http.model.HttpMethods._
import akka.stream.scaladsl.Flow
import akka.stream.FlowMaterializer

/** App class.
  *
  * Use it if you must. Should be stripped out of the library.
  *
  * Example of usage:
  *
  * val db = SpatialLab.stores(3)
  * implicit val resolveTimeout = Timeout(10 seconds)
  * val inbox = Inbox.create(SpatialLab.actorSystem)
  *
  * inbox.send(GenericAdmin.admin,
  *   GenericAdmin.CreateGeneric(2, 1, Aabb(9.944249, -84.042528)))
  *
  * // Test #1
  * // Gets the first generic
  *
  * inbox.send(GenericAdmin.admin, GenericAdmin.Msg2User(1, SpatialStore.Serialize))
  * println(inbox.receive(5 seconds))
  *
  *
  * // This won't work as the thread gets block, making the generic unreachable.
  * // The generic gets created but remains invisible to current thread.
  * // The problem gets solve by delegating the creation of the generic to another
  * // ExecutionContext.
  * val genericRef = Await.result(SpatialLab.actorSystem.
  *   actorSelection("/user/generic/user/1")
  *   .resolveOne(), resolveTimeout.duration)
  * inbox.send(userRef, SpatialStore.Serialize)
  * println(inbox.receive(10 seconds))
  *
  *
  * // Test #2
  * // Gets the spatial reference and serializes it.
  *
  * val spatialRef = Await.result(SpatialLab.actorSystem.
  *   actorSelection("/user/spatialdb_2D/user1")
  *   .resolveOne(), resolveTimeout.duration)
  * inbox.send(spatialRef, SpatialStore.Serialize)
  * println(inbox.receive(5 seconds))
  * inbox.send(spatialRef, SpatialStore.Serialize)
  * println(inbox.receive(5 seconds))
  *
  * // This should fail as the user with Id 2 doesn't exist.
  * SpatialStore.actorSystem.actorSelection("/user/spatialdb/user2") ! Identify
  */
object SynergyBaseApp {

  def main(args: Array[String]) {
    implicit val system = Central.actorSystem
    implicit val materializer = FlowMaterializer()
    val lat = 9.944249
    val long = -84.042528
    val userId = 1
    val dimensions = 2
    val lab = system.actorOf(SpatialLab.props, "lab")

    val init = actor(new Act {
      whenStarting { lab ! SpatialLab.Default }
      become {
        case SpatialLab.AdminRef(adm) ⇒
          becomeStacked {
            case a: AdminMsg =>
              adm ! a
          }
      }
    })

    val serverBinding = Http(system).bind(
     interface = Central.config.getString("spatialbase.tcp.host"),
     port = Central.config.getInt("spatialbase.tcp.port"))


    val requestHandler: HttpRequest => HttpResponse = {
      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
        HttpResponse(
          entity = HttpEntity(MediaTypes.`text/html`,
            "<html><body>Hello world!</body></html>"))

      case HttpRequest(GET, Uri.Path("/ping"), _, _, _)  =>
        HttpResponse(entity = collection.immutable.Map(3 -> "PONG!").toString)
      case HttpRequest(GET, Uri.Path("/crash"), _, _, _) => sys.error("BOOM!")
      case _: HttpRequest                                => HttpResponse(404, entity = "Unknown resource!")
    }

    serverBinding.connections foreach { connection =>
      println("Accepted new connection from " + connection.remoteAddress)

      connection handleWithSyncHandler requestHandler
      // this is equivalent to
      // connection handleWith { Flow[HttpRequest] map requestHandler }
    }



/*
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
*/

  }

}
