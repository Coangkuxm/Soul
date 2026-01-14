package com.example.soul.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.FeedItem
import com.example.soul.databinding.ActivityFeedBinding
import com.example.soul.ui.auth.LoginActivity
import com.example.soul.ui.home.adapter.FeedAdapter
import com.example.soul.utils.Resource

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var feedAdapter: FeedAdapter

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)

        // Check if user is logged in
        if (authPreferences.getToken().isNullOrEmpty()) {
            navigateToLogin()
            return
        }

        setupUI()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupUI() {
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.primary_dark
        )
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            onItemClick = { feedItem -> 
                onFeedItemClicked(feedItem) 
            },
            onUserClick = { userId ->
                onUserClicked(userId)
            }
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(this@FeedActivity)
            adapter = feedAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        // Observe profile for header avatar
        viewModel.profile.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { profile ->
                        // Load user avatar in header
                        val avatarUrl = profile.avatarUrl
                        val isValidUrl = !avatarUrl.isNullOrEmpty() && 
                            !avatarUrl.contains("example.com") &&
                            (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))
                        
                        if (isValidUrl) {
                            Glide.with(this@FeedActivity)
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .circleCrop()
                                .into(binding.ivUserAvatar)
                        } else {
                            binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                        }
                    }
                }
                else -> {}
            }
        }

        // Observe feed items
        viewModel.feedItems.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    resource.data?.let { items ->
                        if (items.isEmpty()) {
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.rvFeed.visibility = View.GONE
                        } else {
                            binding.layoutEmpty.visibility = View.GONE
                            binding.rvFeed.visibility = View.VISIBLE
                            feedAdapter.submitList(items)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvFeed.visibility = View.GONE
                }
            }
        }

        // Observe refresh state
        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }
    }

    private fun setupListeners() {
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        // User avatar click - go to own profile
        binding.ivUserAvatar.setOnClickListener {
            Toast.makeText(this, "Your profile coming soon", Toast.LENGTH_SHORT).show()
        }

        // Filter dropdown
        binding.btnFilter.setOnClickListener {
            showFilterMenu(it)
        }

        // Notification button
        binding.btnNotification.setOnClickListener {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        // FAB - Add new item
        binding.fabAdd.setOnClickListener {
            Toast.makeText(this, "Add item coming soon", Toast.LENGTH_SHORT).show()
        }

        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home/feed
                    true
                }
                R.id.nav_explore -> {
                    Toast.makeText(this, "Explore coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_library -> {
                    // Navigate to library/collections
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun showFilterMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Everyone")
            menu.add("Friends Only")
            setOnMenuItemClickListener { item ->
                binding.btnFilter.text = item.title
                // TODO: Filter feed based on selection
                viewModel.refresh()
                true
            }
            show()
        }
    }

    private fun onFeedItemClicked(feedItem: FeedItem) {
        Toast.makeText(this, "Opening ${feedItem.item.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to item detail or play
    }

    private fun onUserClicked(userId: Int) {
        Toast.makeText(this, "Opening user profile", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to user profile
    }

    private fun logout() {
        authPreferences.clearSession()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
