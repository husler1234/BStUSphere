package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BstuCampusCatalog
import com.example.data.model.ClassScheduleItem
import com.example.data.model.LectureHall
import com.example.ui.viewmodel.UiState
import java.util.Calendar

private val dayLabels = listOf(
  0L to "All",
  1L to "Mon",
  2L to "Tue",
  3L to "Wed",
  4L to "Thu",
  5L to "Fri",
  6L to "Sat",
)

private val classTypes = listOf("Lecture", "Seminar", "Lab", "Tutorial")

@Composable
fun ScheduleScreen(
  schedulesState: UiState<List<ClassScheduleItem>>,
  lectureHalls: List<LectureHall>,
  prefillHallForNewClass: LectureHall?,
  onConsumePrefillHall: () -> Unit,
  onSaveSchedule: (ClassScheduleItem) -> Unit,
  onDeleteSchedule: (String) -> Unit,
  onFindHallOnCampus: (building: String, roomNumber: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val todayDayOfWeek = remember {
    when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
      Calendar.MONDAY -> 1L
      Calendar.TUESDAY -> 2L
      Calendar.WEDNESDAY -> 3L
      Calendar.THURSDAY -> 4L
      Calendar.FRIDAY -> 5L
      Calendar.SATURDAY -> 6L
      else -> 1L
    }
  }

  var selectedDay by rememberSaveable { mutableLongStateOf(0L) }
  var editingItem by remember { mutableStateOf<ClassScheduleItem?>(null) }
  var showDialog by remember { mutableStateOf(false) }

  if (prefillHallForNewClass != null) {
    editingItem = ClassScheduleItem(
      building = prefillHallForNewClass.building,
      roomNumber = prefillHallForNewClass.roomNumber,
      hallId = prefillHallForNewClass.id,
      dayOfWeek = if (selectedDay in 1L..6L) selectedDay else todayDayOfWeek,
    )
    showDialog = true
    onConsumePrefillHall()
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingItem = ClassScheduleItem(
            dayOfWeek = if (selectedDay in 1L..6L) selectedDay else todayDayOfWeek
          )
          showDialog = true
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_schedule_fab"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add_class))
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = innerPadding.calculateBottomPadding()),
    ) {
      // Day filter strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        dayLabels.forEach { (dayNum, label) ->
          val chipLabel = if (dayNum == todayDayOfWeek) "$label • Today" else label
          FilterChip(
            selected = selectedDay == dayNum,
            onClick = { selectedDay = dayNum },
            label = { Text(chipLabel) },
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("day_filter_$dayNum"),
          )
        }
      }

      when (schedulesState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          ErrorCard(message = schedulesState.message)
        }
        is UiState.Success -> {
          val allItems = schedulesState.data
          val filtered = remember(allItems, selectedDay) {
            if (selectedDay == 0L) allItems else allItems.filter { it.dayOfWeek == selectedDay }
          }

          if (filtered.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Rounded.CalendarMonth,
              title = stringResource(R.string.empty_schedule_title),
              body = stringResource(R.string.empty_schedule_body),
              actionLabel = stringResource(R.string.action_add_class),
              onAction = {
                editingItem = ClassScheduleItem(
                  dayOfWeek = if (selectedDay in 1L..6L) selectedDay else todayDayOfWeek
                )
                showDialog = true
              },
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp, top = 4.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              val todayCount = allItems.count { it.dayOfWeek == todayDayOfWeek }
              item(key = "schedule_summary") {
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(18.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = if (todayCount > 0) "$todayCount classes today" else "Weekly Timetable",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                      )
                      Text(
                        text = "Tap any room badge to locate the lecture hall on campus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                      )
                    }
                    Surface(
                      color = MaterialTheme.colorScheme.primary,
                      shape = RoundedCornerShape(12.dp),
                    ) {
                      Text(
                        text = "${allItems.size} total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      )
                    }
                  }
                }
              }

              items(filtered, key = { it.id }) { item ->
                ScheduleItemCard(
                  item = item,
                  onFindHall = { onFindHallOnCampus(item.building, item.roomNumber) },
                  onEdit = {
                    editingItem = item
                    showDialog = true
                  },
                  onDelete = { onDeleteSchedule(item.id) },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showDialog && editingItem != null) {
    ScheduleEditorDialog(
      initial = editingItem!!,
      lectureHalls = lectureHalls,
      onDismiss = { showDialog = false },
      onSave = { saved ->
        onSaveSchedule(saved)
        showDialog = false
      },
    )
  }
}

@Composable
private fun ScheduleItemCard(
  item: ClassScheduleItem,
  onFindHall: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val dayName = dayLabels.firstOrNull { it.first == item.dayOfWeek }?.second ?: "Day ${item.dayOfWeek}"
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("schedule_card_${item.id}"),
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
        ) {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "$dayName • ${item.startTime}–${item.endTime}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = item.classType,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            )
          }
        }

        Row {
          IconButton(
            onClick = onEdit,
            modifier = Modifier.size(48.dp).testTag("edit_schedule_${item.id}"),
          ) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit class")
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(48.dp).testTag("delete_schedule_${item.id}"),
          ) {
            Icon(
              Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.cd_delete_item),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = item.courseName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (!item.instructor.isNullOrBlank() || !item.courseCode.isNullOrBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Person,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val meta = listOfNotNull(item.courseCode, item.instructor).joinToString(" • ")
            Text(
              text = meta,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (!item.notes.isNullOrBlank()) {
        Text(
          text = item.notes,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      AssistChip(
        onClick = onFindHall,
        label = {
          Text("${item.building} • Room ${item.roomNumber} (Locate)")
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        },
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("schedule_hall_chip_${item.id}"),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditorDialog(
  initial: ClassScheduleItem,
  lectureHalls: List<LectureHall>,
  onDismiss: () -> Unit,
  onSave: (ClassScheduleItem) -> Unit,
) {
  var courseName by remember { mutableStateOf(initial.courseName) }
  var courseCode by remember { mutableStateOf(initial.courseCode.orEmpty()) }
  var instructor by remember { mutableStateOf(initial.instructor.orEmpty()) }
  var building by remember { mutableStateOf(initial.building) }
  var roomNumber by remember { mutableStateOf(initial.roomNumber) }
  var hallId by remember { mutableStateOf(initial.hallId) }
  var dayOfWeek by remember { mutableLongStateOf(initial.dayOfWeek) }
  var startTime by remember { mutableStateOf(initial.startTime) }
  var endTime by remember { mutableStateOf(initial.endTime) }
  var classType by remember { mutableStateOf(initial.classType) }
  var notes by remember { mutableStateOf(initial.notes.orEmpty()) }

  var buildingMenuExpanded by remember { mutableStateOf(false) }
  var hallMenuExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initial.id.isBlank()) "Add BStU Class" else "Edit Class")
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 460.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = courseName,
          onValueChange = { courseName = it },
          label = { Text("Course name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("schedule_course_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = courseCode,
            onValueChange = { courseCode = it },
            label = { Text("Code") },
            singleLine = true,
            modifier = Modifier.weight(0.9f),
          )
          OutlinedTextField(
            value = instructor,
            onValueChange = { instructor = it },
            label = { Text("Lecturer") },
            singleLine = true,
            modifier = Modifier.weight(1.3f),
          )
        }

        // Quick pick from BStU lecture halls
        ExposedDropdownMenuBox(
          expanded = hallMenuExpanded,
          onExpandedChange = { hallMenuExpanded = !hallMenuExpanded },
        ) {
          OutlinedTextField(
            value = "Quick-pick BStU hall (optional)",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hallMenuExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = hallMenuExpanded,
            onDismissRequest = { hallMenuExpanded = false },
          ) {
            lectureHalls.take(15).forEach { hall ->
              DropdownMenuItem(
                text = { Text("${hall.building} • ${hall.roomNumber} (${hall.name})") },
                onClick = {
                  building = hall.building
                  roomNumber = hall.roomNumber
                  hallId = hall.id
                  hallMenuExpanded = false
                },
              )
            }
          }
        }

        ExposedDropdownMenuBox(
          expanded = buildingMenuExpanded,
          onExpandedChange = { buildingMenuExpanded = !buildingMenuExpanded },
        ) {
          OutlinedTextField(
            value = building,
            onValueChange = { building = it },
            label = { Text("Building *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingMenuExpanded) },
            singleLine = true,
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = buildingMenuExpanded,
            onDismissRequest = { buildingMenuExpanded = false },
          ) {
            BstuCampusCatalog.buildings.forEach { b ->
              DropdownMenuItem(
                text = { Text(b) },
                onClick = {
                  building = b
                  buildingMenuExpanded = false
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = roomNumber,
          onValueChange = { roomNumber = it },
          label = { Text("Lecture hall / Room number *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("schedule_room_input"),
        )

        Text("Day of week", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          dayLabels.filter { it.first > 0L }.forEach { (dayNum, label) ->
            FilterChip(
              selected = dayOfWeek == dayNum,
              onClick = { dayOfWeek = dayNum },
              label = { Text(label) },
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Start (HH:mm)") },
            leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("End (HH:mm)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Text("Format", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          classTypes.forEach { type ->
            FilterChip(
              selected = classType == type,
              onClick = { classType = type },
              label = { Text(type) },
            )
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            initial.copy(
              courseName = courseName.trim(),
              courseCode = courseCode.trim().ifEmpty { null },
              instructor = instructor.trim().ifEmpty { null },
              building = building.trim().ifEmpty { "Main Building" },
              roomNumber = roomNumber.trim(),
              hallId = hallId,
              dayOfWeek = dayOfWeek,
              startTime = startTime.trim().ifEmpty { "09:00" },
              endTime = endTime.trim().ifEmpty { "10:20" },
              classType = classType,
              notes = notes.trim().ifEmpty { null },
            )
          )
        },
        enabled = courseName.isNotBlank() && roomNumber.isNotBlank(),
        modifier = Modifier.testTag("save_schedule_button"),
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

@Composable
fun ErrorCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.errorContainer,
      shape = RoundedCornerShape(18.dp),
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.padding(18.dp),
      )
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  body: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.size(56.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(28.dp),
            )
          }
        }
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = body,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onAction,
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}
