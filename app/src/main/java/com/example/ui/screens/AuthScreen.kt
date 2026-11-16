package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
  authViewModel: AuthViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
  val webClientId = rememberWebClientId(context)

  LaunchedEffect(webClientId) {
    if (webClientId.isNotBlank()) {
      authViewModel.attemptSilentSignIn(context, webClientId)
    }
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 560.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        // Hero banner card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(24.dp)),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_campus_hero),
            contentDescription = stringResource(id = R.string.cd_campus_hero),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
                  )
                )
              )
          )
          Row(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.School,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onTertiary,
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
              )
              Text(
                text = stringResource(id = R.string.tagline_campus),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(id = R.string.auth_headline),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = stringResource(id = R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // Feature summary grid
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            AuthFeatureRow(
              icon = Icons.Rounded.LocationOn,
              title = stringResource(id = R.string.auth_feature_halls),
              subtitle = "Main Building, Academic Buildings 1–3 & Library rooms",
            )
            AuthFeatureRow(
              icon = Icons.Rounded.CalendarMonth,
              title = stringResource(id = R.string.auth_feature_schedule),
              subtitle = "Track daily lectures, seminars, and jump straight to rooms",
            )
            AuthFeatureRow(
              icon = Icons.AutoMirrored.Rounded.MenuBook,
              title = stringResource(id = R.string.auth_feature_materials),
              subtitle = "Post thoughts, whiteboard photos, notes, and study links",
            )
            AuthFeatureRow(
              icon = Icons.Rounded.Groups,
              title = stringResource(id = R.string.auth_feature_groups),
              subtitle = "Create public or private study circles and message peers",
            )
          }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = uiState.errorMessage.orEmpty(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        Button(
          onClick = { authViewModel.signInWithGoogle(context, webClientId) },
          enabled = !uiState.isLoading,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (uiState.isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = stringResource(id = R.string.auth_signing_in),
              style = MaterialTheme.typography.labelLarge,
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.AccountCircle,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(id = R.string.auth_sign_in_google),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AuthFeatureRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(42.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(22.dp),
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun rememberWebClientId(context: Context): String {
  val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
  return if (resId != 0) context.getString(resId) else ""
}
