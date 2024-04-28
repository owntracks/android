package org.owntracks.android.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.owntracks.android.databinding.ModeBottomSheetDialogBinding
import org.owntracks.android.preferences.types.MonitoringMode

class MonitoringModeBottomSheetDialog : BottomSheetDialogFragment() {
  private val viewModel: MapViewModel by activityViewModels()
  private lateinit var binding: ModeBottomSheetDialogBinding

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = ModeBottomSheetDialogBinding.inflate(inflater, container, false)
    mapOf(
            binding.fabMonitoringModeQuiet to MonitoringMode.QUIET,
            binding.fabMonitoringModeManual to MonitoringMode.MANUAL,
            binding.fabMonitoringModeSignificantChanges to MonitoringMode.SIGNIFICANT,
            binding.fabMonitoringModeMove to MonitoringMode.MOVE)
        .forEach {
          it.key.setOnClickListener { _ ->
            viewModel.setMonitoringMode(it.value)
            dismiss()
          }
        }
    return binding.root
  }
}
