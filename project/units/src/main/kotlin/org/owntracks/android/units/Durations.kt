package org.owntracks.android.units

import java.math.BigDecimal


interface Duration: Unit {
  override fun toString(): String
  fun toMilliseconds(): Milliseconds
}

data class Milliseconds(val value: BigDecimal) : Duration{
  override fun toString(): String = "${value}ms"
  override fun toMilliseconds(): Milliseconds = this
}

data class Seconds(val value: BigDecimal): Duration {
  override fun toString(): String = "${value}s"

  override fun toMilliseconds(): Milliseconds = Milliseconds(value.multiply(BigDecimal(1000)))
}

fun <T : Duration> ScalarMeasurement<T>.toMilliseconds(): ScalarMeasurement<Milliseconds> {
  return ScalarMeasurement(Milliseconds(this.value.toMilliseconds()))
}
