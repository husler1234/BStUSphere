package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BstuAppShell
import com.example.ui.theme.BStUsphereTheme
import com.example.ui.viewmodel.AuthViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      BStUsphereTheme {
        val context = LocalContext.current
        val authViewModel: AuthViewModel = viewModel()
        val currentUser by authViewModel.currentUserFlow.collectAsStateWithLifecycle(
          initialValue = Firebase.auth.currentUser
        )

        val user = currentUser
        if (user == null) {
          AuthScreen(authViewModel = authViewModel)
        } else {
          key(user.uid) {
            BstuAppShell(
              user = user,
              onSignOut = { authViewModel.signOut(context) },
            )
          }
        }
      }
    }
  }
}
