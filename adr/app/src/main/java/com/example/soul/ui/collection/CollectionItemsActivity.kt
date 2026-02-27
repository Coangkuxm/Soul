package com.example.soul.ui.collection

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.soul.R
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityCollectionItemsBinding
import kotlinx.coroutines.launch

class CollectionItemsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COLLECTION_ID = "extra_collection_id"
        const val EXTRA_COLLECTION_NAME = "extra_collection_name"
    }

    private lateinit var binding: ActivityCollectionItemsBinding
    private lateinit var adapter: CollectionContentAdapter
    private var collectionId: Int = -1
    private var collectionName: String = "Collection"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        collectionId = intent.getIntExtra(EXTRA_COLLECTION_ID, -1)
        collectionName = intent.getStringExtra(EXTRA_COLLECTION_NAME).orEmpty().ifBlank { "Collection" }

        if (collectionId <= 0) {
            Toast.makeText(this, "Collection không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUi()
        loadItems()
    }

    private fun setupUi() {
        binding.tvCollectionTitle.text = collectionName
        binding.swipeRefresh.setColorSchemeResources(R.color.primary, R.color.primary_dark)
        binding.btnBack.setOnClickListener { finish() }

        adapter = CollectionContentAdapter {
            Toast.makeText(this, "Opening item", Toast.LENGTH_SHORT).show()
        }
        binding.rvItems.layoutManager = GridLayoutManager(this, 2)
        binding.rvItems.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadItems() }
    }

    private fun loadItems() {
        lifecycleScope.launch {
            if (!binding.swipeRefresh.isRefreshing) {
                binding.progressBar.visibility = View.VISIBLE
            }
            binding.tvEmpty.visibility = View.GONE

            try {
                val response = RetrofitClient.apiService.getCollectionItems(
                    collectionId = collectionId,
                    page = 1,
                    limit = 100
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data.orEmpty()
                    adapter.submitList(items)
                    binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    adapter.submitList(emptyList())
                    binding.tvEmpty.visibility = View.VISIBLE
                    Toast.makeText(this@CollectionItemsActivity, "Không tải được item", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                binding.tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@CollectionItemsActivity, e.message ?: "Lỗi mạng", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
