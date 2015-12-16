package com.coredump.synergybase.api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import com.coredump.synergybase.entity.GenericIdentifiable.GenericMap
import com.coredump.synergybase.spatial.Aabb
import scala.collection.immutable.Map
import spray.json._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import DefaultJsonProtocol._

import scala.concurrent.Future


/** REST routes for Generic Objects. */
trait GenericRoute extends DefaultJsonProtocol {
  import scala.concurrent.ExecutionContext.Implicits.global
  //implicit val genMapFormat: RootJsonFormat[GenericMap] =
  //implicit val genMapFormat: RootJsonFormat[Seq[(String, Any)]] =
  //  jsonFormat1(GenericMap)

  implicit val genMapFormat: RootJsonFormat[GenericMap] = new RootJsonFormat[GenericMap] {
    def write(x: GenericMap) =
      JsObject(x.mapValues(JsString(_)).toIndexedSeq: _*)
    def read(value: JsValue) = value match {
      case JsObject(n) => n.mapValues(_.toString())// [String, String]
    }
  }

  import com.coredump.synergybase.entity.GenericIdentifiable.GenericMap
  //implicit val genMapFormat: RootJsonFormat[GenericMap] = jsonFormat1(collection.immutable.Map[String, String])
  implicit val spatialFormat: RootJsonFormat[SpatialEntity] = jsonFormat4(SpatialEntity)
  /*
  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Long => JsNumber(n)
      case n: Double => JsNumber(n)
      case s: String => JsString(s)
      case b: Boolean if b == true => JsTrue
      case b: Boolean if b == false => JsFalse
    }
    def read(value: JsValue) = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
    }
  }
*/
  protected def getGeneric(l: Long, s: String): Future[SpatialEntity]
  protected def tellToPostGeneric(s: SpatialEntity): Unit

  val g = SpatialEntity(
    genericType = "User",
    id = 1L,
    data = Map("2" -> "22", "1" -> "none"),
    position = List(1.0, 2.0)
  )

  val route =
    pathPrefix("generic") {

      path(LongNumber / Rest) { (genericId, genericType) =>
        get {
          complete {
            val f = getGeneric(genericId, genericType).mapTo[SpatialEntity]
            (OK, f.map(_.toJson))
          }
        }
      } ~
      post {
        // decompress gzipped or deflated requests if required
        decodeRequest {
          entity(as[SpatialEntity]) { generic =>
            complete {
              tellToPostGeneric(generic)
              (OK, generic)
              //g.toJson
            }
          }
        }
      }
  }
}
