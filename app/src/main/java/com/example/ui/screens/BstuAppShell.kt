package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.BstuCampusCatalog
import com.example.data.model.LectureHall
import com.example.data.model.UserProfile
import com.example.data.repository.CommunityAndGroupRepository
import com.example.data.repository.ScheduleAndHallRepository
import com.example.ui.viewmodel.BstuMainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

private enum class BstuDestination(
  val route: String,
  val labelRes: Int,
  val icon: ImageVector,
) {
  SCHEDULE("schedule", R.string.nav_schedule, Icons.Rounded.CalendarMonth),
  HALLS("halls", R.string.nav_halls, Icons.Rounded.Apartment),
  SPHERE("sphere", R.string.nav_sphere, Icons.AutoMirrored.Rounded.MenuBook),
  GROUPS("groups", R.string.nav_groups, Icons.Rounded.Groups),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BstuAppShell(
  user: FirebaseUser,
  onSignOut: () -> Unit,
) {
  val viewModel: BstuMainViewModel = viewModel(
    key = user.uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        BstuMainViewModel(
          scheduleRepo = ScheduleAndHallRepository(db),
          communityRepo = CommunityAndGroupRepository(db),
          currentUserId = user.uid,
          defaultDisplayName = user.displayName ?: "BStU Student",
          defaultEmail = user.email,
          defaultPhotoUrl = user.photoUrl?.toString(),
        )
      }
    },
  )

  val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val schedulesState by viewModel.schedules.collectAsStateWithLifecycle()
  val lectureHallsState by viewModel.lectureHalls.collectAsStateWithLifecycle()
  val postsState by viewModel.posts.collectAsStateWithLifecycle()
  val groupsState by viewModel.groups.collectAsStateWithLifecycle()
  val commentsState by viewModel.activePostComments.collectAsStateWithLifecycle()
  val messagesState by viewModel.activeGroupMessages.collectAsStateWithLifecycle()
  val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  var currentTab by rememberSaveable { mutableStateOf(BstuDestination.SCHEDULE) }
  var hallSearchPrefill by rememberSaveable { mutableStateOf("") }
  var scheduleHallPrefill by remember { mutableStateOf<LectureHall?>(null) }
  var showProfileDialog by remember { mutableStateOf(false) }
  var showShareDialog by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  var localToastMessage by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(feedback, localToastMessage) {
    val msg = feedback ?: localToastMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      if (feedback != null) viewModel.clearFeedback()
      localToastMessage = null
    }
  }

  val allHalls = (lectureHallsState as? UiState.Success)?.data ?: BstuCampusCatalog.builtInLectureHalls
  val profile = (userProfileState as? UiState.Success)?.data

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
          title = {
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
              )
              val subtitle = profile?.faculty?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.tagline_campus)
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
          actions = {
            IconButton(
              onClick = { showShareDialog = true },
              modifier = Modifier
                .size(48.dp)
                .testTag("share_app_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.cd_share_app),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = { showProfileDialog = true },
              modifier = Modifier
                .size(48.dp)
                .testTag("profile_button"),
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = stringResource(R.string.cd_profile_avatar),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          },
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar {
            BstuDestination.entries.forEach { dest ->
              NavigationBarItem(
                selected = currentTab == dest,
                onClick = { currentTab = dest },
                icon = { Icon(dest.icon, contentDescription = null) },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag("nav_${dest.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
          ) {
            Spacer(modifier = Modifier.size(12.dp))
            BstuDestination.entries.forEach { dest ->
              NavigationRailItem(
                selected = currentTab == dest,
                onClick = { currentTab = dest },
                icon = { Icon(dest.icon, contentDescription = null) },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag("rail_${dest.route}"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (currentTab) {
            BstuDestination.SCHEDULE -> {
              ScheduleScreen(
                schedulesState = schedulesState,
                lectureHalls = allHalls,
                prefillHallForNewClass = scheduleHallPrefill,
                onConsumePrefillHall = { scheduleHallPrefill = null },
                onSaveSchedule = viewModel::saveScheduleItem,
                onDeleteSchedule = viewModel::deleteScheduleItem,
                onFindHallOnCampus = { _, roomNumber ->
                  hallSearchPrefill = roomNumber
                  currentTab = BstuDestination.HALLS
                },
              )
            }
            BstuDestination.HALLS -> {
              CampusHallsScreen(
                hallsState = lectureHallsState,
                currentUserId = user.uid,
                initialSearchQuery = hallSearchPrefill,
                onConsumeInitialSearch = { hallSearchPrefill = "" },
                onAddHall = viewModel::addLectureHall,
                onDeleteHall = viewModel::deleteLectureHall,
                onAddHallToSchedule = { hall ->
                  scheduleHallPrefill = hall
                  currentTab = BstuDestination.SCHEDULE
                },
              )
            }
            BstuDestination.SPHERE -> {
              CommunityFeedScreen(
                postsState = postsState,
                commentsState = commentsState,
                currentUserId = user.uid,
                onCreatePost = viewModel::createPost,
                onDeletePost = viewModel::deletePost,
                onOpenComments = viewModel::openPostComments,
                onAddComment = viewModel::addComment,
              )
            }
            BstuDestination.GROUPS -> {
              GroupsScreen(
                groupsState = groupsState,
                messagesState = messagesState,
                currentUserId = user.uid,
                onCreateGroup = viewModel::createGroup,
                onJoinGroup = viewModel::joinGroup,
                onLeaveGroup = viewModel::leaveGroup,
                onDeleteGroup = viewModel::deleteGroup,
                onOpenGroupChat = viewModel::openGroupChat,
                onSendMessage = viewModel::sendGroupMessage,
              )
            }
          }
        }
      }
    }
  }

  if (showProfileDialog) {
    ProfileSettingsDialog(
      profile = profile ?: UserProfile(
        uid = user.uid,
        displayName = user.displayName ?: "BStU Student",
        email = user.email,
      ),
      onDismiss = { showProfileDialog = false },
      onSave = { name, faculty, academicGroup, year, bio ->
        viewModel.saveProfile(name, faculty, academicGroup, year, bio)
        showProfileDialog = false
      },
      onSignOut = {
        showProfileDialog = false
        onSignOut()
      },
      onOpenShare = {
        showProfileDialog = false
        showShareDialog = true
      },
    )
  }

  if (showShareDialog) {
    ShareAppDialog(
      onDismiss = { showShareDialog = false },
      onNotify = { msg -> localToastMessage = msg },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSettingsDialog(
  profile: UserProfile,
  onDismiss: () -> Unit,
  onSave: (name: String, faculty: String, academicGroup: String, year: Long, bio: String?) -> Unit,
  onSignOut: () -> Unit,
  onOpenShare: () -> Unit,
) {
  var displayName by remember { mutableStateOf(profile.displayName) }
  var faculty by remember { mutableStateOf(profile.faculty) }
  var academicGroup by remember { mutableStateOf(profile.academicGroup) }
  var studyYear by remember { mutableLongStateOf(profile.studyYear.coerceIn(1L, 6L)) }
  var bio by remember { mutableStateOf(profile.bio.orEmpty()) }
  var facultyExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.profile_title)) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 440.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!profile.email.isNullOrBlank()) {
          Text(
            text = profile.email,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text("Display name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenuBox(
          expanded = facultyExpanded,
          onExpandedChange = { facultyExpanded = !facultyExpanded },
        ) {
          OutlinedTextField(
            value = faculty,
            onValueChange = { faculty = it },
            label = { Text("BStU Faculty") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = facultyExpanded,
            onDismissRequest = { facultyExpanded = false },
          ) {
            BstuCampusCatalog.faculties.forEach { f ->
              DropdownMenuItem(
                text = { Text(f) },
                onClick = {
                  faculty = f
                  facultyExpanded = false
                },
              )
            }
          }
        }
        OutlinedTextField(
          value = academicGroup,
          onValueChange = { academicGroup = it },
          label = { Text("Academic Group (e.g. 642a)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Study Year", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          (1L..4L).forEach { yr ->
            FilterChip(
              selected = studyYear == yr,
              onClick = { studyYear = yr },
              label = { Text("Year $yr") },
            )
          }
        }
        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it },
          label = { Text("Bio (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider()
        OutlinedButton(
          onClick = onOpenShare,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("profile_share_app_button"),
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.action_share_app))
        }
        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("sign_out_button"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.action_sign_out))
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            displayName.trim(),
            faculty.trim(),
            academicGroup.trim(),
            studyYear,
            bio.trim().ifEmpty { null },
          )
        },
        enabled = displayName.isNotBlank(),
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
