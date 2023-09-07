package org.owntracks.android.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.welcome.fragments.ConnectionSetupFragment
import org.owntracks.android.ui.welcome.fragments.FinishFragment
import org.owntracks.android.ui.welcome.fragments.IntroFragment
import org.owntracks.android.ui.welcome.fragments.LocationPermissionFragment
import org.owntracks.android.ui.welcome.fragments.NotificationPermissionFragment
import org.owntracks.android.ui.welcome.fragments.OSRestrictionsFragment
import org.owntracks.android.ui.welcome.fragments.WelcomeFragment

abstract class BaseWelcomeActivity : AppCompatActivity() {
    private val viewModel: WelcomeViewModel by viewModels()
    private lateinit var binding: UiWelcomeBinding
    abstract val fragmentList: List<WelcomeFragment>

    @Inject
    lateinit var requirementsChecker: RequirementsChecker

    @Inject
    lateinit var introFragment: IntroFragment

    @Inject
    lateinit var connectionSetupFragment: ConnectionSetupFragment

    @Inject
    lateinit var locationPermissionFragment: LocationPermissionFragment

    @Inject
    lateinit var notificationPermissionFragment: NotificationPermissionFragment

    @Inject
    lateinit var osRestrictionsFragment: OSRestrictionsFragment

    @Inject
    lateinit var finishFragment: FinishFragment

    @Inject
    lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (preferences.setupCompleted) {
            startActivity(
                Intent(this, MapActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            finish()
            return
        }

        binding =
            DataBindingUtil.setContentView<UiWelcomeBinding>(this, R.layout.ui_welcome)
                .apply {
                    vm = viewModel
                    lifecycleOwner = this@BaseWelcomeActivity
                    viewPager.adapter = WelcomeAdapter(this@BaseWelcomeActivity).apply {
                        addFragmentsToAdapter(this)
                    }
                    viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            viewModel.moveToPage(position)
                            super.onPageSelected(position)
                        }
                    })
                    btnNext.setOnClickListener { viewModel.nextPage() }
                    btnDone.setOnClickListener {
                        startActivity(
                            Intent(this@BaseWelcomeActivity, MapActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                }

        viewModel.currentFragmentPosition.observe(this) { position: Int ->
            binding.viewPager.currentItem = position
            setPagerIndicator(position)
        }

        buildPagerIndicator()
        onBackPressedDispatcher.addCallback(this) {
            if (binding.viewPager.currentItem == 0) {
                finish()
            } else {
                viewModel.previousPage()
            }
        }
    }

    private fun addFragmentsToAdapter(welcomeAdapter: WelcomeAdapter) {
        welcomeAdapter.setupFragments(fragmentList)
    }

    private fun setPagerIndicator(position: Int) {
        val itemCount = (binding.viewPager.adapter?.itemCount ?: 0) // TODO Can we globalize
        if (position < itemCount) {
            for (i in 0 until itemCount) {
                val circle = binding.circles.getChildAt(i) as ImageView
                if (i == position) {
                    circle.alpha = 1f
                } else {
                    circle.alpha = 0.5f
                }
            }
        }
    }

    private fun buildPagerIndicator() {
        val itemCount = (binding.viewPager.adapter?.itemCount ?: 0)
        val scale = resources.displayMetrics.density
        val padding = (5 * scale + 0.5f).toInt()
        for (i in 0 until itemCount) {
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
}
