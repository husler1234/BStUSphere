package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val uid: String = "",
  val displayName: String = "",
  val email: String? = null,
  val faculty: String = "Applied Mathematics & Cybernetics",
  val academicGroup: String = "",
  val studyYear: Long = 1L,
  val bio: String? = null,
  val photoUrl: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "uid" to uid,
      "displayName" to displayName.trim().take(80),
      "email" to email?.trim()?.take(150)?.takeIf { it.isNotEmpty() },
      "faculty" to faculty.trim().take(80),
      "academicGroup" to academicGroup.trim().take(40),
      "studyYear" to studyYear.coerceIn(1L, 6L),
      "bio" to bio?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
      "photoUrl" to photoUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>

  fun toUpdateMap(): Map<String, Any> =
    mapOf(
      "displayName" to displayName.trim().take(80),
      "email" to email?.trim()?.take(150)?.takeIf { it.isNotEmpty() },
      "faculty" to faculty.trim().take(80),
      "academicGroup" to academicGroup.trim().take(40),
      "studyYear" to studyYear.coerceIn(1L, 6L),
      "bio" to bio?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
      "photoUrl" to photoUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class ClassScheduleItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val courseName: String = "",
  val courseCode: String? = null,
  val instructor: String? = null,
  val building: String = "Main Building",
  val roomNumber: String = "",
  val hallId: String? = null,
  val dayOfWeek: Long = 1L,
  val startTime: String = "09:00",
  val endTime: String = "10:20",
  val classType: String = "Lecture",
  val notes: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "userId" to userId,
      "courseName" to courseName.trim().take(100),
      "courseCode" to courseCode?.trim()?.take(24)?.takeIf { it.isNotEmpty() },
      "instructor" to instructor?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
      "building" to building.trim().take(60),
      "roomNumber" to roomNumber.trim().take(30),
      "hallId" to hallId?.trim()?.take(128)?.takeIf { it.isNotEmpty() },
      "dayOfWeek" to dayOfWeek.coerceIn(1L, 7L),
      "startTime" to startTime.trim().take(10),
      "endTime" to endTime.trim().take(10),
      "classType" to if (classType in listOf("Lecture", "Seminar", "Lab", "Tutorial")) classType else "Lecture",
      "notes" to notes?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>

  fun toUpdateMap(): Map<String, Any> =
    mapOf(
      "courseName" to courseName.trim().take(100),
      "courseCode" to courseCode?.trim()?.take(24)?.takeIf { it.isNotEmpty() },
      "instructor" to instructor?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
      "building" to building.trim().take(60),
      "roomNumber" to roomNumber.trim().take(30),
      "hallId" to hallId?.trim()?.take(128)?.takeIf { it.isNotEmpty() },
      "dayOfWeek" to dayOfWeek.coerceIn(1L, 7L),
      "startTime" to startTime.trim().take(10),
      "endTime" to endTime.trim().take(10),
      "classType" to if (classType in listOf("Lecture", "Seminar", "Lab", "Tutorial")) classType else "Lecture",
      "notes" to notes?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class LectureHall(
  @DocumentId val id: String = "",
  val createdBy: String = "",
  val visibility: String = "campus",
  val name: String = "",
  val building: String = "",
  val floor: String = "",
  val roomNumber: String = "",
  val faculty: String? = null,
  val capacity: Long = 60L,
  val directions: String? = null,
  val amenities: List<String> = emptyList(),
  val isBuiltInCatalog: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "createdBy" to createdBy,
      "visibility" to "campus",
      "name" to name.trim().take(100),
      "building" to building.trim().take(60),
      "floor" to floor.trim().take(30),
      "roomNumber" to roomNumber.trim().take(30),
      "faculty" to faculty?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
      "capacity" to capacity.coerceIn(1L, 1000L),
      "directions" to directions?.trim()?.take(400)?.takeIf { it.isNotEmpty() },
      "amenities" to amenities.take(10),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class CampusPost(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorFaculty: String? = null,
  val visibility: String = "campus",
  val category: String = "thought",
  val subject: String? = null,
  val title: String = "",
  val body: String = "",
  val attachmentName: String? = null,
  val attachmentMimeType: String? = null,
  val attachmentSizeBytes: Long? = null,
  val attachmentTextPreview: String? = null,
  val attachmentImageBase64: String? = null,
  val resourceUrl: String? = null,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "authorId" to authorId,
      "authorName" to authorName.trim().take(80).ifEmpty { "BStU Student" },
      "authorFaculty" to authorFaculty?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
      "visibility" to "campus",
      "category" to if (category in listOf("thought", "material", "question", "announcement")) category else "thought",
      "subject" to subject?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
      "title" to title.trim().take(140),
      "body" to body.trim().take(2000),
      "attachmentName" to attachmentName?.trim()?.take(180)?.takeIf { it.isNotEmpty() },
      "attachmentMimeType" to attachmentMimeType?.trim()?.take(100)?.takeIf { it.isNotEmpty() },
      "attachmentSizeBytes" to attachmentSizeBytes?.coerceIn(0L, 104_857_600L),
      "attachmentTextPreview" to attachmentTextPreview?.take(4000)?.takeIf { it.isNotEmpty() },
      "attachmentImageBase64" to attachmentImageBase64?.take(180_000)?.takeIf { it.isNotEmpty() },
      "resourceUrl" to resourceUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
      "commentsCount" to 0L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class PostComment(
  @DocumentId val id: String = "",
  val postId: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorFaculty: String? = null,
  val visibility: String = "campus",
  val body: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "postId" to postId,
      "authorId" to authorId,
      "authorName" to authorName.trim().take(80).ifEmpty { "BStU Student" },
      "authorFaculty" to authorFaculty?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
      "visibility" to "campus",
      "body" to body.trim().take(1000),
      "createdAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class StudyGroup(
  @DocumentId val id: String = "",
  val name: String = "",
  val subject: String? = null,
  val faculty: String? = null,
  val description: String? = null,
  val meetingLocation: String? = null,
  val visibility: String = "public",
  val createdBy: String = "",
  val creatorName: String = "",
  val memberIds: List<String> = emptyList(),
  val memberCount: Long = 1L,
  val lastMessagePreview: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "name" to name.trim().take(80),
      "subject" to subject?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
      "faculty" to faculty?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
      "description" to description?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
      "meetingLocation" to meetingLocation?.trim()?.take(100)?.takeIf { it.isNotEmpty() },
      "visibility" to if (visibility == "private") "private" else "public",
      "createdBy" to createdBy,
      "creatorName" to creatorName.trim().take(80).ifEmpty { "BStU Student" },
      "memberIds" to listOf(createdBy),
      "memberCount" to 1L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class GroupMessage(
  @DocumentId val id: String = "",
  val groupId: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val body: String = "",
  val visibility: String = "public",
  val memberIds: List<String> = emptyList(),
  val attachmentName: String? = null,
  val attachmentMimeType: String? = null,
  val attachmentImageBase64: String? = null,
  val resourceUrl: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "groupId" to groupId,
      "authorId" to authorId,
      "authorName" to authorName.trim().take(80).ifEmpty { "BStU Student" },
      "body" to body.trim().take(1500),
      "visibility" to if (visibility == "private") "private" else "public",
      "memberIds" to memberIds.take(50),
      "attachmentName" to attachmentName?.trim()?.take(180)?.takeIf { it.isNotEmpty() },
      "attachmentMimeType" to attachmentMimeType?.trim()?.take(100)?.takeIf { it.isNotEmpty() },
      "attachmentImageBase64" to attachmentImageBase64?.take(180_000)?.takeIf { it.isNotEmpty() },
      "resourceUrl" to resourceUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
      "createdAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}
