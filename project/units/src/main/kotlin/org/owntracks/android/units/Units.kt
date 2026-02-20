package org.owntracks.android.units

class ScalarMeasurement<T : Unit>(val value: T)

class DivisorMeasurement<U : Unit, V : Unit>(val value: U)

class ProductMeasurement<U: Unit, V: Unit>(val value: U)

interface Unit
