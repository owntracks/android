package org.owntracks.android.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeBinding
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.welcome.finish.FinishFragment
import org.owntracks.android.ui.welcome.intro.IntroFragment
import org.owntracks.android.ui.welcome.version.VersionFragment
import javax.inject.Inject

abstract class BaseWelcomeActivity : AppCompatActivity() {
    @Inject
    lateinit var requirementsChecker: RequirementsChecker

    @Inject
    lateinit var introFragment: IntroFragment

    @Inject
    lateinit var versionFragment: VersionFragment

    @Inject
    lateinit var finishFragment: FinishFragment

    private lateinit var welcomeAdapter: WelcomeAdapter
    private lateinit var binding: UiWelcomeBinding
    private val viewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requirementsChecker.areRequirementsMet()) {
            setupCompletedAndStartMapActivity()
            finish()
            return
        }
        binding = DataBindingUtil.setContentView(this, R.layout.ui_welcome)
        binding.lifecycleOwner = this
        binding.vm = viewModel

        welcomeAdapter = WelcomeAdapter(this, requirementsChecker).apply { addFragmentsToAdapter(this) }

        binding.viewPager.adapter = welcomeAdapter
        binding.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.currentFragmentPosition.value = position
            }
        })

        binding.done.setOnClickListener {
            setupCompletedAndStartMapActivity()
        }

        viewModel.currentFragmentPosition.observe({ this.lifecycle }, { position: Int ->
            binding.viewPager.currentItem = position
            setPagerIndicator(position)
        })

        buildPagerIndicator()
    }

    abstract fun addFragmentsToAdapter(welcomeAdapter: WelcomeAdapter)

    private fun setupCompletedAndStartMapActivity() {
        viewModel.setSetupCompleted()
        startActivity(
                Intent(
                        this,
                        MapActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun setPagerIndicator(position: Int) {
        if (position < welcomeAdapter!!.itemCount) {
            for (i in 0 until welcomeAdapter!!.itemCount) {
                val circle = binding!!.circles.getChildAt(i) as ImageView
                if (i == position) {
                    circle.alpha = 1f
                } else {
                    circle.alpha = 0.5f
                }
            }
        }
    }

    private fun buildPagerIndicator() {
        val scale = resources.displayMetrics.density
        val padding = (5 * scale + 0.5f).toInt()
        for (i in 0 until welcomeAdapter.itemCount) {
            val circle = ImageView(this)
            circle.setImageDrawable(
                    ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_baseline_fiber_manual_record_24
                    )
            )
            circle.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            )
            circle.adjustViewBounds = true
            circle.setPadding(padding, 0, padding, 0)
            binding.circles.addView(circle)
        }
        setPagerIndicator(0)
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            finish()
        } else {
            viewModel.moveBack()
        }
    }
}