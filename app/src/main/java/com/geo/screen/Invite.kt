package com.geo.screen

import com.google.firebase.firestore.DocumentId

data class Invite(
    @DocumentId val inviteId: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = ""
)
