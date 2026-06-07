package com.example.soul.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.soul.data.model.AdminReportRow
import com.example.soul.databinding.ItemAdminReportBinding

class AdminReportsAdapter(
    private val onPrimaryAction: (AdminReportRow) -> Unit
) : RecyclerView.Adapter<AdminReportsAdapter.ReportViewHolder>() {

    private val items = mutableListOf<AdminReportRow>()

    fun submitList(data: List<AdminReportRow>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemAdminReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ReportViewHolder(
        private val binding: ItemAdminReportBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AdminReportRow) {
            when (item) {
                is AdminReportRow.Post -> {
                    val post = item.value
                    binding.tvTitle.text = post.itemTitle ?: "Bài viết #${post.collectionItemId}"
                    binding.tvSubtitle.text =
                        listOfNotNull(
                            post.collectionName?.let { "Bộ sưu tập: $it" },
                            post.ownerUsername?.let { "Chủ sở hữu: @$it" }
                        ).joinToString(" • ")
                    binding.tvMeta.text =
                        "Báo cáo: ${post.reportCount} • ${post.reasonCodes.orEmpty().joinToString(", ")}"
                    val locked = post.moderationStatus == "locked"
                    binding.btnPrimary.text = if (locked) "Mở khóa" else "Khóa"
                    binding.tvStatus.text = if (locked) {
                        "Đã khóa${post.lockedReason?.let { " • $it" } ?: ""}"
                    } else {
                        "Đang hoạt động"
                    }
                }
                is AdminReportRow.User -> {
                    val user = item.value
                    binding.tvTitle.text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username
                    binding.tvSubtitle.text = "@${user.username} • ${user.role ?: "user"}"
                    binding.tvMeta.text =
                        "Báo cáo: ${user.reportCount} • ${user.reasonCodes.orEmpty().joinToString(", ")}"
                    val locked = user.accountStatus == "locked"
                    binding.btnPrimary.text = if (locked) "Mở khóa" else "Khóa"
                    binding.tvStatus.text = if (locked) {
                        "Đã khóa${user.lockedReason?.let { " • $it" } ?: ""}"
                    } else {
                        "Đang hoạt động"
                    }
                }
            }

            binding.btnPrimary.setOnClickListener { onPrimaryAction(item) }
        }
    }
}
