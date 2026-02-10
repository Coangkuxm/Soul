package com.example.soul.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.Collection
import com.example.soul.databinding.ActivityHomeBinding
import com.example.soul.ui.auth.LoginActivity
import com.example.soul.ui.main.adapter.CollectionAdapter
import com.example.soul.utils.Resource

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var collectionAdapter: CollectionAdapter

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
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
        collectionAdapter = CollectionAdapter(
            onCollectionClick = { collection -> 
                onCollectionClicked(collection) 
            },
            onAddClick = { 
                onAddCollectionClicked() 
            },
            onMenuClick = { collection, view -> 
                showCollectionMenu(collection, view) 
            }
        )

        binding.rvCollections.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = collectionAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        // Observe profile data
        viewModel.profile.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading if needed
                }
                is Resource.Success -> {
                    resource.data?.let { profile ->
                        binding.apply {
                            tvUsername.text = profile.username
                            tvProfileLink.text = profile.profileUrl

                            // Load avatar - check if URL is valid
                            val avatarUrl = profile.avatarUrl
                            val isValidUrl = !avatarUrl.isNullOrEmpty() && 
                                !avatarUrl.contains("example.com") &&
                                (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))
                            
                            if (isValidUrl) {
                                Glide.with(this@HomeActivity)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .circleCrop()
                                    .into(ivAvatar)
                            } else {
                                ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe collections
        viewModel.collections.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    resource.data?.let { collections ->
                        collectionAdapter.submitCollections(collections)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                    // Show empty state with add placeholders
                    collectionAdapter.submitCollections(emptyList())
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

        // Edit profile button
        binding.btnEdit.setOnClickListener {
            Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show()
        }

        // Share button
        binding.btnShare.setOnClickListener {
            shareProfile()
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            showSettingsMenu(it)
        }

        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_explore -> {
                    Toast.makeText(this, "Explore coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_notification -> {
                    true
                }
                R.id.nav_library -> {
                    Toast.makeText(this, "Library coming soon", Toast.LENGTH_SHORT).show()
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

    private fun onCollectionClicked(collection: Collection) {
        Toast.makeText(this, "Opening ${collection.name}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to collection detail
    }

    private fun onAddCollectionClicked() {
        Toast.makeText(this, "Add collection coming soon", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to add collection screen
    }

    private fun showCollectionMenu(collection: Collection, anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_collection_item, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        Toast.makeText(this@HomeActivity, "Edit ${collection.name}", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_delete -> {
                        Toast.makeText(this@HomeActivity, "Delete ${collection.name}", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_share -> {
                        shareCollection(collection)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun shareProfile() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out my profile: ${binding.tvProfileLink.text}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share profile"))
    }

    private fun shareCollection(collection: Collection) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out my ${collection.name} collection!")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share collection"))
    }

    private fun showSettingsMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Logout")
            setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Logout" -> {
                        logout()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
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








