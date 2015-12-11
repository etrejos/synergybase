package com.coredump.synergybase.spatial

import co.paralleluniverse.spacebase.AABB

/** Axis-Aligned Bounding Box.
  * @see co.paralleluniverse.spacebase.AABB
  */
object Aabb {

  /** Creates a new double-precision mutable 2-dimensional AABB. */
  def apply(minX: Double,
            maxX: Double,
            minY: Double,
            maxY: Double) = AABB.create(minX, maxX, minY, maxY)

  /** Creates a new double-precision mutable 2-dimensional AABB with the form
    * of a Point.
    */
  def apply(x: Double,
            y: Double) = AABB.create(x, x, y, y)

  /** Creates a new double-precision mutable 3-dimensional AABB. */
  def apply(minX: Double,
            maxX: Double,
            minY: Double,
            maxY: Double,
            minZ: Double,
            maxZ: Double) = AABB.create(minX, maxX, minY, maxY, minZ, maxZ)

  /** Creates a new double-precision mutable 3-dimensional AABB with the form
    * of a Point.
    */
  def apply(x: Double,
            y: Double,
            z: Double) = AABB.create(x, x, y, y, z, z)

  /** Creates a new single-precision mutable 2-dimensional AABB. */
  def apply(minX: Float,
            maxX: Float,
            minY: Float,
            maxY: Float) = AABB.create(minX, maxX, minY, maxY)

  /** Creates a new single-precision mutable 3-dimensional AABB. */
  def apply(minX: Float,
            maxX: Float,
            minY: Float,
            maxY: Float,
            minZ: Float,
            maxZ: Float) = AABB.create(minX, maxX, minY, maxY, minZ, maxZ)

  /** Creates a blank (invalid) double-precision mutable AABB. */
  def apply(dims: Int) = AABB.create(dims)

}
