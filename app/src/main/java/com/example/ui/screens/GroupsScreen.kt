package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BstuCampusCatalog
import com.example.data.model.GroupMessage
import com.example.data.model.StudyGroup
import com.example.ui.viewmodel.UiState
import com.example.util.AttachmentHelper
import com.example.util.PickedAttachment
import kotlinx.coroutines.launch

@Composable
fun GroupsScreen(
  groupsState: UiState<List<StudyGroup>>,
  messagesState: UiState<List<GroupMessage>>,
  currentUserId: String,
  onCreateGroup: (
    name: String,
    subject: String?,
    faculty: String?,
    description: String?,
    meetingLocation: String?,
    visibility: String,
  ) -> Unit,
  onJoinGroup: (StudyGroup) -> Unit,
  onLeaveGroup: (StudyGroup) -> Unit,
  onDeleteGroup: (String) -> Unit,
  onOpenGroupChat: (StudyGroup?) -> Unit,
  onSendMessage: (
    group: StudyGroup,
    body: String,
    attachmentName: String?,
    attachmentMimeType: String?,
    attachmentImageBase64: String?,
    resourceUrl: String?,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  var activeGroup by remember { mutableStateOf<StudyGroup?>(null) }
  var filterTab by rememberSaveable { mutableStateOf("all") }
  var showCreateDialog by remember { mutableStateOf(false) }

  // Keep activeGroup synced with real-time updates (e.g., member count / list changes)
  val latestGroups = (groupsState as? UiState.Success)?.data.orEmpty()
  val syncedActiveGroup = activeGroup?.let { current ->
    latestGroups.firstOrNull { it.id == current.id } ?: current
  }

  if (syncedActiveGroup != null) {
    BackHandler {
      activeGroup = null
      onOpenGroupChat(null)
    }
    GroupChatView(
      group = syncedActiveGroup,
      messagesState = messagesState,
      currentUserId = currentUserId,
      onBack = {
        activeGroup = null
        onOpenGroupChat(null)
      },
      onSendMessage = { body, att ->
        onSendMessage(
          syncedActiveGroup,
          body,
          att?.name,
          att?.mimeType,
          att?.imageBase64,
          null,
        )
      },
      modifier = modifier,
    )
    return
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("create_group_fab"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_new_group))
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
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = filterTab == "all",
          onClick = { filterTab = "all" },
          label = { Text("All Groups") },
          modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
          selected = filterTab == "mine",
          onClick = { filterTab = "mine" },
          label = { Text("My Groups") },
          modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
          selected = filterTab == "discover",
          onClick = { filterTab = "discover" },
          label = { Text("Discover") },
          modifier = Modifier.heightIn(min = 48.dp),
        )
      }

      when (groupsState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          ErrorCard(message = groupsState.message)
        }
        is UiState.Success -> {
          val filtered = remember(groupsState.data, filterTab, currentUserId) {
            when (filterTab) {
              "mine" -> groupsState.data.filter { currentUserId in it.memberIds }
              "discover" -> groupsState.data.filter { currentUserId !in it.memberIds && it.visibility == "public" }
              else -> groupsState.data
            }
          }

          if (filtered.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Rounded.Groups,
              title = stringResource(R.string.empty_groups_title),
              body = stringResource(R.string.empty_groups_body),
              actionLabel = stringResource(R.string.action_new_group),
              onAction = { showCreateDialog = true },
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(filtered, key = { it.id }) { group ->
                val isMember = currentUserId in group.memberIds
                val isCreator = group.createdBy == currentUserId
                StudyGroupCard(
                  group = group,
                  isMember = isMember,
                  isCreator = isCreator,
                  onJoin = { onJoinGroup(group) },
                  onLeave = { onLeaveGroup(group) },
                  onDelete = { onDeleteGroup(group.id) },
                  onOpenChat = {
                    activeGroup = group
                    onOpenGroupChat(group)
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
    CreateGroupDialog(
      onDismiss = { showCreateDialog = false },
      onCreate = { name, subject, faculty, desc, location, vis ->
        onCreateGroup(name, subject, faculty, desc, location, vis)
        showCreateDialog = false
      },
    )
  }
}

@Composable
private fun StudyGroupCard(
  group: StudyGroup,
  isMember: Boolean,
  isCreator: Boolean,
  onJoin: () -> Unit,
  onLeave: () -> Unit,
  onDelete: () -> Unit,
  onOpenChat: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("group_card_${group.id}"),
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
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = if (group.visibility == "private") Icons.Rounded.Lock else Icons.Rounded.Public,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = group.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "${group.memberCount} members",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          if (isCreator) {
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

      if (!group.subject.isNullOrBlank() || !group.meetingLocation.isNullOrBlank()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (!group.subject.isNullOrBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = group.subject,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
          if (!group.meetingLocation.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(
                Icons.Rounded.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.tertiary,
              )
              Text(
                text = group.meetingLocation,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      if (!group.description.isNullOrBlank()) {
        Text(
          text = group.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (!group.lastMessagePreview.isNullOrBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = group.lastMessagePreview,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (isMember) {
          Button(
            onClick = onOpenChat,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("open_group_chat_${group.id}"),
          ) {
            Text(stringResource(R.string.action_open_chat))
          }
          if (!isCreator) {
            OutlinedButton(
              onClick = onLeave,
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text(stringResource(R.string.action_leave))
            }
          }
        } else {
          Button(
            onClick = onJoin,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("join_group_${group.id}"),
          ) {
            Text(stringResource(R.string.action_join))
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupChatView(
  group: StudyGroup,
  messagesState: UiState<List<GroupMessage>>,
  currentUserId: String,
  onBack: () -> Unit,
  onSendMessage: (body: String, attachment: PickedAttachment?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var inputText by remember { mutableStateOf("") }
  var pendingAttachment by remember { mutableStateOf<PickedAttachment?>(null) }

  val photoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      scope.launch { pendingAttachment = AttachmentHelper.readAttachment(context, uri) }
    }
  }
  val docLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      scope.launch { pendingAttachment = AttachmentHelper.readAttachment(context, uri) }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              text = "${group.memberCount} members • ${group.meetingLocation ?: "BStU Study Circle"}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp).testTag("group_chat_back"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (messagesState) {
          is UiState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
          }
          is UiState.Error -> {
            ErrorCard(message = messagesState.message)
          }
          is UiState.Success -> {
            val msgs = messagesState.data
            if (msgs.isEmpty()) {
              Text(
                text = "Say hello to your group and share study notes or lecture room updates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                items(msgs, key = { it.id }) { msg ->
                  val isMine = msg.authorId == currentUserId
                  val decoded = remember(msg.attachmentImageBase64) {
                    AttachmentHelper.decodeBase64Image(msg.attachmentImageBase64)
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                  ) {
                    Surface(
                      color = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                      shape = RoundedCornerShape(16.dp),
                      modifier = Modifier.widthIn(max = 310.dp),
                    ) {
                      Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        if (!isMine) {
                          Text(
                            text = msg.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                          )
                        }
                        if (decoded != null) {
                          Image(
                            bitmap = decoded,
                            contentDescription = stringResource(R.string.cd_attachment_preview),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                              .fillMaxWidth()
                              .heightIn(max = 180.dp)
                              .clip(RoundedCornerShape(10.dp)),
                          )
                        }
                        if (!msg.attachmentName.isNullOrBlank()) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                          ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                              text = msg.attachmentName,
                              style = MaterialTheme.typography.labelMedium,
                            )
                          }
                        }
                        Text(
                          text = msg.body,
                          style = MaterialTheme.typography.bodyMedium,
                          color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      HorizontalDivider()

      if (pendingAttachment != null) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Attached: ${pendingAttachment!!.name}",
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            IconButton(
              onClick = { pendingAttachment = null },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Remove attachment")
            }
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = {
            photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.size(48.dp),
        ) {
          Icon(Icons.Rounded.Image, contentDescription = stringResource(R.string.action_attach_photo))
        }
        IconButton(
          onClick = { docLauncher.launch(arrayOf("*/*")) },
          modifier = Modifier.size(48.dp),
        ) {
          Icon(Icons.Rounded.AttachFile, contentDescription = stringResource(R.string.action_attach_file))
        }
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Message group…") },
          maxLines = 3,
          modifier = Modifier.weight(1f).testTag("group_message_input"),
        )
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
          onClick = {
            onSendMessage(inputText.trim(), pendingAttachment)
            inputText = ""
            pendingAttachment = null
          },
          enabled = inputText.isNotBlank() || pendingAttachment != null,
          modifier = Modifier.size(48.dp).testTag("send_group_message_button"),
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
}

@Composable
private fun CreateGroupDialog(
  onDismiss: () -> Unit,
  onCreate: (
    name: String,
    subject: String?,
    faculty: String?,
    description: String?,
    meetingLocation: String?,
    visibility: String,
  ) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf("") }
  var faculty by remember { mutableStateOf(BstuCampusCatalog.faculties.first()) }
  var description by remember { mutableStateOf("") }
  var meetingLocation by remember { mutableStateOf("") }
  var visibility by remember { mutableStateOf("public") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create BStU Study Group") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 430.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Group name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("group_name_input"),
        )
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Course / Topic") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = meetingLocation,
          onValueChange = { meetingLocation = it },
          label = { Text("Meetup room (e.g. Main • 312)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Visibility", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = visibility == "public",
            onClick = { visibility = "public" },
            label = { Text("Public (All BStU)") },
          )
          FilterChip(
            selected = visibility == "private",
            onClick = { visibility = "private" },
            label = { Text("Private") },
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onCreate(
            name.trim(),
            subject.trim().ifEmpty { null },
            faculty.trim().ifEmpty { null },
            description.trim().ifEmpty { null },
            meetingLocation.trim().ifEmpty { null },
            visibility,
          )
        },
        enabled = name.trim().length >= 2,
        modifier = Modifier.testTag("save_group_button"),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}
