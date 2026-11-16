package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
)

class AuthViewModel(
  private val authRepository: AuthRepository = AuthRepository(),
) : ViewModel() {

  val currentUserFlow = authRepository.authStateFlow()

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  private var silentSignInAttempted = false

  fun attemptSilentSignIn(context: Context, webClientId: String) {
    if (silentSignInAttempted || webClientId.isBlank()) return
    silentSignInAttempted = true
    viewModelScope.launch {
      try {
        val googleIdOption = GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(true)
          .setServerClientId(webClientId)
          .setAutoSelectEnabled(true)
          .build()
        val request = GetCredentialRequest.Builder()
          .addCredentialOption(googleIdOption)
          .build()
        val result = CredentialManager.create(context).getCredential(context, request)
        handleCredentialResult(result.credential)
      } catch (e: NoCredentialException) {
        Log.d(TAG, "No authorized cached Google credential for silent sign-in")
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Silent Google Sign-In dismissed: ${e.message}", e)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Silent Google Sign-In skipped: ${e.message}", e)
      }
    }
  }

  fun signInWithGoogle(context: Context, webClientId: String) {
    if (webClientId.isBlank()) {
      _uiState.value = AuthUiState(errorMessage = "Missing Google Web Client ID configuration.")
      return
    }
    viewModelScope.launch {
      _uiState.value = AuthUiState(isLoading = true, errorMessage = null)
      try {
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
          .addCredentialOption(signInOption)
          .build()
        val result = CredentialManager.create(context).getCredential(context, request)
        handleCredentialResult(result.credential)
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
        _uiState.value = AuthUiState(isLoading = false, errorMessage = null)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        _uiState.value = AuthUiState(
          isLoading = false,
          errorMessage = e.localizedMessage ?: "Google Sign-In failed. Please try again.",
        )
      }
    }
  }

  private suspend fun handleCredentialResult(credential: androidx.credentials.Credential) {
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val googleToken = GoogleIdTokenCredential.createFrom(credential.data)
      val signInRes = authRepository.signInWithGoogleIdToken(googleToken.idToken)
      signInRes.fold(
        onSuccess = {
          _uiState.value = AuthUiState(isLoading = false, errorMessage = null)
        },
        onFailure = { err ->
          Log.e(TAG, "Firebase auth with Google token failed", err)
          _uiState.value = AuthUiState(
            isLoading = false,
            errorMessage = err.localizedMessage ?: "Firebase authentication failed.",
          )
        },
      )
    } else {
      _uiState.value = AuthUiState(
        isLoading = false,
        errorMessage = "Unsupported credential type returned.",
      )
    }
  }

  fun signOut(context: Context) {
    viewModelScope.launch {
      _uiState.value = AuthUiState(isLoading = true)
      authRepository.signOut(context)
      silentSignInAttempted = false
      _uiState.value = AuthUiState(isLoading = false)
    }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  private companion object {
    const val TAG = "AuthViewModel"
  }
}
