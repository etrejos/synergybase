package com.coredump.synergybase.config

import java.util.concurrent.{ ThreadPoolExecutor, ForkJoinPool }

import com.coredump.synergybase.spatial.{ SpatialStore, SpatialGC }
import com.coredump.synergybase.util.BeanUtils

import collection.immutable.{ Map => iMap }
import collection.JavaConversions._
import collection.mutable.Map

import scala.language.postfixOps
import akka.actor.{ ActorRef, Props, FSM, ActorLogging, Stash }

import co.paralleluniverse.db.api.DbExecutors
import co.paralleluniverse.spacebase.{ SpaceBaseBuilder, SpaceBase }

/** Utility methods for creating spatial stores. */
object SpatialLab {

  /** Actor indexed in a map. */
  type IndexedActor = iMap[Int, ActorRef]

  /** Execution mode for the spatial database. */
  sealed trait ExecutionMode
  /** Parallel mode.
    * @see java.util.concurrent.ThreadPoolExecutor
    */
  object Parallel extends ExecutionMode {
    def apply(threads: Int): ForkJoinPool = DbExecutors.parallel(threads)
    def unapply(threads: Int): Some[Int] = Some(threads)
  }

  /** Concurrent mode.
    * @see jsr166e.ForkJoinPool
    */
  object Concurrent extends ExecutionMode {
    def apply(threads: Int): ThreadPoolExecutor = DbExecutors.concurrent(threads)
    def unapply(threads: Int): Some[Int] = Some(threads)
  }

  val Config = Central.config

  lazy val ParallelMode = Parallel(Config.getInt("spatialbase.execution.threads"))
  lazy val ConcurrentMode = Concurrent(
    Config.getInt("spatialbase.execution.threads"))

  /** Messages. */
  sealed trait Recipe

  /** Void recipe. */
  case object Void extends Recipe

  /** Configure the factory class. */
  case class Configure(e: ExecutionMode, stores: IndexedActor) extends Recipe

  /** Admin reference. */
  case class AdminRef(a: ActorRef) extends Recipe

  /** Administrate lab. Indicates to function at its full capacity */
  case class Administrate(
      e: ExecutionMode,
      stores: IndexedActor,
      adm: ActorRef)
    extends Recipe

  /** Empty message. */
  case object Default extends Recipe

  /** Gets a store. */
  case class GetStore(dimension: Int) extends Recipe

  /** Gets the admin. */
  case object GetAdmin extends Recipe

  /** States. */
  sealed trait InitStage
  /** Hasn't started yet. */
  case object Unset extends InitStage
  /** Its all set to function. */
  case object Configured extends InitStage

  /** Gets the props of the class
    * @return Props of SpatialLab
    */
  def props: Props = Props[SpatialLab]

}

