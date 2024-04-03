package org.owntracks.android.ui.base

/**
 * An interface that allows a RecyclerView to notify that it's completed re-drawing its layout in
 * response to a change
 */
interface RecyclerViewLayoutCompleteListener {

  /**
   * An interface that allows an Activity to be used by an IdlingResource, so that tests can
   * checkpoint on the RecyclerView being re-drawn.
   */
  interface RecyclerViewIdlingCallback {
    fun setRecyclerViewLayoutCompleteListener(listener: RecyclerViewLayoutCompleteListener)

    fun removeRecyclerViewLayoutCompleteListener(listener: RecyclerViewLayoutCompleteListener)

    // Callback for the idling resource to check if the resource (in this example the activity
    // containing the recyclerview)
    // is idle
    var isRecyclerViewLayoutCompleted: Boolean
  }

  // Callback to notify the idling resource that it can transition to the idle state
  fun onLayoutCompleted()
}
