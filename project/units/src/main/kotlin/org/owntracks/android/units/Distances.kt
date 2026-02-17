package org.owntracks.android.units

import java.math.BigDecimal
import java.math.MathContext

val FEET_PER_METER = BigDecimal("3.28084")
val FEET_PER_MILE = BigDecimal("5280")

interface Distance : Unit {
  fun toMetres(): Metres

  override fun toString(): String
}

data class Metres(val value: BigDecimal) : Distance {
  override fun toMetres(): Metres = this

  override fun toString(): String = "${value}m"
}

data class Feet(val value: BigDecimal) : Distance {
  override fun toMetres(): Metres = Metres(value.divide(FEET_PER_METER, MathContext.DECIMAL128))

  override fun toString(): String = "${value}ft"
}

data class Miles(val value: BigDecimal) : Distance {
  override fun toMetres(): Metres = Metres(value.multiply(FEET_PER_MILE).divide(FEET_PER_METER))

  override fun toString(): String = "${value}mi"
}

/**
 * This lets us convert any scalarMeasurement of type Distance we might be holding to metres.
 *
 * @param T
 * @return
 */
fun <T : Distance> ScalarMeasurement<T>.toMetres(): ScalarMeasurement<Metres> {
  return ScalarMeasurement(this.value.toMetres())
}

/** Some extension functions */
inline val Int.metres
  get(): ScalarMeasurement<Metres> = this.to()
inline val Int.feet
  get(): ScalarMeasurement<Feet> = this.to()
inline val Int.miles
  get(): ScalarMeasurement<Miles> = this.to()
inline val Double.metres
  get(): ScalarMeasurement<Metres> = this.to()
inline val Double.feet
  get(): ScalarMeasurement<Feet> = this.to()
inline val Double.miles
  get(): ScalarMeasurement<Miles> = this.to()
