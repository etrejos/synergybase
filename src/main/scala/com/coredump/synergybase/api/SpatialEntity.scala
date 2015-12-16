package com.coredump.synergybase.api

import com.coredump.synergybase.entity.GenericIdentifiable.GenericMap

import collection.immutable.Map

/** External contract for using the generic spatial API.
  * Example:
  *
  *   SpatialEntity(
  *     "rocket",
  *     "23422",
  *     Map(
  *       "destination" -> "unknown",
  *       "height" -> 51.1,
  *       "diameter" -> 3,
  *       "active" -> true
  *     ),
  *     Position[18.3556756, 18.3556756, // min, max X (degrees maybe but not limited to)
  *              80.3456756, 23.3456756, // min, max Y (degrees maybe but not limited to)
  *              3000, 30001] // min, max Z (meters)
  *   )
  *
  * @param genericType name of the type
  * @param id unique identifier within its type
  * @param data data associated with the object
  * @param position bounding box, see [[com.coredump.synergybase.spatial.Aabb]]
  */
case class SpatialEntity(genericType: String, id: Long, data: GenericMap, position: List[Double])
