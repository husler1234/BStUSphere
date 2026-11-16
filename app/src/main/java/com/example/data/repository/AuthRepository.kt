package com.example.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
  private val auth: FirebaseAuth = Firebase.auth,
) {
  fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> =
    runCatching {
      val credential = GoogleAuthProvider.getCredential(idToken, null)
      val result = auth.signInWithCredential(credential).await()
      checkNotNull(result.user) { "Google Sign-In completed without an authenticated user." }
    }

  suspend fun signOut(context: Context) {
    withContext(NonCancellable) {
      auth.signOut()
      runCatching {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
      }
    }
  }
}
