package com.example.soul.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.data.model.Collection
import com.example.soul.data.local.AuthPreferences
import com.example.soul.databinding.ActivityHomeBinding
import com.example.soul.ui.collection.CollectionItemsActivity
import com.example.soul.ui.auth.LoginActivity
import com.example.soul.ui.home.HomeViewModel
import com.example.soul.ui.home.HomeViewModelFactory
import com.example.soul.ui.main.adapter.CollectionAdapter
import com.example.soul.utils.Resource

class ProfileFragment : Fragment() {

    private var _binding: ActivityHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var collectionAdapter: CollectionAdapter

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Ensure avatar/info luôn cập nhật khi mở tab Profile
        viewModel.refreshProfileOnly()
    }

    private fun setupUI() {
        // Bottom nav and FAB are owned by MainTabsActivity
        binding.bottomNavigation.visibility = View.GONE
        binding.fabAdd.visibility = View.GONE
        binding.swipeRefresh.setColorSchemeResources(R.color.primary, R.color.primary_dark)
    }

    private fun setupRecyclerView() {
        collectionAdapter = CollectionAdapter(
            onCollectionClick = { onCollectionClicked(it) },
            onAddClick = { onAddCollectionClicked() },
            onMenuClick = { collection, anchor -> showCollectionMenu(collection, anchor) }
        )
        binding.rvCollections.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = collectionAdapter
            setHasFixedSize(false)
        }
        collectionAdapter.submitCollections(emptyList())
    }

    private fun setupObservers() {
        viewModel.profile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { profile ->
                        binding.tvUsername.text = profile.username
                        binding.tvProfileLink.text = profile.profileUrl
                        val avatarUrl = profile.avatarUrl
                        val isValidUrl = !avatarUrl.isNullOrEmpty() &&
                            !avatarUrl.contains("example.com") &&
                            (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))
                        if (isValidUrl) {
                            Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .circleCrop()
                                .into(binding.ivAvatar)
                        } else {
                            binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.collections.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    if (collectionAdapter.itemCount == 0) collectionAdapter.submitCollections(emptyList())
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    collectionAdapter.submitCollections(resource.data.orEmpty())
                    binding.rvCollections.requestLayout()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    collectionAdapter.submitCollections(emptyList())
                }
            }
        }

        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.btnEdit.setOnClickListener {
            Toast.makeText(requireContext(), "Edit profile coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnShare.setOnClickListener { shareProfile() }
        binding.btnSettings.setOnClickListener { showSettingsMenu(it) }
    }

    private fun onCollectionClicked(collection: Collection) {
        startActivity(Intent(requireContext(), CollectionItemsActivity::class.java).apply {
            putExtra(CollectionItemsActivity.EXTRA_COLLECTION_ID, collection.id)
            putExtra(CollectionItemsActivity.EXTRA_COLLECTION_NAME, collection.name)
        })
    }

    private fun onAddCollectionClicked() {
        Toast.makeText(requireContext(), "Add collection coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showCollectionMenu(collection: Collection, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_collection_item, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        Toast.makeText(requireContext(), "Edit ${collection.name}", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_delete -> {
                        Toast.makeText(requireContext(), "Delete ${collection.name}", Toast.LENGTH_SHORT).show()
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
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out my profile: ${binding.tvProfileLink.text}")
            type = "text/plain"
        }, "Share profile"))
    }

    private fun shareCollection(collection: Collection) {
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out my ${collection.name} collection!")
            type = "text/plain"
        }, "Share collection"))
    }

    private fun showSettingsMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add("Logout")
            setOnMenuItemClickListener { item ->
                if (item.title == "Logout") {
                    performLogout()
                    true
                } else false
            }
            show()
        }
    }

    private fun performLogout() {
        val prefs = AuthPreferences(requireContext())
        prefs.clearSession()
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finishAffinity()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
