package com.geo.screen

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class MemberRole { OWNER, MEMBER }

data class Group(
    @DocumentId val groupId: String = "",
    val groupName: String = "",
    val ownerId: String = "",
    // serverTimestamp() is stored as Timestamp, a Long here fails toObject()
    val createdAt: Timestamp? = null,
    val memberIds: List<String> = emptyList(),
    // null = owner has not set a geofence yet
    val fence: Fence? = null,
)

// Circle geofence: Android's Geofencing API only supports circles
data class Fence(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radiusM: Double = 300.0
)

data class Member(
    val memberId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: MemberRole = MemberRole.MEMBER,
    val joinedAt: Timestamp? = null
)
