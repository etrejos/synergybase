package com.coredump.synergybase.api

import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.coredump.synergybase.config.GenericAdmin.{GetGeneric, AdminMsg, CreateGeneric}
import com.coredump.synergybase.config.{GenericAdmin, Central, SpatialLab}
import akka.actor.ActorDSL._
import com.coredump.synergybase.spatial.Aabb
import scala.concurrent.Future
import akka.pattern.ask
import scala.concurrent.duration._
import scala.language.postfixOps

trait SpatialApi extends GenericRoute {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout: Timeout = 1 second
  implicit val system = Central.actorSystem
  val lab = system.actorOf(SpatialLab.props, "lab")
  val config = Central.config
  implicit val materializer = ActorMaterializer()

  val init = actor(ctor = new Act {
    whenStarting {
      lab ! SpatialLab.Default
    }
    become {
      case SpatialLab.AdminRef(adm) ⇒
        becomeStacked {
          case c: GenericAdmin.CreateGeneric ⇒
            adm forward c
          case a: AdminMsg ⇒
            adm ! a
        }
    }
  })

  def bind: Future[ServerBinding] = {
    Http().bindAndHandle(route,
      config.getString("spatialbase.tcp.host"),
      config.getInt("spatialbase.tcp.port"))
  }

  private def translateEntity(s: SpatialEntity, dim: Int) = {
    val ab = s.position.length match {
      case 2 => Aabb(s.position(0), s.position(1))
      case 3 => Aabb(s.position(0), s.position(1), s.position(2))
      case 4 => Aabb(s.position(0), s.position(1), s.position(2), s.position(3))
      case 6 => Aabb(s.position(0), s.position(1), s.position(2), s.position(3),
        s.position(4), s.position(5))
    }
    CreateGeneric(
      dimension = 3,
      genericType = s.genericType,
      genericId = s.id,
      data = s.data,
      aabb = ab)
  }

  def getGeneric(l: Long, s: String): Future[SpatialEntity] =
    (init ? GetGeneric(s, l)).mapTo[SpatialEntity]

  def tellToPostGeneric(s: SpatialEntity): Unit =
    init ! translateEntity(s, 3)
}
