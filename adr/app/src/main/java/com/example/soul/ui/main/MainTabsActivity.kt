package com.example.soul.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.soul.R
import com.example.soul.databinding.ActivityMainTabsBinding
import com.example.soul.ui.explore.ExploreFragment
import com.example.soul.ui.home.FeedFragment
import com.example.soul.ui.notification.NotificationFragment
import com.example.soul.ui.profile.ProfileFragment

class MainTabsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainTabsBinding
    private var currentTag: String = TAG_HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, FeedFragment(), TAG_HOME)
                .commit()
            currentTag = TAG_HOME
        } else {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG, TAG_HOME)
        }

        binding.bottomNavigation.selectedItemId = if (currentTag == TAG_PROFILE) {
            R.id.nav_profile
        } else {
            R.id.nav_home
        }

        setupBottomNav()
    }

    private fun setupBottomNav() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchTo(TAG_HOME) { FeedFragment() }
                    true
                }
                R.id.nav_profile -> {
                    switchTo(TAG_PROFILE) { ProfileFragment() }
                    true
                }
                R.id.nav_explore -> {
                    switchTo(TAG_EXPLORE) { ExploreFragment() }
                    true
                }
                R.id.nav_notification -> {
                    switchTo(TAG_NOTIFICATION) { NotificationFragment() }
                    true
                }
                R.id.nav_library -> {
                    switchTo(TAG_LIBRARY) {
                        PlaceholderFragment.newInstance(
                            title = "Library",
                            subtitle = "Coming soon"
                        )
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun selectProfileTab() {
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
    }

    private fun switchTo(tag: String, factory: () -> Fragment) {
        if (currentTag == tag) return

        val fm = supportFragmentManager
        val current = fm.findFragmentByTag(currentTag)
        var target = fm.findFragmentByTag(tag)

        val tx = fm.beginTransaction()
        if (current != null) tx.hide(current)
        if (target == null) {
            target = factory()
            tx.add(R.id.fragmentContainer, target, tag)
        } else {
            tx.show(target)
        }
        tx.commit()
        currentTag = tag
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_CURRENT_TAG, currentTag)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_CURRENT_TAG = "current_tag"
        private const val TAG_HOME = "tab_home"
        private const val TAG_PROFILE = "tab_profile"
        private const val TAG_EXPLORE = "tab_explore"
        private const val TAG_NOTIFICATION = "tab_notification"
        private const val TAG_LIBRARY = "tab_library"
    }
}
