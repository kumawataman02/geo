
package com.geo.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.geo.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun signInWithGoogle(context: Context): User {
        val googleIdOption = GetGoogleIdOption.Builder().setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        val result = credentialManager.getCredential(
            context = context,
            request = request
        )

        val credential = result.credential

        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException(
                "Invalid Google credential received"
            )
        }

        val googleCredential =
            GoogleIdTokenCredential.createFrom(credential.data)

        val firebaseCredential = GoogleAuthProvider.getCredential(
            googleCredential.idToken,
            null
        )

        val authResult = auth
            .signInWithCredential(firebaseCredential)
            .await()

        val firebaseUser = authResult.user
            ?: throw IllegalStateException("Firebase user not found")

        return User(
            userId = firebaseUser.uid,
            userName = firebaseUser.displayName.orEmpty(),
            userImage = firebaseUser.photoUrl?.toString().orEmpty(),
            userEmail = firebaseUser.email.orEmpty()
        )
    }

    fun getCurrentUser(): User? {
        val user = auth.currentUser ?: return null

        return User(
            userId = user.uid,
            userName = user.displayName.orEmpty(),
            userImage = user.photoUrl?.toString().orEmpty(),
            userEmail = user.email.orEmpty()
        )
    }

    suspend fun saveUser(user: User) {
        val userData = mapOf(
            "uid" to user.userId,
            "name" to user.userName,
            "email" to user.userEmail.lowercase(),
            "image" to user.userImage,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(user.userId)
            .set(userData, SetOptions.merge())
            .await()
    }
}
