package com.example.soul.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityUserProfileBinding
import com.example.soul.ui.collection.CollectionItemsActivity
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
    }

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var adapter: UserCollectionAdapter
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        userId = intent.getIntExtra(EXTRA_USER_ID, -1)
        if (userId <= 0) {
            finish()
            return
        }

        setupUi()
        loadData()
    }

    private fun setupUi() {
        adapter = UserCollectionAdapter { collection ->
            startActivity(android.content.Intent(this, CollectionItemsActivity::class.java).apply {
                putExtra(CollectionItemsActivity.EXTRA_COLLECTION_ID, collection.id)
                putExtra(CollectionItemsActivity.EXTRA_COLLECTION_NAME, collection.name)
            })
        }
        binding.rvCollections.layoutManager = GridLayoutManager(this, 2)
        binding.rvCollections.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.swipeRefresh.setOnRefreshListener { loadData() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"

                val profileRs = RetrofitClient.apiService.getUserProfile(token, userId)
                if (profileRs.isSuccessful) {
                    val user = profileRs.body()?.user
                    if (user != null) {
                        binding.tvUsername.text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username
                        binding.tvLink.text = "shelf.im/${user.username}"
                        Glide.with(this@UserProfileActivity)
                            .load(user.avatarUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(binding.ivAvatar)
                    }
                }

                val collectionsRs = RetrofitClient.apiService.getCollections(
                    token = token,
                    userId = userId,
                    page = 1,
                    limit = 50
                )
                if (collectionsRs.isSuccessful) {
                    val data = collectionsRs.body()?.data.orEmpty()
                    adapter.submitList(data)
                    binding.tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    adapter.submitList(emptyList())
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                binding.tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@UserProfileActivity, e.message ?: "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
