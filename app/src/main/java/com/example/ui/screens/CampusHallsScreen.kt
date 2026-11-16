package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BstuCampusCatalog
import com.example.data.model.LectureHall
import com.example.ui.viewmodel.UiState

@Composable
fun CampusHallsScreen(
  hallsState: UiState<List<LectureHall>>,
  currentUserId: String,
  initialSearchQuery: String,
  onConsumeInitialSearch: () -> Unit,
  onAddHall: (LectureHall) -> Unit,
  onDeleteHall: (String) -> Unit,
  onAddHallToSchedule: (LectureHall) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedBuilding by rememberSaveable { mutableStateOf("All") }
  var showAddDialog by remember { mutableStateOf(false) }

  LaunchedEffect(initialSearchQuery) {
    if (initialSearchQuery.isNotBlank()) {
      searchQuery = initialSearchQuery
      selectedBuilding = "All"
      onConsumeInitialSearch()
    }
  }

  val halls = (hallsState as? UiState.Success)?.data ?: BstuCampusCatalog.builtInLectureHalls

  val filteredHalls = remember(halls, searchQuery, selectedBuilding) {
    halls.filter { hall ->
      val matchesBuilding = selectedBuilding == "All" || hall.building.equals(selectedBuilding, ignoreCase = true)
      val q = searchQuery.trim()
      val matchesSearch = q.isEmpty() ||
        hall.name.contains(q, ignoreCase = true) ||
        hall.roomNumber.contains(q, ignoreCase = true) ||
        hall.building.contains(q, ignoreCase = true) ||
        (hall.faculty?.contains(q, ignoreCase = true) == true)
      matchesBuilding && matchesSearch
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_hall_fab"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add_hall))
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = innerPadding.calculateBottomPadding()),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "halls_banner") {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(RoundedCornerShape(20.dp)),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_lecture_hall),
            contentDescription = stringResource(R.string.cd_lecture_hall_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .align(Alignment.CenterStart)
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "BStU Lecture Hall Finder",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
              text = "Search by room code, faculty, or building wing",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            )
          }
        }
      }

      item(key = "halls_search") {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search room (e.g. 312, Act-200, Law, Physics)") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { searchQuery = "" },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_clear_search))
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("hall_search_input"),
        )
      }

      item(key = "halls_building_filters") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val buildings = listOf("All") + BstuCampusCatalog.buildings
          buildings.forEach { building ->
            FilterChip(
              selected = selectedBuilding == building,
              onClick = { selectedBuilding = building },
              label = { Text(building) },
              modifier = Modifier.heightIn(min = 48.dp),
            )
          }
        }
      }

      if (filteredHalls.isEmpty()) {
        item(key = "halls_empty") {
          EmptyStateCard(
            icon = Icons.Rounded.Apartment,
            title = "No matching lecture halls",
            body = "Can't find your classroom? Contribute it to the shared BStU campus directory for fellow students.",
            actionLabel = stringResource(R.string.action_add_hall),
            onAction = { showAddDialog = true },
          )
        }
      } else {
        items(filteredHalls, key = { it.id }) { hall ->
          LectureHallCard(
            hall = hall,
            canDelete = !hall.isBuiltInCatalog && hall.createdBy == currentUserId,
            onDelete = { onDeleteHall(hall.id) },
            onAddToSchedule = { onAddHallToSchedule(hall) },
          )
        }
      }
    }
  }

  if (showAddDialog) {
    AddLectureHallDialog(
      onDismiss = { showAddDialog = false },
      onSave = { hall ->
        onAddHall(hall)
        showAddDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LectureHallCard(
  hall: LectureHall,
  canDelete: Boolean,
  onDelete: () -> Unit,
  onAddToSchedule: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hall_card_${hall.id}"),
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
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
          ) {
            Text(
              text = hall.roomNumber,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = hall.name,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${hall.building} • ${hall.floor}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (canDelete) {
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

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        if (!hall.faculty.isNullOrBlank()) {
          Text(
            text = hall.faculty,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(
            imageVector = Icons.Rounded.Groups,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${hall.capacity} seats",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (!hall.directions.isNullOrBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = hall.directions,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (hall.amenities.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          hall.amenities.forEach { amenity ->
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = amenity,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      AssistChip(
        onClick = onAddToSchedule,
        label = { Text("Schedule a class here") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        },
        modifier = Modifier.heightIn(min = 48.dp),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLectureHallDialog(
  onDismiss: () -> Unit,
  onSave: (LectureHall) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var building by remember { mutableStateOf(BstuCampusCatalog.buildings.first()) }
  var floor by remember { mutableStateOf("2nd Floor") }
  var roomNumber by remember { mutableStateOf("") }
  var faculty by remember { mutableStateOf("") }
  var capacityText by remember { mutableStateOf("60") }
  var directions by remember { mutableStateOf("") }
  var buildingExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Campus Lecture Hall") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 440.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Hall / Room name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("new_hall_name_input"),
        )
        ExposedDropdownMenuBox(
          expanded = buildingExpanded,
          onExpandedChange = { buildingExpanded = !buildingExpanded },
        ) {
          OutlinedTextField(
            value = building,
            onValueChange = { building = it },
            label = { Text("Building *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingExpanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = buildingExpanded,
            onDismissRequest = { buildingExpanded = false },
          ) {
            BstuCampusCatalog.buildings.forEach { b ->
              DropdownMenuItem(
                text = { Text(b) },
                onClick = {
                  building = b
                  buildingExpanded = false
                },
              )
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = roomNumber,
            onValueChange = { roomNumber = it },
            label = { Text("Room # *") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("new_hall_room_input"),
          )
          OutlinedTextField(
            value = floor,
            onValueChange = { floor = it },
            label = { Text("Floor *") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = faculty,
            onValueChange = { faculty = it },
            label = { Text("Faculty") },
            singleLine = true,
            modifier = Modifier.weight(1.4f),
          )
          OutlinedTextField(
            value = capacityText,
            onValueChange = { capacityText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Seats") },
            singleLine = true,
            modifier = Modifier.weight(0.8f),
          )
        }
        OutlinedTextField(
          value = directions,
          onValueChange = { directions = it },
          label = { Text("How to get there") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            LectureHall(
              name = name.trim(),
              building = building.trim(),
              floor = floor.trim().ifEmpty { "1st Floor" },
              roomNumber = roomNumber.trim(),
              faculty = faculty.trim().ifEmpty { null },
              capacity = (capacityText.toLongOrNull() ?: 60L).coerceIn(1L, 1000L),
              directions = directions.trim().ifEmpty { null },
              amenities = listOf("Wi-Fi", "Projector"),
            )
          )
        },
        enabled = name.isNotBlank() && roomNumber.isNotBlank(),
        modifier = Modifier.testTag("save_hall_button"),
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
