package org.owntracks.android.units

import java.math.BigDecimal
import kotlin.reflect.full.primaryConstructor

class ScalarMeasurement<T : Unit>(val value: T)

class DivisorMeasurement<U : Unit, V : Unit>(val value: U)

interface Unit

/**
 * Implementation of converting ints to Scalarmeasurements.
 *
 * @param T
 * @return
 */
inline fun <reified T : Unit> Int.to(): ScalarMeasurement<T> {
  val constructor =
      T::class.primaryConstructor
          ?: throw IllegalArgumentException(
              "Type ${T::class.simpleName} must have a primary constructor")
  return ScalarMeasurement(constructor.call(BigDecimal(this)))
}

/**
 * Implementation of converting doubles to Scalarmeasurements.
 *
 * @param T
 * @return
 */
inline fun <reified T : Unit> Double.to(): ScalarMeasurement<T> {
  val constructor =
      T::class.primaryConstructor
          ?: throw IllegalArgumentException(
              "Type ${T::class.simpleName} must have a primary constructor")
  return ScalarMeasurement(constructor.call(BigDecimal(this)))
}

inline fun <reified T: Unit, reified V: Unit> Int.to(): DivisorMeasurement<T, V> {
  val constructor =
      T::class.primaryConstructor
        ?: throw IllegalArgumentException(
            "Type ${T::class.simpleName} must have a primary constructor")
  return DivisorMeasurement(constructor.call(BigDecimal(this)))
}
