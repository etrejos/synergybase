package com.coredump.synergybase.entity

import com.coredump.synergybase.spatial.SpatialStore.{ Locate, SpatialMessage,
  LocatableMessage, Serialize }
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.immutable.Map
import akka.actor.{ Props, ActorLogging, FSM, Identify, Stash, ActorIdentity,
  ActorRef }
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.json.{ Json, JsNull, Format, JsValue, JsResult, JsSuccess,
  JsNumber, JsBoolean, JsString, JsArray }
import play.api.libs.json.Reads._
import play.api.libs.json.Json.JsValueWrapper

/** Companion object for GenericIdentifiable. */
object GenericIdentifiable {

  /** Return Props of the class GenericIdentifiable
    * @param id GenericIdentifiable's unique identifier
    * @return props of the class
    */
  def props(genericType: String, id: Long): Props =
    Props(classOf[GenericIdentifiable], id, genericType)

  /** Generic data map. */
  type GenericMap = Map[String, Any]

  /** The generic object has its data. */
  case object HasData extends LocationState

  /** Bound data. */
  case class GenericData(data: GenericMap) extends LocatableMessage

  /** Request to associate an spatial reference. */
  case class LocateWithData(spatial: ActorRef, data: GenericMap)
    extends LocatableMessage

  implicit val objectMapFormat = new Format[GenericMap] {

    def writes(map: GenericMap): JsValue =
      Json.obj(map.map{case (s, o) =>
        val ret:(String, JsValueWrapper) = o match {
          case _: Long => s -> JsNumber(o.asInstanceOf[Long])
          case _: Double => s -> JsNumber(o.asInstanceOf[Double])
          case _: String => s -> JsString(o.asInstanceOf[String])
          case _: Boolean => s -> JsBoolean(o.asInstanceOf[Boolean])
          case _ => s -> JsArray(o.asInstanceOf[List[String]].map(JsString(_)))
        }
        ret
      }.toSeq: _*)

    def reads(jv: JsValue): JsResult[GenericMap] =
      JsSuccess(jv.as[Map[String, JsValue]].map{case (k, v) =>
        k -> (v match {
          case s: JsString => s.as[String]
          case l => l.as[List[String]]
        })
      })
  }
}

/** Generic locatable entity that has a unique identifier. */
class GenericIdentifiable(val id: Long,
                          val genericType: String)
  extends Locatable
  with ActorLogging
  with Stash {

  import GenericIdentifiable._
  import context.dispatcher // Verify

  startWith(Unreachable, Uninitialized)

  /** Hook redefinition to log stopped spatial entities */
  override def postStop: Unit = log.debug(s"Generic $genericType$id stopped.")

  /** Converts the data of the generic object to a String.
    * @param m Data
    * @return serialized form of the data
    */
  def dataToSerializedStr(m: GenericMap): String = Json.stringify(Json.toJson(m))


  when(Unreachable) {
    case Event(g: GenericData, _) => {
      log.info(s"Generic object now has data ${g.data}.")
      goto(HasData) using g
    }
  }

  when(HasData) {

    case Event(Locate(ref), GenericData(data)) => {
      val d: String = dataToSerializedStr(data)
      log.info(
        s"Generic object, with data $d located with $ref."
      )
      goto(Located) using LocateWithData(ref, data)
    }

    case Event(Serialize, GenericData(data)) => {
      val d: String = dataToSerializedStr(data)
      sender ! s"{genericType: $genericType, id: $id, data: $d, position: null}"
      stay
    }
  }

  when(Located) {
    case Event(Serialize, LocateWithData(spatial, data)) => {
      implicit val timeout = Timeout(1 second)
      // val future = spatial ? Serialize
      var spatialStr = "" //Await.result(future, timeout.duration)
      //   .asInstanceOf[String]
      val f = spatial ? Serialize
      f foreach { s => spatialStr += s }

      val d = dataToSerializedStr(data)
      val serial = s"{genericType: $genericType, id: $id, data: $d, position: $spatialStr}"
      log.debug(s"Serializing $self: $serial")
      sender ! serial
      stay
    }

    case Event(s: SpatialMessage, LocateWithData(spatial, data)) => {
      spatial ! s
      stay
    }


    case Event(GenericData(newData), LocateWithData(spatial, data)) => {
      log.info(s"Generic object now has data ${newData}.")
      goto(Located) using LocateWithData(spatial, newData)
    }
  }

  initialize()

}
