package org.owntracks.android.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.owntracks.android.R

class MapLayerBottomSheetDialog : BottomSheetDialogFragment() {
  private val viewModel: MapViewModel by activityViewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val rootView = inflater.inflate(R.layout.map_layer_bottom_sheet_dialog, container, false)
    mapLayerSelectorButtonsToStyles.forEach {
      rootView.findViewById<AppCompatImageButton>(it.key).setOnClickListener { _ ->
        val currentMapLayerStyle = viewModel.mapLayerStyle.value
        val newMapLayerStyle = it.value
        viewModel.setMapLayerStyle(it.value)
        if (currentMapLayerStyle?.isSameProviderAs(newMapLayerStyle) != true) {
          // Replace the map fragment
          val mapFragment =
              parentFragmentManager.fragmentFactory.instantiate(
                  requireActivity().classLoader, MapFragment::class.java.name)
          parentFragmentManager.commit(true) { replace(R.id.mapFragment, mapFragment, "map") }
        }

        dismiss()
      }
    }
    return rootView
  }
}
