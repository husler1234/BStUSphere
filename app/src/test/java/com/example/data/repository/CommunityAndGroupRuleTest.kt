package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CampusPost
import com.example.data.model.GroupMessage
import com.example.data.model.PostComment
import com.example.data.model.StudyGroup
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityAndGroupRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun postsAndComments_sharedCampusFlowAndBatchCommentIncrement() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = CommunityAndGroupRepository(firestore)

    val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val postRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(
        CampusPost(
          id = postId,
          authorId = aliceUid,
          authorName = "Alice",
          authorFaculty = "Applied Math",
          category = "material",
          subject = "Calculus II",
          title = "Midterm Formula Sheet",
          body = "Covers series convergence and improper integrals.",
          attachmentName = "calc2_formulas.pdf",
          attachmentMimeType = "application/pdf",
          attachmentSizeBytes = 128000L,
        )
      )
    }
    assertTrue(postRes.isSuccess)

    val bobUid = signInTestUser(BOB_EMAIL)
    val feed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeCampusPosts().first { list -> list.any { it.id == postId } }
    }
    assertTrue(feed.any { it.id == postId })

    val commentRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addComment(
        postId,
        PostComment(
          authorId = bobUid,
          authorName = "Bob",
          body = "Super helpful before Friday's quiz!",
        ),
      )
    }
    assertTrue(commentRes.isSuccess)

    val comments = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeComments(postId).first { it.isNotEmpty() }
    }
    assertTrue(comments.any { it.authorId == bobUid })
  }

  @Test
  fun studyGroups_publicJoinAndMemberMessaging_nonMemberWriteDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = CommunityAndGroupRepository(firestore)

    val groupId = "grp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val createdGroup = StudyGroup(
      id = groupId,
      name = "BStU AI & Algorithms",
      subject = "Computer Science",
      faculty = "Applied Mathematics & Cybernetics",
      description = "Lab prep and study notes.",
      meetingLocation = "Main Building • 405",
      visibility = "public",
      createdBy = aliceUid,
      creatorName = "Alice",
      memberIds = listOf(aliceUid),
      memberCount = 1L,
    )
    val createRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.createGroup(createdGroup) }
    assertTrue(createRes.isSuccess)

    val bobUid = signInTestUser(BOB_EMAIL)
    // Non-member Bob tries sending message before joining -> must fail
    val nonMemberSend = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.sendGroupMessage(
        createdGroup,
        GroupMessage(
          authorId = bobUid,
          authorName = "Bob",
          body = "Can I post without joining?",
        ),
      )
    }
    assertTrue(nonMemberSend.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      nonMemberSend.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    // Bob joins public group
    val joinRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.joinPublicGroup(createdGroup, bobUid) }
    assertTrue(joinRes.isSuccess)

    val updatedGroup = createdGroup.copy(
      memberIds = listOf(aliceUid, bobUid),
      memberCount = 2L,
    )
    val memberSend = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.sendGroupMessage(
        updatedGroup,
        GroupMessage(
          authorId = bobUid,
          authorName = "Bob",
          body = "Joined! Sharing the lab starter guide.",
        ),
      )
    }
    assertTrue(memberSend.isSuccess)

    val messages = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeGroupMessages(groupId, "public", bobUid).first { it.isNotEmpty() }
    }
    assertTrue(messages.any { it.authorId == bobUid })
  }

  private companion object {
    const val ALICE_EMAIL = "alice.comm@test.com"
    const val BOB_EMAIL = "bob.comm@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
