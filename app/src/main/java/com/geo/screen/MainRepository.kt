package com.geo.screen


import com.geo.auth.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

    suspend fun makeGroup(group: Group): Result<String> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(
                    IllegalStateException("User is not logged in")
                )

            val groupName = group.groupName.trim()

            if (groupName.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("Group name is required")
                )
            }

            val groupRef = firestore
                .collection("groups")
                .document()

            val ownerRef = groupRef
                .collection("members")
                .document(user.uid)

            val groupData = mapOf(
                "groupName" to groupName,
                "ownerId" to user.uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val ownerData = mapOf(
                "memberId" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "role" to MemberRole.OWNER.name,
                "joinedAt" to FieldValue.serverTimestamp()
            )

            firestore.batch().apply {
                set(groupRef, groupData)
                set(ownerRef, ownerData)
            }.commit().await()

            Result.success(groupRef.id)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
