package com.geo.screen


enum class MemberRole(
    val role: String
) {
    OWNER("OWNER"),
    MEMBER("MEMBER")
}

data class Group(
    val groupId: String = "",
    val groupName: String = "",
    val ownerId: String = "",
    val createdAt: Long? = null,
    val members: List<Member> = emptyList()
)

data class Member(
    val memberId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: MemberRole = MemberRole.MEMBER,
    val joinedAt: Long? = null
)

