package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

private const val PREFS_NAME = "bstusphere_prefs"
private const val KEY_DOWNLOAD_URL = "download_url"

@Composable
fun ShareAppDialog(
  onDismiss: () -> Unit,
  onNotify: (String) -> Unit,
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val prefs = remember(context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }
  val defaultUrl = stringResource(R.string.default_download_url)

  var downloadUrl by remember {
    val saved = prefs.getString(KEY_DOWNLOAD_URL, "").orEmpty()
    mutableStateOf(saved.ifBlank { defaultUrl })
  }

  val trimmedUrl = downloadUrl.trim()
  val isValidUrl = trimmedUrl.startsWith("https://", ignoreCase = true) ||
    trimmedUrl.startsWith("http://", ignoreCase = true)

  val qrMatrix = remember(trimmedUrl, isValidUrl) {
    if (!isValidUrl) null else encodeQrMatrix(trimmedUrl)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.share_dialog_title)) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.share_dialog_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = downloadUrl,
          onValueChange = { updated ->
            downloadUrl = updated
            prefs.edit().putString(KEY_DOWNLOAD_URL, updated.trim()).apply()
          },
          label = { Text("Website download URL") },
          placeholder = { Text("https://username.github.io/BStUsphere/") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("share_url_input"),
        )

        if (qrMatrix != null) {
          Surface(
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
              .size(200.dp)
              .testTag("share_qr_code"),
          ) {
            QrCodeCanvas(
              matrix = qrMatrix,
              modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            )
          }
          Text(
            text = "Ask a classmate to scan this QR code to open the download page.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.QrCode2,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "Publish your download page first",
                  style = MaterialTheme.typography.titleSmall,
                )
              }
              Text(
                text = "1. Push this project to GitHub.\n2. Enable GitHub Pages from the /docs folder.\n3. Paste your https://… link above to generate a live QR code and share link.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(trimmedUrl))
              onNotify("Download link copied")
            },
            enabled = isValidUrl,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("copy_download_link_button"),
          ) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.share_copy_link))
          }

          Button(
            onClick = {
              val shareText =
                "Join BStUsphere — find BStU lecture halls, track your class schedule, share notes, and join study groups:\n$trimmedUrl"
              val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Download BStUsphere for BStU")
                putExtra(Intent.EXTRA_TEXT, shareText)
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share BStUsphere"))
            },
            enabled = isValidUrl,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("send_download_link_button"),
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.share_send_link))
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    },
  )
}

@Composable
private fun QrCodeCanvas(
  matrix: BitMatrix,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val width = matrix.width
    val height = matrix.height
    if (width <= 0 || height <= 0) return@Canvas
    val cellWidth = size.width / width.toFloat()
    val cellHeight = size.height / height.toFloat()

    for (y in 0 until height) {
      for (x in 0 until width) {
        if (matrix.get(x, y)) {
          drawRect(
            color = Color.Black,
            topLeft = Offset(x * cellWidth, y * cellHeight),
            size = Size(cellWidth + 0.5f, cellHeight + 0.5f),
          )
        }
      }
    }
  }
}

internal fun encodeQrMatrix(content: String): BitMatrix? =
  runCatching {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
  }.getOrNull()
