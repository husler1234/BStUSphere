package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CampusPost
import com.example.data.model.GroupMessage
import com.example.data.model.PostComment
import com.example.data.model.StudyGroup
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CommunityAndGroupRepository(
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  // --- Campus Thoughts, Files & Study Materials ---

  fun observeCampusPosts(): Flow<List<CampusPost>> =
    db.collection("posts")
      .whereEqualTo("visibility", "campus")
      .snapshots()
      .map { querySnapshot ->
        querySnapshot
          .toObjects(CampusPost::class.java, ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }

  suspend fun createPost(post: CampusPost): Result<String> {
    val postId = post.id.takeIf { it.isNotBlank() } ?: "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("posts").document(postId)
    return try {
      docRef.set(post.toCreateMap()).await()
      Result.success(postId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deletePost(postId: String): Result<Unit> {
    val docRef = db.collection("posts").document(postId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeComments(postId: String): Flow<List<PostComment>> =
    db.collection("posts")
      .document(postId)
      .collection("comments")
      .whereEqualTo("visibility", "campus")
      .snapshots()
      .map { querySnapshot ->
        querySnapshot
          .toObjects(PostComment::class.java, ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt ?: Timestamp(0, 0) }
      }

  suspend fun addComment(postId: String, comment: PostComment): Result<String> {
    val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val postRef = db.collection("posts").document(postId)
    val commentRef = postRef.collection("comments").document(commentId)
    return try {
      val batch = db.batch()
      batch.set(commentRef, comment.copy(postId = postId).toCreateMap())
      batch.update(
        postRef,
        mapOf(
          "commentsCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(commentId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, commentRef.path)
      Result.failure(e)
    }
  }

  // --- Study Groups & Group Chat ---

  fun observeGroupsForUser(userId: String): Flow<List<StudyGroup>> {
    val publicFlow =
      db.collection("groups")
        .whereEqualTo("visibility", "public")
        .snapshots()
        .map { it.toObjects(StudyGroup::class.java, ServerTimestampBehavior.ESTIMATE) }
    val myFlow =
      db.collection("groups")
        .whereArrayContains("memberIds", userId)
        .snapshots()
        .map { it.toObjects(StudyGroup::class.java, ServerTimestampBehavior.ESTIMATE) }

    return combine(myFlow, publicFlow) { myGroups, publicGroups ->
      (myGroups + publicGroups)
        .distinctBy { it.id }
        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
    }
  }

  suspend fun createGroup(group: StudyGroup): Result<String> {
    val groupId = group.id.takeIf { it.isNotBlank() } ?: "grp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("groups").document(groupId)
    return try {
      docRef.set(group.toCreateMap()).await()
      Result.success(groupId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun joinPublicGroup(group: StudyGroup, userId: String): Result<Unit> {
    val docRef = db.collection("groups").document(group.id)
    val updatedMembers = (group.memberIds + userId).distinct()
    return try {
      docRef
        .update(
          mapOf(
            "memberIds" to updatedMembers,
            "memberCount" to updatedMembers.size.toLong(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun leaveGroup(group: StudyGroup, userId: String): Result<Unit> {
    val docRef = db.collection("groups").document(group.id)
    val updatedMembers = group.memberIds.filter { it != userId }
    return try {
      docRef
        .update(
          mapOf(
            "memberIds" to updatedMembers,
            "memberCount" to updatedMembers.size.toLong(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteGroup(groupId: String): Result<Unit> {
    val docRef = db.collection("groups").document(groupId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeGroupMessages(
    groupId: String,
    visibility: String,
    userId: String,
  ): Flow<List<GroupMessage>> {
    val base = db.collection("groups").document(groupId).collection("messages")
    val query =
      if (visibility == "public") {
        base.whereEqualTo("visibility", "public")
      } else {
        base.whereArrayContains("memberIds", userId)
      }
    return query.snapshots().map { querySnapshot ->
      querySnapshot
        .toObjects(GroupMessage::class.java, ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
    }
  }

  suspend fun sendGroupMessage(group: StudyGroup, message: GroupMessage): Result<String> {
    val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val groupRef = db.collection("groups").document(group.id)
    val msgRef = groupRef.collection("messages").document(messageId)
    val previewText =
      "${message.authorName}: ${message.body.ifBlank { message.attachmentName ?: "Shared a file" }}"
        .take(140)
    return try {
      val batch = db.batch()
      batch.set(
        msgRef,
        message
          .copy(
            groupId = group.id,
            visibility = group.visibility,
            memberIds = group.memberIds,
          )
          .toCreateMap(),
      )
      batch.update(
        groupRef,
        mapOf(
          "lastMessagePreview" to previewText,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(messageId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      Result.failure(e)
    }
  }
}
