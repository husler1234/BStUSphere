package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ClassScheduleItem
import com.example.data.model.LectureHall
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ScheduleAndHallRepository(
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    db.collection("users")
      .document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }

  suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
    val docRef = db.collection("users").document(profile.uid)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(profile.toUpdateMap()).await()
      } else {
        docRef.set(profile.toCreateMap()).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeSchedules(userId: String): Flow<List<ClassScheduleItem>> =
    db.collection("users")
      .document(userId)
      .collection("schedules")
      .snapshots()
      .map { querySnapshot ->
        querySnapshot
          .toObjects(ClassScheduleItem::class.java, ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<ClassScheduleItem> { it.dayOfWeek }.thenBy { it.startTime })
      }

  suspend fun createSchedule(userId: String, item: ClassScheduleItem): Result<String> {
    val scheduleId = item.id.takeIf { it.isNotBlank() } ?: "sch_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("users").document(userId).collection("schedules").document(scheduleId)
    return try {
      docRef.set(item.copy(userId = userId).toCreateMap()).await()
      Result.success(scheduleId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateSchedule(userId: String, item: ClassScheduleItem): Result<Unit> {
    require(item.id.isNotBlank()) { "Schedule ID must not be blank for update" }
    val docRef = db.collection("users").document(userId).collection("schedules").document(item.id)
    return try {
      docRef.update(item.toUpdateMap()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteSchedule(userId: String, scheduleId: String): Result<Unit> {
    val docRef = db.collection("users").document(userId).collection("schedules").document(scheduleId)
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

  fun observeCampusLectureHalls(): Flow<List<LectureHall>> =
    db.collection("lectureHalls")
      .whereEqualTo("visibility", "campus")
      .snapshots()
      .map { querySnapshot ->
        querySnapshot
          .toObjects(LectureHall::class.java, ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }

  suspend fun addLectureHall(userId: String, hall: LectureHall): Result<String> {
    val hallId = hall.id.takeIf { it.isNotBlank() } ?: "hall_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("lectureHalls").document(hallId)
    return try {
      docRef.set(hall.copy(createdBy = userId, visibility = "campus").toCreateMap()).await()
      Result.success(hallId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteLectureHall(hallId: String): Result<Unit> {
    val docRef = db.collection("lectureHalls").document(hallId)
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
}
