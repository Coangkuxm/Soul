package com.example.soul.ui.explore

import com.example.soul.data.model.ExploreUser

/**
 * Một dòng trong danh sách Explore: tiêu đề nhóm hoặc một người dùng.
 */
sealed class ExploreRow {
    data class Header(val title: String) : ExploreRow()

    /**
     * [followingSnapshot] chụp lại trạng thái follow tại thời điểm dựng list để DiffUtil
     * phát hiện thay đổi và rebind nút (ExploreUser là object mutable, dùng chung tham chiếu).
     */
    data class UserItem(
        val user: ExploreUser,
        val followingSnapshot: Boolean = user.isFollowing
    ) : ExploreRow()
}
