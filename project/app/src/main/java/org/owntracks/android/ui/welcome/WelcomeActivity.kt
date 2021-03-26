package org.owntracks.android.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeBinding
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.navigator.Navigator
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.welcome.finish.FinishFragment
import org.owntracks.android.ui.welcome.intro.IntroFragment
import org.owntracks.android.ui.welcome.permission.PermissionFragment
import org.owntracks.android.ui.welcome.permission.PlayFragment
import org.owntracks.android.ui.welcome.version.VersionFragment
import timber.log.Timber
import javax.inject.Inject

class WelcomeActivity : BaseActivity<UiWelcomeBinding?, WelcomeViewModel?>(), WelcomeMvvm.View {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var requirementsChecker: RequirementsChecker

    private var welcomeAdapter: WelcomeAdapter? = null
    private var playFragment: PlayFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        welcomeAdapter = WelcomeAdapter(this, requirementsChecker)
        if (requirementsChecker.areRequirementsMet()) {
            navigator.startActivity(MapActivity::class.java, null, Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            finish()
            return
        }
        bindAndAttachContentView(R.layout.ui_welcome, savedInstanceState)
        setHasEventBus(false)

        playFragment = PlayFragment()
        welcomeAdapter!!.setupFragments(IntroFragment(), VersionFragment(), playFragment!!, PermissionFragment(), FinishFragment())

        binding!!.viewPager.isUserInputEnabled = true
        binding!!.viewPager.adapter = welcomeAdapter
        binding!!.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding!!.vm!!.currentFragmentPosition.value=position
                setPagerIndicator(position)
                refreshNextDoneButtons()
                super.onPageSelected(position)
            }
        })

        binding!!.vm!!.currentFragmentPosition.observe({ this.lifecycle }, { fragmentPosition: Int ->
            showFragment(fragmentPosition)
        })

        Timber.v("pager setup with %s fragments", welcomeAdapter!!.itemCount)
        buildPagerIndicator()
        showFragment(0)
    }

    override fun showNextFragment() {
        showFragment(binding!!.viewPager.currentItem + 1)
    }

    override fun setPagerIndicator(index: Int) {
        if (index < welcomeAdapter!!.itemCount) {
            for (i in 0 until welcomeAdapter!!.itemCount) {
                val circle = binding!!.circles.getChildAt(i) as ImageView
                if (i == index) {
                    circle.alpha = 1f
                } else {
                    circle.alpha = 0.5f
                }
            }
        }
    }

    private fun showFragment(position: Int) {
        binding!!.viewPager.currentItem = position
        welcomeAdapter!!.getFragment(binding!!.viewPager.currentItem).onShowFragment()
    }

    // TODO I really feel like we can replace this to auto refresh when the VM changes. Somehow.
    override fun refreshNextDoneButtons() {
        viewModel!!.nextEnabled = welcomeAdapter!!.getFragment(binding!!.viewPager.currentItem).isNextEnabled
        binding!!.viewPager.isUserInputEnabled = viewModel!!.nextEnabled
        viewModel!!.doneEnabled = binding!!.viewPager.currentItem == welcomeAdapter!!.lastItemPosition
    }

    private fun buildPagerIndicator() {
        val scale = resources.displayMetrics.density
        val padding = (5 * scale + 0.5f).toInt()
        for (i in 0 until welcomeAdapter!!.itemCount) {
            val circle = ImageView(this)
            circle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_fiber_manual_record_24))
            circle.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            circle.adjustViewBounds = true
            circle.setPadding(padding, 0, padding, 0)
            binding!!.circles.addView(circle)
        }
        setPagerIndicator(0)
    }

    override fun onBackPressed() {
        if (binding!!.viewPager.currentItem == 0) {
            finish()
        } else {
            binding!!.vm!!.moveBack()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PlayFragment.PLAY_SERVICES_RESOLUTION_REQUEST) {
            playFragment!!.onPlayServicesResolutionResult()
        }
    }
}