package com.example.ui.viewmodel

import android.util.Log
import com.example.data.model.BstuCampusCatalog
import com.example.data.model.CampusPost
import com.example.data.model.ClassScheduleItem
import com.example.data.model.GroupMessage
import com.example.data.model.LectureHall
import com.example.data.model.PostComment
import com.example.data.model.StudyGroup
import com.example.data.model.UserProfile
import com.example.data.repository.CommunityAndGroupRepository
import com.example.data.repository.OperationType
import com.example.data.repository.ScheduleAndHallRepository
import com.example.data.repository.handleFirestoreError
import com.example.data.repository.unwrapFirestoreRootError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class BstuMainViewModel(
  private val scheduleRepo: ScheduleAndHallRepository,
  private val communityRepo: CommunityAndGroupRepository,
  val currentUserId: String,
  private val defaultDisplayName: String,
  private val defaultEmail: String?,
  private val defaultPhotoUrl: String?,
) : ViewModel() {

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _selectedPostId = MutableStateFlow<String?>(null)
  private val _selectedGroup = MutableStateFlow<StudyGroup?>(null)

  val userProfile: StateFlow<UiState<UserProfile>> =
    scheduleRepo.observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile>> { profile ->
        UiState.Success(
          profile ?: UserProfile(
            uid = currentUserId,
            displayName = defaultDisplayName.ifBlank { "BStU Student" },
            email = defaultEmail,
            faculty = "Applied Mathematics & Cybernetics",
            academicGroup = "",
            studyYear = 1L,
            photoUrl = defaultPhotoUrl,
          )
        )
      }
      .catch { error ->
        val root = unwrapFirestoreRootError(error)
        handleFirestoreError(root, OperationType.GET, "users/$currentUserId")
        emit(UiState.Error(root.localizedMessage ?: "Failed to load profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val schedules: StateFlow<UiState<List<ClassScheduleItem>>> =
    scheduleRepo.observeSchedules(currentUserId)
      .map<List<ClassScheduleItem>, UiState<List<ClassScheduleItem>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreRootError(error)
        handleFirestoreError(root, OperationType.LIST, "users/$currentUserId/schedules")
        emit(UiState.Error(root.localizedMessage ?: "Failed to observe schedule"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val lectureHalls: StateFlow<UiState<List<LectureHall>>> =
    scheduleRepo.observeCampusLectureHalls()
      .map<List<LectureHall>, UiState<List<LectureHall>>> { cloudHalls ->
        UiState.Success(cloudHalls + BstuCampusCatalog.builtInLectureHalls)
      }
      .catch { error ->
        val root = unwrapFirestoreRootError(error)
        handleFirestoreError(root, OperationType.LIST, "lectureHalls")
        Log.w(TAG, "Falling back to built-in BStU campus directory after error", root)
        emit(UiState.Success(BstuCampusCatalog.builtInLectureHalls))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Success(BstuCampusCatalog.builtInLectureHalls),
      )

  val posts: StateFlow<UiState<List<CampusPost>>> =
    communityRepo.observeCampusPosts()
      .map<List<CampusPost>, UiState<List<CampusPost>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreRootError(error)
        handleFirestoreError(root, OperationType.LIST, "posts")
        emit(UiState.Error(root.localizedMessage ?: "Failed to load campus feed"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val groups: StateFlow<UiState<List<StudyGroup>>> =
    communityRepo.observeGroupsForUser(currentUserId)
      .map<List<StudyGroup>, UiState<List<StudyGroup>>> { UiState.Success(it) }
      .catch { error ->
        val root = unwrapFirestoreRootError(error)
        handleFirestoreError(root, OperationType.LIST, "groups")
        emit(UiState.Error(root.localizedMessage ?: "Failed to load study groups"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val activePostComments: StateFlow<UiState<List<PostComment>>> =
    _selectedPostId
      .flatMapLatest { postId ->
        if (postId == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          communityRepo.observeComments(postId)
            .map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
            .catch { error ->
              val root = unwrapFirestoreRootError(error)
              handleFirestoreError(root, OperationType.LIST, "posts/$postId/comments")
              emit(UiState.Error(root.localizedMessage ?: "Failed to load replies"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Success(emptyList()),
      )

  val activeGroupMessages: StateFlow<UiState<List<GroupMessage>>> =
    _selectedGroup
      .flatMapLatest { group ->
        if (group == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          communityRepo.observeGroupMessages(group.id, group.visibility, currentUserId)
            .map<List<GroupMessage>, UiState<List<GroupMessage>>> { UiState.Success(it) }
            .catch { error ->
              val root = unwrapFirestoreRootError(error)
              handleFirestoreError(root, OperationType.LIST, "groups/${group.id}/messages")
              emit(UiState.Error(root.localizedMessage ?: "Failed to load messages"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Success(emptyList()),
      )

  fun openPostComments(postId: String?) {
    _selectedPostId.value = postId
  }

  fun openGroupChat(group: StudyGroup?) {
    _selectedGroup.value = group
  }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  private fun currentAuthorName(): String =
    (userProfile.value as? UiState.Success)?.data?.displayName?.takeIf { it.isNotBlank() }
      ?: defaultDisplayName.ifBlank { "BStU Student" }

  private fun currentFaculty(): String? =
    (userProfile.value as? UiState.Success)?.data?.faculty?.takeIf { it.isNotBlank() }

  fun saveProfile(
    displayName: String,
    faculty: String,
    academicGroup: String,
    studyYear: Long,
    bio: String?,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = scheduleRepo.saveUserProfile(
        UserProfile(
          uid = currentUserId,
          displayName = displayName.ifBlank { defaultDisplayName },
          email = defaultEmail,
          faculty = faculty,
          academicGroup = academicGroup,
          studyYear = studyYear,
          bio = bio,
          photoUrl = defaultPhotoUrl,
        )
      )
      _isBusy.value = false
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Student profile saved" },
          onFailure = { it.localizedMessage ?: "Could not save profile" },
        )
    }
  }

  fun saveScheduleItem(item: ClassScheduleItem) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = if (item.id.isBlank()) {
        scheduleRepo.createSchedule(currentUserId, item).map { Unit }
      } else {
        scheduleRepo.updateSchedule(currentUserId, item)
      }
      _isBusy.value = false
      _feedbackMessage.value =
        res.fold(
          onSuccess = { if (item.id.isBlank()) "Class added to schedule" else "Class updated" },
          onFailure = { it.localizedMessage ?: "Could not save class" },
        )
    }
  }

  fun deleteScheduleItem(scheduleId: String) {
    viewModelScope.launch {
      val res = scheduleRepo.deleteSchedule(currentUserId, scheduleId)
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Class removed" },
          onFailure = { it.localizedMessage ?: "Could not remove class" },
        )
    }
  }

  fun addLectureHall(hall: LectureHall) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = scheduleRepo.addLectureHall(currentUserId, hall)
      _isBusy.value = false
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Lecture hall added to BStU directory" },
          onFailure = { it.localizedMessage ?: "Could not add lecture hall" },
        )
    }
  }

  fun deleteLectureHall(hallId: String) {
    viewModelScope.launch {
      val res = scheduleRepo.deleteLectureHall(hallId)
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Lecture hall removed" },
          onFailure = { it.localizedMessage ?: "Could not delete lecture hall" },
        )
    }
  }

  fun createPost(
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
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = communityRepo.createPost(
        CampusPost(
          authorId = currentUserId,
          authorName = currentAuthorName(),
          authorFaculty = currentFaculty(),
          category = category,
          subject = subject,
          title = title,
          body = body,
          attachmentName = attachmentName,
          attachmentMimeType = attachmentMimeType,
          attachmentSizeBytes = attachmentSizeBytes,
          attachmentTextPreview = attachmentTextPreview,
          attachmentImageBase64 = attachmentImageBase64,
          resourceUrl = resourceUrl,
        )
      )
      _isBusy.value = false
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Shared to BStUsphere" },
          onFailure = { it.localizedMessage ?: "Could not publish post" },
        )
    }
  }

  fun deletePost(postId: String) {
    viewModelScope.launch {
      val res = communityRepo.deletePost(postId)
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Post deleted" },
          onFailure = { it.localizedMessage ?: "Could not delete post" },
        )
    }
  }

  fun addComment(postId: String, body: String) {
    if (body.isBlank()) return
    viewModelScope.launch {
      val res = communityRepo.addComment(
        postId,
        PostComment(
          postId = postId,
          authorId = currentUserId,
          authorName = currentAuthorName(),
          authorFaculty = currentFaculty(),
          body = body,
        ),
      )
      res.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Could not post reply"
      }
    }
  }

  fun createGroup(
    name: String,
    subject: String?,
    faculty: String?,
    description: String?,
    meetingLocation: String?,
    visibility: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = communityRepo.createGroup(
        StudyGroup(
          name = name,
          subject = subject,
          faculty = faculty ?: currentFaculty(),
          description = description,
          meetingLocation = meetingLocation,
          visibility = visibility,
          createdBy = currentUserId,
          creatorName = currentAuthorName(),
        )
      )
      _isBusy.value = false
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Study group created" },
          onFailure = { it.localizedMessage ?: "Could not create group" },
        )
    }
  }

  fun joinGroup(group: StudyGroup) {
    viewModelScope.launch {
      val res = communityRepo.joinPublicGroup(group, currentUserId)
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Joined ${group.name}" },
          onFailure = { it.localizedMessage ?: "Could not join group" },
        )
    }
  }

  fun leaveGroup(group: StudyGroup) {
    viewModelScope.launch {
      val res = communityRepo.leaveGroup(group, currentUserId)
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Left ${group.name}" },
          onFailure = { it.localizedMessage ?: "Could not leave group" },
        )
    }
  }

  fun deleteGroup(groupId: String) {
    viewModelScope.launch {
      val res = communityRepo.deleteGroup(groupId)
      _feedbackMessage.value =
        res.fold(
          onSuccess = { "Study group deleted" },
          onFailure = { it.localizedMessage ?: "Could not delete group" },
        )
    }
  }

  fun sendGroupMessage(
    group: StudyGroup,
    body: String,
    attachmentName: String? = null,
    attachmentMimeType: String? = null,
    attachmentImageBase64: String? = null,
    resourceUrl: String? = null,
  ) {
    if (body.isBlank() && attachmentName == null && resourceUrl.isNullOrBlank()) return
    viewModelScope.launch {
      val res = communityRepo.sendGroupMessage(
        group,
        GroupMessage(
          groupId = group.id,
          authorId = currentUserId,
          authorName = currentAuthorName(),
          body = body.ifBlank { "Shared ${attachmentName ?: "a resource"}" },
          attachmentName = attachmentName,
          attachmentMimeType = attachmentMimeType,
          attachmentImageBase64 = attachmentImageBase64,
          resourceUrl = resourceUrl,
        ),
      )
      res.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Could not send message"
      }
    }
  }

  private companion object {
    const val TAG = "BstuMainVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
