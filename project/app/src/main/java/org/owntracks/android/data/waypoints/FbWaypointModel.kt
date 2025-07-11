package org.owntracks.android.data.waypoints

import com.google.flatbuffers.Constants
import com.google.flatbuffers.FlatBufferBuilder
import com.google.flatbuffers.Table
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("unused")
/**
 * Flatbuffer waypoint model. Used to deserialize the objectbox-serialized waypoints used in
 * previous versions of the app.
 *
 * Automatically generated by the FlatBuffers compiler, do not modify.
 *
 * @constructor Create empty Fb waypoint model
 */
class FbWaypointModel : Table() {

  fun init(_i: Int, _bb: ByteBuffer) {
    __reset(_i, _bb)
  }

  fun assign(_i: Int, _bb: ByteBuffer): FbWaypointModel {
    init(_i, _bb)
    return this
  }

  val id: Long
    get() {
      val o = __offset(4)
      return if (o != 0) bb.getLong(o + bb_pos) else 0L
    }

  val description: String?
    get() {
      val o = __offset(6)
      return if (o != 0) {
        __string(o + bb_pos)
      } else {
        null
      }
    }

  val descriptionAsByteBuffer: ByteBuffer
    get() = __vector_as_bytebuffer(6, 1)

  fun descriptionInByteBuffer(_bb: ByteBuffer): ByteBuffer = __vector_in_bytebuffer(_bb, 6, 1)

  val geofenceLatitude: Double
    get() {
      val o = __offset(8)
      return if (o != 0) bb.getDouble(o + bb_pos) else 0.0
    }

  val geofenceLongitude: Double
    get() {
      val o = __offset(10)
      return if (o != 0) bb.getDouble(o + bb_pos) else 0.0
    }

  val geofenceRadius: Int
    get() {
      val o = __offset(12)
      return if (o != 0) bb.getInt(o + bb_pos) else 0
    }

  val lastTriggered: Long
    get() {
      val o = __offset(14)
      return if (o != 0) bb.getLong(o + bb_pos) else 0L
    }

  val lastTransition: Int
    get() {
      val o = __offset(16)
      return if (o != 0) bb.getInt(o + bb_pos) else 0
    }

  val tst: Long
    get() {
      val o = __offset(18)
      return if (o != 0) bb.getLong(o + bb_pos) else 0L
    }

  override fun toString(): String {
    return "FbWaypointModel(id=$id,description=$description,latitude=$geofenceLatitude,longitude=$geofenceLongitude,radius=$geofenceRadius,lastTransition=$lastTransition,lastTriggered=$lastTriggered,tst=$tst)"
  }

  companion object {
    fun validateVersion() = Constants.FLATBUFFERS_23_5_26()

    fun getRootAsFbWaypointModel(_bb: ByteBuffer): FbWaypointModel =
        getRootAsFbWaypointModel(_bb, FbWaypointModel())

    private fun getRootAsFbWaypointModel(_bb: ByteBuffer, obj: FbWaypointModel): FbWaypointModel {
      _bb.order(ByteOrder.LITTLE_ENDIAN)
      return (obj.assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
    }

    fun createFbWaypointModel(
        builder: FlatBufferBuilder,
        id: Long,
        descriptionOffset: Int,
        geofenceLatitude: Double,
        geofenceLongitude: Double,
        geofenceRadius: Int,
        lastTriggered: Long,
        lastTransition: Int,
        tst: Long
    ): Int {
      builder.startTable(8)
      addTst(builder, tst)
      addLastTriggered(builder, lastTriggered)
      addGeofenceLongitude(builder, geofenceLongitude)
      addGeofenceLatitude(builder, geofenceLatitude)
      addId(builder, id)
      addLastTransition(builder, lastTransition)
      addGeofenceRadius(builder, geofenceRadius)
      addDescription(builder, descriptionOffset)
      return endFbWaypointModel(builder)
    }

    fun startFbWaypointModel(builder: FlatBufferBuilder) = builder.startTable(8)

    private fun addId(builder: FlatBufferBuilder, id: Long) = builder.addLong(0, id, 0L)

    private fun addDescription(builder: FlatBufferBuilder, description: Int) =
        builder.addOffset(1, description, 0)

    private fun addGeofenceLatitude(builder: FlatBufferBuilder, geofenceLatitude: Double) =
        builder.addDouble(2, geofenceLatitude, 0.0)

    private fun addGeofenceLongitude(builder: FlatBufferBuilder, geofenceLongitude: Double) =
        builder.addDouble(3, geofenceLongitude, 0.0)

    private fun addGeofenceRadius(builder: FlatBufferBuilder, geofenceRadius: Int) =
        builder.addInt(4, geofenceRadius, 0)

    private fun addLastTriggered(builder: FlatBufferBuilder, lastTriggered: Long) =
        builder.addLong(5, lastTriggered, 0L)

    private fun addLastTransition(builder: FlatBufferBuilder, lastTransition: Int) =
        builder.addInt(6, lastTransition, 0)

    private fun addTst(builder: FlatBufferBuilder, tst: Long) = builder.addLong(7, tst, 0L)

    private fun endFbWaypointModel(builder: FlatBufferBuilder): Int {
      return builder.endTable()
    }

    fun finishFbWaypointModelBuffer(builder: FlatBufferBuilder, offset: Int) =
        builder.finish(offset)

    fun finishSizePrefixedFbWaypointModelBuffer(builder: FlatBufferBuilder, offset: Int) =
        builder.finishSizePrefixed(offset)
  }
}
