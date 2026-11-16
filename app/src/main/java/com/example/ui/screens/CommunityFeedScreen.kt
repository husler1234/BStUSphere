package com.example.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CampusPost
import com.example.data.model.PostComment
import com.example.ui.viewmodel.UiState
import com.example.util.AttachmentHelper
import com.example.util.PickedAttachment
import kotlinx.coroutines.launch

private val postCategories = listOf(
  "all" to "All",
  "thought" to "Thoughts",
  "material" to "Materials",
  "question" to "Questions",
  "announcement" to "Campus",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
  postsState: UiState<List<CampusPost>>,
  commentsState: UiState<List<PostComment>>,
  currentUserId: String,
  onCreatePost: (
    title: String,
    body: String,
    category: String,
    subject: String?,
    attachmentName: String?,
    attachmentMimeType: String?,
    attachmentSizeBytes: Long?,
    attachmentTextPreview: String?,
    attachmentImageBase64: String?,
    resourceUrl: String?,
  ) -> Unit,
  onDeletePost: (String) -> Unit,
  onOpenComments: (String?) -> Unit,
  onAddComment: (postId: String, body: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedCategory by rememberSaveable { mutableStateOf("all") }
  var showCreateDialog by remember { mutableStateOf(false) }
  var activePostForComments by remember { mutableStateOf<CampusPost?>(null) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("create_post_fab"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_new_post))
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = innerPadding.calculateBottomPadding()),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        postCategories.forEach { (key, label) ->
          FilterChip(
            selected = selectedCategory == key,
            onClick = { selectedCategory = key },
            label = { Text(label) },
            modifier = Modifier.heightIn(min = 48.dp),
          )
        }
      }

      when (postsState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          ErrorCard(message = postsState.message)
        }
        is UiState.Success -> {
          val allPosts = postsState.data
          val filtered = remember(allPosts, selectedCategory) {
            if (selectedCategory == "all") allPosts else allPosts.filter { it.category == selectedCategory }
          }

          if (filtered.isEmpty()) {
            EmptyStateCard(
              icon = Icons.AutoMirrored.Rounded.MenuBook,
              title = stringResource(R.string.empty_posts_title),
              body = stringResource(R.string.empty_posts_body),
              actionLabel = stringResource(R.string.action_new_post),
              onAction = { showCreateDialog = true },
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(filtered, key = { it.id }) { post ->
                CampusPostCard(
                  post = post,
                  isOwner = post.authorId == currentUserId,
                  onDelete = { onDeletePost(post.id) },
                  onOpenComments = {
                    activePostForComments = post
                    onOpenComments(post.id)
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    CreatePostDialog(
      onDismiss = { showCreateDialog = false },
      onPublish = { title, body, category, subject, attachment, url ->
        onCreatePost(
          title,
          body,
          category,
          subject,
          attachment?.name,
          attachment?.mimeType,
          attachment?.sizeBytes,
          attachment?.textPreview,
          attachment?.imageBase64,
          url,
        )
        showCreateDialog = false
      },
    )
  }

  val commentPost = activePostForComments
  if (commentPost != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = {
        activePostForComments = null
        onOpenComments(null)
      },
      sheetState = sheetState,
    ) {
      PostCommentsSheetContent(
        post = commentPost,
        commentsState = commentsState,
        onSendComment = { text -> onAddComment(commentPost.id, text) },
      )
    }
  }
}

@Composable
private fun CampusPostCard(
  post: CampusPost,
  isOwner: Boolean,
  onDelete: () -> Unit,
  onOpenComments: () -> Unit,
) {
  val uriHandler = LocalUriHandler.current
  val decodedImage = remember(post.attachmentImageBase64) {
    AttachmentHelper.decodeBase64Image(post.attachmentImageBase64)
  }
  val relativeTime = remember(post.createdAt) {
    post.createdAt?.toDate()?.time?.let {
      DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    } ?: "Just now"
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("post_card_${post.id}"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = post.authorName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          val subMeta = listOfNotNull(post.authorFaculty, relativeTime).joinToString(" • ")
          Text(
            text = subMeta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            val catLabel = postCategories.firstOrNull { it.first == post.category }?.second ?: post.category
            Text(
              text = catLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          if (isOwner) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.cd_delete_item),
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }

      if (!post.subject.isNullOrBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = post.subject,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text = post.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = post.body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (decodedImage != null) {
        Image(
          bitmap = decodedImage,
          contentDescription = stringResource(R.string.cd_attachment_preview),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .clip(RoundedCornerShape(14.dp)),
        )
      }

      if (!post.attachmentName.isNullOrBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              val sizeStr = AttachmentHelper.formatBytes(post.attachmentSizeBytes)
              val label = if (sizeStr.isNotEmpty()) "${post.attachmentName} ($sizeStr)" else post.attachmentName
              Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (!post.attachmentTextPreview.isNullOrBlank()) {
              Text(
                text = post.attachmentTextPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      if (!post.resourceUrl.isNullOrBlank()) {
        AssistChip(
          onClick = {
            val url = if (post.resourceUrl.startsWith("http")) post.resourceUrl else "https://${post.resourceUrl}"
            runCatching { uriHandler.openUri(url) }
          },
          label = { Text("Open shared link", maxLines = 1, overflow = TextOverflow.Ellipsis) },
          leadingIcon = {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          modifier = Modifier.heightIn(min = 48.dp),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onOpenComments,
          modifier = Modifier.heightIn(min = 48.dp).testTag("post_comments_btn_${post.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("${post.commentsCount} replies • Join discussion")
        }
      }
    }
  }
}

@Composable
private fun CreatePostDialog(
  onDismiss: () -> Unit,
  onPublish: (
    title: String,
    body: String,
    category: String,
    subject: String?,
    attachment: PickedAttachment?,
    resourceUrl: String?,
  ) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var title by remember { mutableStateOf("") }
  var body by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("thought") }
  var subject by remember { mutableStateOf("") }
  var resourceUrl by remember { mutableStateOf("") }
  var pickedAttachment by remember { mutableStateOf<PickedAttachment?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        pickedAttachment = AttachmentHelper.readAttachment(context, uri)
      }
    }
  }

  val docPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        pickedAttachment = AttachmentHelper.readAttachment(context, uri)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Share on BStUsphere") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 460.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          postCategories.filter { it.first != "all" }.forEach { (key, label) ->
            FilterChip(
              selected = category == key,
              onClick = { category = key },
              label = { Text(label) },
            )
          }
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Headline *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("post_title_input"),
        )
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Course / Subject") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          label = { Text("Thoughts, summary, or question *") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("post_body_input"),
        )
        OutlinedTextField(
          value = resourceUrl,
          onValueChange = { resourceUrl = it },
          label = { Text("External file/drive link (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) {
            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Photo")
          }
          OutlinedButton(
            onClick = { docPickerLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) {
            Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("File")
          }
        }

        if (pickedAttachment != null) {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = pickedAttachment!!.name,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              IconButton(
                onClick = { pickedAttachment = null },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove attachment")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onPublish(
            title.trim(),
            body.trim(),
            category,
            subject.trim().ifEmpty { null },
            pickedAttachment,
            resourceUrl.trim().ifEmpty { null },
          )
        },
        enabled = title.isNotBlank() && body.isNotBlank(),
        modifier = Modifier.testTag("publish_post_button"),
      ) {
        Text(stringResource(R.string.action_new_post))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
private fun PostCommentsSheetContent(
  post: CampusPost,
  commentsState: UiState<List<PostComment>>,
  onSendComment: (String) -> Unit,
) {
  var replyText by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = post.title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
    HorizontalDivider()

    when (commentsState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Text(commentsState.message, color = MaterialTheme.colorScheme.error)
      }
      is UiState.Success -> {
        val list = commentsState.data
        if (list.isEmpty()) {
          Text(
            text = "No replies yet. Start the discussion!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(list, key = { it.id }) { comment ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                      text = comment.authorName,
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!comment.authorFaculty.isNullOrBlank()) {
                      Text(
                        text = comment.authorFaculty,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                  Text(
                    text = comment.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = replyText,
        onValueChange = { replyText = it },
        placeholder = { Text("Write a reply…") },
        modifier = Modifier.weight(1f).testTag("comment_input"),
      )
      IconButton(
        onClick = {
          onSendComment(replyText.trim())
          replyText = ""
        },
        enabled = replyText.isNotBlank(),
        modifier = Modifier.size(48.dp).testTag("send_comment_button"),
      ) {
        Icon(
          Icons.AutoMirrored.Rounded.Send,
          contentDescription = stringResource(R.string.cd_send_message),
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
