package com.coredump.synergybase.config

import com.coredump.synergybase.spatial._

import scala.concurrent.duration._
import scala.language.postfixOps
import akka.pattern.ask

import akka.testkit.{ TestKit, ImplicitSender }
import akka.actor.{ ActorRef, ActorIdentity, ActorSystem, Identify,
  PoisonPill }

import org.scalatest.{ WordSpecLike, Matchers, BeforeAndAfterAll }

/** Case suite for the factory SpatialLab. */
class SpatialLabTest(_system: ActorSystem) extends TestKit(_system)
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(Central.actorSystem)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val lat = 9.944249
  val long = -84.042528
  val userId = 1
  val dimensions = 2
  val lab = system.actorOf(SpatialLab.props, "lab")
  var adm: ActorRef = _

  "An Spatial Lab" must {
    "create lab and don't reply" in {
      lab ! SpatialLab.Default
    }

    "get admin" must {
      within(1 second) {
        lab ! SpatialLab.GetAdmin
        expectMsgClass(classOf[ActorRef])
      }

      lab ! SpatialLab.GetAdmin
      adm = receiveOne(1 second).asInstanceOf[ActorRef]

      //GenericAdmin.admin ! GenericAdmin.CreateUser(dimensions, userId, Aabb(long, lat))
    }

    "create user and don't reply" must {
      expectNoMsg()
      adm ! GenericAdmin.CreateGeneric(2, "User", 1, Map(),  Aabb(long, lat))
    }

     //
     //     "get created user's serialiazed form" in {
//       within(1 second) {
//         GenericAdmin.admin ! GenericAdmin.Msg2User(userId, SpatialStore.Serialize)
//         expectMsg(s"{userId: $userId, spatial: {position: {x: $long, y: $lat}}}")
//       }
//     }
//
//     "get created spatial reference" in {
//       val storeName = Central.config.getString("spatialbase.defaults.space.name")
//       within(500 millis) {
//         val spatialRef = system.actorSelection(
//           s"/user/${storeName}_${dimensions}D/user$userId") !
//           SpatialStore.Serialize
//           // .resolveOne(resolveTimeout.duration)
//         expectMsg(s"{position: {x: $long, y: $lat}}")
//
//       }
//     }
//
//     "reply to Identify" in {
//       GenericAdmin.admin ! GenericAdmin.Msg2User(1, Identify(1))
//       expectMsgClass(classOf[ActorIdentity])
//       GenericAdmin.admin ! GenericAdmin.Msg2User(1, Identify(1))
//       val x = receiveOne(500 millis)
//       x match {
//         case l: ActorRef => assert(true)
//         case ActorIdentity(identifyId, Some(ref)) =>
//         case ActorIdentity(identifyId, None) =>
//           assert(false, "No match on identifying id.")
//         case _ => assert(false, "Expected message not received.")
//       }
//     }
//
//     "not reply to Kill" in {
//       GenericAdmin.admin ! GenericAdmin.Msg2User(1, PoisonPill)
//       expectNoMsg()
//     }

  }

   /** Reduced Scala app version of the this test suite.
     * This is not mantained. You may have to adjust it, but hopefully not.
     *
     *  def main(args: Array[String]) {
     *    val db = SpatialLab.stores(3)
     *    implicit val resolveTimeout = Timeout(10 seconds)
     *    val inbox = Inbox.create(SpatialLab.actorSystem)
     *
     *    inbox.send(GenericAdmin.admin,
     *      GenericAdmin.CreateUser(2, 1, Aabb(9.944249, -84.042528)))
     *
     *    // Test #1
     *    // Gets the first user
     *    inbox.send(GenericAdmin.admin, GenericAdmin.Msg2User(1, SpatialStore.Serialize))
     *    println(inbox.receive(5 seconds))
     *
     *    val userRef = Await.result(SpatialLab.actorSystem.
     *      actorSelection("/user/Genericadmin/user1")
     *      .resolveOne(), resolveTimeout.duration)
     *    inbox.send(userRef, SpatialStore.Serialize)
     *    println(inbox.receive(10 seconds))
     *
     *    val spatialRef = Await.result(SpatialLab.actorSystem.
     *      actorSelection("/user/spatialdb_2D/user1")
     *      .resolveOne(), resolveTimeout.duration)
     *    inbox.send(spatialRef, SpatialStore.Serialize)
     *    println(inbox.receive(5 seconds))
     *    inbox.send(spatialRef, SpatialStore.Serialize)
     *    println(inbox.receive(5 seconds))
     *
     *    // This should fail.
     *    // SpatialStore.actorSystem.actorSelection("/user/spatialdb/2") ! Identify
     *    println("Application started.")
     * }
     */
}
