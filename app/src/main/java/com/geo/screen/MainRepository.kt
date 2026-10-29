package com.geo.screen


import com.geo.auth.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun searchUserByEmail(
        email: String
    ): Result<User?> = runCatching {
        val document = firestore
            .collection("users")
            .whereEqualTo("email", email.trim().lowercase())
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull() ?: return@runCatching null

        // Fields match LoginRepository.saveUser
        User(
            userId = document.getString("uid").orEmpty(),
            userName = document.getString("name").orEmpty(),
            userEmail = document.getString("email").orEmpty(),
            userImage = document.getString("image").orEmpty()
        )
    }

    // Group, owner and all invites are written in one batch: all or nothing
    suspend fun makeGroup(groupName: String, invitees: List<User>): Result<String> = runCatching {
        val me = auth.currentUser ?: error("User is not logged in")
        val name = groupName.trim()
        require(name.isNotBlank()) { "Group name is required" }

        val groupRef = firestore.collection("groups").document()

        firestore.batch().apply {
            set(
                groupRef, mapOf(
                    "groupName" to name,
                    "ownerId" to me.uid,
                    "memberIds" to listOf(me.uid),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            set(groupRef.collection("members").document(me.uid), memberData(me, MemberRole.OWNER))

            invitees.filter { it.userId != me.uid }.distinctBy { it.userId }.forEach { friend ->
                // Deterministic id: one pending invite per user per group
                set(
                    firestore.collection("invites").document("${groupRef.id}_${friend.userId}"),
                    mapOf(
                        "groupId" to groupRef.id,
                        "groupName" to name,
                        "fromUid" to me.uid,
                        "fromName" to me.displayName.orEmpty(),
                        "toUid" to friend.userId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.commit().await()

        groupRef.id
    }

    // An invite doc existing = pending. Accept/reject both delete it.
    fun observeMyInvites(): Flow<List<Invite>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return firestore.collection("invites")
            .whereEqualTo("toUid", uid)
            .snapshots()
            .map { it.toObjects(Invite::class.java) }
    }

    // No orderBy: arrayContains + orderBy needs a composite index, sort in UI instead
    fun observeMyGroups(): Flow<List<Group>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return firestore.collection("groups")
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { it.toObjects(Group::class.java) }
    }

    suspend fun acceptInvite(invite: Invite): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("User is not logged in")
        val groupRef = firestore.collection("groups").document(invite.groupId)
        firestore.batch().apply {
            set(groupRef.collection("members").document(me.uid), memberData(me, MemberRole.MEMBER))
            update(groupRef, "memberIds", FieldValue.arrayUnion(me.uid))
            delete(firestore.collection("invites").document(invite.inviteId))
        }.commit().await()
    }

    suspend fun rejectInvite(invite: Invite): Result<Unit> = runCatching<Unit> {
        firestore.collection("invites").document(invite.inviteId).delete().await()
    }

    val myUid: String? get() = auth.currentUser?.uid

    suspend fun getGroup(groupId: String): Result<Group> = runCatching {
        firestore.collection("groups").document(groupId).get().await()
            .toObject(Group::class.java) ?: error("Group not found")
    }

    // update(), not set(): only the fence field changes, memberIds etc. stay
    suspend fun setFence(groupId: String, fence: Fence): Result<Unit> = runCatching<Unit> {
        firestore.collection("groups").document(groupId).update(
            "fence", mapOf(
                "lat" to fence.lat,
                "lng" to fence.lng,
                "radiusM" to fence.radiusM
            )
        ).await()
    }

    private fun memberData(user: FirebaseUser, role: MemberRole) = mapOf(
        "memberId" to user.uid,
        "email" to user.email.orEmpty().lowercase(),
        "displayName" to user.displayName.orEmpty(),
        "role" to role.name,
        "joinedAt" to FieldValue.serverTimestamp()
    )
}
