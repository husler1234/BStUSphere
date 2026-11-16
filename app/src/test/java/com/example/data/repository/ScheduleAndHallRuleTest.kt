package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ClassScheduleItem
import com.example.data.model.LectureHall
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleAndHallRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun userProfile_ownerCanCreateAndUpdate_crossUserReadDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = ScheduleAndHallRepository(firestore)

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveUserProfile(
        UserProfile(
          uid = aliceUid,
          displayName = "Alice BStU",
          email = ALICE_EMAIL,
          faculty = "Applied Mathematics & Cybernetics",
          academicGroup = "642a",
          studyYear = 3L,
        )
      )
    }
    assertTrue(saveResult.isSuccess)

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserProfile(aliceUid).first { it != null }
    }
    assertEquals("Alice BStU", profile?.displayName)

    signInTestUser(BOB_EMAIL)
    var deniedCode: FirebaseFirestoreException.Code? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).get().await()
      }
    } catch (e: Exception) {
      deniedCode = e.unwrapFirestoreException()?.code
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, deniedCode)
  }

  @Test
  fun schedules_ownerCrudAndRealtimeStream_crossUserDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = ScheduleAndHallRepository(firestore)

    val scheduleId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createSchedule(
        aliceUid,
        ClassScheduleItem(
          courseName = "Linear Algebra",
          courseCode = "MATH-201",
          instructor = "Prof. Aliyev",
          building = "Main Building",
          roomNumber = "312",
          dayOfWeek = 1L,
          startTime = "09:00",
          endTime = "10:20",
          classType = "Lecture",
        ),
      ).getOrThrow()
    }
    assertTrue(scheduleId.isNotBlank())

    val list = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSchedules(aliceUid).first { items -> items.any { it.id == scheduleId } }
    }
    assertTrue(list.any { it.id == scheduleId && it.courseName == "Linear Algebra" })

    signInTestUser(BOB_EMAIL)
    var unauthOrCrossErr: FirebaseFirestoreException? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeSchedules(aliceUid).first()
      }
    } catch (e: Exception) {
      unauthOrCrossErr = e.unwrapFirestoreException()
    }
    assertNotNull(unauthOrCrossErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unauthOrCrossErr?.code)
  }

  @Test
  fun lectureHalls_unauthenticatedDenied_authenticatedCreateAndObserveSucceeds() = runBlocking {
    auth.signOut()
    val repo = ScheduleAndHallRepository(firestore)

    var unauthErr: FirebaseFirestoreException? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCampusLectureHalls().first()
      }
    } catch (e: Exception) {
      unauthErr = e.unwrapFirestoreException()
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unauthErr?.code)

    val aliceUid = signInTestUser(ALICE_EMAIL)
    val hallId = "hall_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val addRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addLectureHall(
        aliceUid,
        LectureHall(
          id = hallId,
          name = "Quantum Optics Lab",
          building = "Main Building",
          floor = "4th Floor",
          roomNumber = "418",
          faculty = "Physics",
          capacity = 45L,
          directions = "East elevator to 4th floor.",
          amenities = listOf("Projector", "Wi-Fi"),
        ),
      )
    }
    assertTrue(addRes.isSuccess)

    signInTestUser(BOB_EMAIL)
    val halls = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeCampusLectureHalls().first { it.any { h -> h.id == hallId } }
    }
    assertTrue(halls.any { it.id == hallId })
  }

  private companion object {
    const val ALICE_EMAIL = "alice.bstu@test.com"
    const val BOB_EMAIL = "bob.bstu@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
