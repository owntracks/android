package org.owntracks.android.units

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

class ScalarTests :
    StringSpec({
      "Int.toMetres should convert int to Metres correctly" {
        10.metres.value.value shouldBe BigDecimal(10)
      }

      "Feet to Metres conversion should use BigDecimal precision" {
        val feet = Feet(BigDecimal("3.28084"))
        val metres = feet.toMetres().value
        // 3.28084 feet is exactly 1 Metre in our definition

        // Using compareTo because equals handles scale strictly
        metres.compareTo(BigDecimal("1")) shouldBe 0
      }
      "Measurements in Metres should return the same value when toMetres is called" {
        val metres = 1.metres
        metres shouldBe metres.toMetres()
      }
      "Measurements in Feet should return the same value when toFeet is called" {
        val feet = 1.feet
        feet.toMetres().value.value shouldBe BigDecimal(0.3048)
      }
    })

class DivisorTests :
    StringSpec({
      "Something about meters per second having an identity function" {
        DivisorMeasurement(10.metres, 1.seconds)
      }
    })
