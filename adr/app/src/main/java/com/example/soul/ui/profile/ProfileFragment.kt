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
import com.example.soul.ui.auth.ChangePasswordActivity
import com.example.soul.ui.collection.CollectionItemsActivity
import com.example.soul.ui.auth.LoginActivity
import com.example.soul.ui.profile.EditProfileActivity
import com.example.soul.ui.home.HomeViewModel
import com.example.soul.ui.home.HomeViewModelFactory
import com.example.soul.ui.admin.AdminReportsActivity
import com.example.soul.ui.main.adapter.CollectionAdapter
import com.example.soul.utils.AvatarLoader
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
                        binding.tvUsername.text = profile.displayName?.takeIf { it.isNotBlank() } ?: profile.username
                        binding.tvProfileLink.text = "@${profile.username}"
                        val bio = profile.bio?.trim().orEmpty()
                        if (bio.isNotEmpty()) {
                            binding.tvBio.visibility = View.VISIBLE
                            binding.tvBio.text = bio
                        } else {
                            binding.tvBio.visibility = View.GONE
                            binding.tvBio.text = ""
                        }
                        AvatarLoader.load(binding.ivAvatar, profile.avatarUrl)
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
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
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
        Toast.makeText(requireContext(), "Tính năng thêm bộ sưu tập sẽ có sớm", Toast.LENGTH_SHORT).show()
    }

    private fun showCollectionMenu(collection: Collection, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_collection_item, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        Toast.makeText(requireContext(), "Chỉnh sửa ${collection.name}", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_delete -> {
                        Toast.makeText(requireContext(), "Xóa ${collection.name}", Toast.LENGTH_SHORT).show()
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
            putExtra(Intent.EXTRA_TEXT, "Xem hồ sơ của tôi: ${binding.tvProfileLink.text}")
            type = "text/plain"
        }, "Chia sẻ hồ sơ"))
    }

    private fun shareCollection(collection: Collection) {
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Xem bộ sưu tập ${collection.name} của tôi!")
            type = "text/plain"
        }, "Chia sẻ bộ sưu tập"))
    }

    private fun showSettingsMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            val prefs = AuthPreferences(requireContext())
            menu.add("Đổi mật khẩu")
            if (prefs.getUser()?.role == "admin") {
                menu.add("Quản trị báo cáo")
            }
            menu.add("Đăng xuất")
            setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Đổi mật khẩu" -> {
                        startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
                        true
                    }
                    "Quản trị báo cáo" -> {
                        startActivity(Intent(requireContext(), AdminReportsActivity::class.java))
                        true
                    }
                    "Đăng xuất" -> {
                        performLogout()
                        true
                    }
                    else -> false
                }
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
