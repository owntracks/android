package org.owntracks.android.ui.map

/**
 * A factory that can supply a [MapFragment], usually to a [MapActivity]
 */
interface IMapFragmentFactory {
    /**
     * Returns a [MapFragment]. Either creates a new one, or optionally returns the supplied [MapFragment]
     * if the fragment that would be returned is of the same type as that supplied. This effectively
     * allows callers to state "I have one of these", and the factory to to state "keep using that"
     * rather than generating a new one unnecessarily.
     *
     * @param mapFragment optional [MapFragment] held by the caller that might be returned
     * @param mapLocationSource a [MapLocationSource] held by the caller needed to instantiate a new [MapFragment]
     * @return a [MapFragment] that the caller should be using
     */
    fun getMapFragment(mapFragment: MapFragment?, mapLocationSource: MapLocationSource): MapFragment
}