/** Utility methods for manipulation of Spatial Stores. */
class SpatialLab extends FSM[SpatialLab.InitStage, SpatialLab.Recipe]
  with ActorLogging
  with Stash {

  import SpatialLab._

  startWith(Unset, Void)

  when(Unset) {
    case Event(Default, _) =>

      def execMode: ExecutionMode = defaultExecMode
      def gcs: IndexedActor = createGCs(
        Config.getIntList("spatialbase.defaults.dimensions").toList.map{_.toInt})

      def createDBs(em: ExecutionMode, gcs: IndexedActor): IndexedActor =
        createStores(
          Config.getIntList("spatialbase.defaults.dimensions").toList.map{_.toInt},
          gcs, em)
      def createAdm(stores: IndexedActor): ActorRef = createAdmin(stores)
      def buildAdminMsg(em: ExecutionMode,
                        gcs: IndexedActor,
                        f: (ExecutionMode, IndexedActor) => IndexedActor,
                        g: IndexedActor => ActorRef): Administrate = {
        val x = f(em, gcs)
        val y = g(x)
        Administrate(em, x, y)
      }
      goto(Configured) using buildAdminMsg(execMode, gcs, createDBs, createAdm)

    case Event(Configure(execM, dbs), _) =>
      log.debug("Configure message recieved.")
      val admin = createAdmin(dbs)
      goto(Configured) using Administrate(execM, dbs, admin)
  }

  when(Configured) {
    case Event(GetStore(dimension), Administrate(e, stores, adm)) =>
      val s = stores(dimension)
      sender ! s
      stay
    case Event(GetAdmin, Administrate(e, stores, adm)) =>
      sender ! AdminRef(adm)
      stay
  }

  /** Creates an spatial store
    * @param dimensions number of dimensions
    * @param name name of the store
    * @param gc spatial GC
    * @param execMode execution mode
    * @tparam A spatial actor
    * @return spatial store
    */
  def createSpace[A <: ActorRef](dimensions: Int,
                                 name: String,
                                 gc: ActorRef,
                                 execMode: ExecutionMode): ActorRef = {
    val builder = new SpaceBaseBuilder().setDimensions(dimensions)
    executionModeForBuilder(execModeFromText(
      Config.getString("spatialbase.execution.mode")), builder)
    val spatialDb: SpaceBase[A] = builder.build(name)
    context.actorOf(
      Props(
        classOf[SpatialStore[A]], spatialDb, gc
      ), name)
  }

  /** Removes performance beans (currently not working) */
  override def postStop() {
    // BeanUtils.removeMBean(name = "co.paralleluniverse:name=MonitoringServices")
    // Config.getIntList("spatialbase.defaults.dimensions").foreach { dim =>
    //   BeanUtils.removeMBean(name =
    //     s"co.paralleluniverse:type=SpaceBase,name=spatialdb_${dim}D," +
    //     "monitor=performance,kind=transactions")
    //   BeanUtils.removeMBean(name =
    //     s"co.paralleluniverse:type=SpaceBase,name=spatialdb_${dim}D," +
    //     "monitor=performance,kind=general")
    // }
  }

  private def createStores(dims: List[Int],
                           gcs: IndexedActor,
                           execMode: ExecutionMode): IndexedActor = {
    val stores = collection.mutable.Map[Int, ActorRef]()
    def addToStore(dim: Int, actor: ActorRef): Unit = stores += dim -> actor
    def getName(dim: Int) =
      Config.getString("spatialbase.defaults.space.name") + s"_$dim" + "D"
    for (dim <- dims if dim == 2 || dim == 3) {
      val space = createSpace[ActorRef](dim, getName(dim), gcs(dim), execMode)
      addToStore(dim, space)
    }
    stores.toMap
  }

  private def createGCs(dims: List[Int]): IndexedActor = {
    val gcs = collection.mutable.Map[Int, ActorRef]()
    def addToStore(dim: Int, actor: ActorRef): Unit = gcs += dim -> actor
    def getName(dim: Int) =
      Config.getString("spatialbase.defaults.gc.name") + s"_$dim" + "D"
    for (dim <- dims if dim == 2 || dim == 3)
      addToStore(dim, createSpatialGC(dim, getName(dim)))
    gcs.toMap
  }

  /** Creates an spatial garbage collector
    * @param dimensions number of dimensions
    * @param name name of the GC, gets a default if none is sent
    * @return spatial GC
    */
  private def createSpatialGC(dimensions: Int,
                              name: String = ""): ActorRef = {
    context.actorOf(
      Props[SpatialGC], Option(name).getOrElse(s"gc-${dimensions}D"))
  }

  private def execModeFromText(t: String): ExecutionMode = t match {
    case "parallel" => Parallel
    case "concurrent" => Concurrent
    case _ =>
      log.warning("Incorrect configuration at: spatialbase.execution.mode.\n"+
                  "Allowed values are: concurrent, parallel.\n" +
                  "Defaulting to: concurrent.")
      Concurrent
  }

  private def executionModeForBuilder(e: ExecutionMode,
                                      b: SpaceBaseBuilder): Unit = e match {
    case Parallel => b.setExecutor(ParallelMode)
    case Concurrent => b.setExecutor(ConcurrentMode)
  }

  private def defaultExecMode = execModeFromText(
    Config.getString("spatialbase.execution.mode"))

  private def createAdmin(dbs: IndexedActor) =
    context.actorOf(
      GenericAdmin.props(dbs), Config.getString(
        "spatialbase.defaults.spatial.admin.name"))

}
