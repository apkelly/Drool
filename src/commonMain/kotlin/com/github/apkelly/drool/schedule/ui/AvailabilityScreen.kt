package com.github.apkelly.drool.schedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.apkelly.drool.resources.Res
import com.github.apkelly.drool.resources.action_back
import com.github.apkelly.drool.resources.action_cancel
import com.github.apkelly.drool.resources.action_confirm
import com.github.apkelly.drool.resources.action_retry
import com.github.apkelly.drool.resources.availability_add
import com.github.apkelly.drool.resources.availability_club
import com.github.apkelly.drool.resources.availability_delete
import com.github.apkelly.drool.resources.availability_delete_body
import com.github.apkelly.drool.resources.availability_delete_title
import com.github.apkelly.drool.resources.availability_description
import com.github.apkelly.drool.resources.availability_does_not_repeat
import com.github.apkelly.drool.resources.availability_empty
import com.github.apkelly.drool.resources.availability_end_time
import com.github.apkelly.drool.resources.availability_fortnightly
import com.github.apkelly.drool.resources.availability_load_failed
import com.github.apkelly.drool.resources.availability_monthly
import com.github.apkelly.drool.resources.availability_no_clubs
import com.github.apkelly.drool.resources.availability_repeat_end_date
import com.github.apkelly.drool.resources.availability_repeats
import com.github.apkelly.drool.resources.availability_save
import com.github.apkelly.drool.resources.availability_save_failed
import com.github.apkelly.drool.resources.availability_start_date
import com.github.apkelly.drool.resources.availability_start_time
import com.github.apkelly.drool.resources.availability_title
import com.github.apkelly.drool.resources.availability_weekly
import com.github.apkelly.drool.schedule.domain.model.AvailabilityRecurrence
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailability
import com.github.apkelly.drool.schedule.domain.model.RefereeAvailabilityDraft
import com.github.apkelly.drool.schedule.domain.model.RefereeClub
import com.github.apkelly.drool.schedule.ui.model.AvailabilityUiModel
import com.github.apkelly.drool.schedule.ui.model.AvailabilityUiState
import com.github.apkelly.drool.ui.format.formatFixtureDate
import com.github.apkelly.drool.ui.format.formatFixtureTime
import com.github.apkelly.drool.ui.format.platformUses24HourClock
import com.github.apkelly.drool.ui.theme.DroolTheme
import com.github.apkelly.drool.ui.widgets.MaterialBackIcon
import com.github.apkelly.drool.ui.widgets.MaterialSymbol
import com.github.apkelly.drool.ui.widgets.MaterialSymbolIcon
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
private const val DAYS_PER_WEEK = 7
private const val HOURS_PER_HALF_DAY = 12
private const val MAX_AVAILABILITY_MONTHS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(
    model: AvailabilityUiModel,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    onCreate: (RefereeAvailabilityDraft) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClearFailure: () -> Unit,
) {
    val currentOnLoad by rememberUpdatedState(onLoad)
    val currentOnClearFailure by rememberUpdatedState(onClearFailure)
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember(model.profileId) { mutableStateOf(false) }
    var pendingDelete by remember(model.profileId) {
        mutableStateOf<RefereeAvailability?>(null)
    }
    val ready = model.state as? AvailabilityUiState.Ready
    val failureMessage = stringResource(Res.string.availability_save_failed)
    LaunchedEffect(model.profileId) {
        currentOnLoad()
    }
    LaunchedEffect(ready?.failure) {
        if (ready?.failure != null) {
            snackbarHostState.showSnackbar(failureMessage)
            currentOnClearFailure()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.availability_title, model.profileName))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialBackIcon(stringResource(Res.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (ready != null && ready.clubs.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                ) {
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.Add,
                        contentDescription = stringResource(Res.string.availability_add),
                    )
                }
            }
        },
    ) { contentPadding ->
        when (val state = model.state) {
            AvailabilityUiState.Idle,
            AvailabilityUiState.Loading,
            -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is AvailabilityUiState.Failed -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(Res.string.availability_load_failed),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(stringResource(Res.string.action_retry))
                }
            }

            is AvailabilityUiState.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 12.dp,
                    end = 20.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        stringResource(Res.string.availability_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.clubs.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.availability_no_clubs),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else if (state.availabilities.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.availability_empty),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    items(state.availabilities, key = RefereeAvailability::id) { availability ->
                        AvailabilityCard(
                            availability = availability,
                            deleting = state.deletingAvailabilityId == availability.id,
                            onDelete = { pendingDelete = availability },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog && ready != null) {
        AddAvailabilityDialog(
            clubs = ready.clubs,
            saving = ready.isSaving,
            onDismiss = { showAddDialog = false },
            onSave = {
                showAddDialog = false
                onCreate(it)
            },
        )
    }

    pendingDelete?.let { availability ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.availability_delete_title)) },
            text = { Text(stringResource(Res.string.availability_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(availability.id)
                    },
                ) {
                    Text(stringResource(Res.string.availability_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun AvailabilityCard(
    availability: RefereeAvailability,
    deleting: Boolean,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    formatFixtureDate(availability.startEpochMillis),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${formatFixtureTime(availability.startEpochMillis)}–" +
                        formatFixtureTime(availability.endEpochMillis),
                    style = MaterialTheme.typography.bodyLarge,
                )
                availability.clubName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (availability.recurrence != AvailabilityRecurrence.SingleDay) {
                    Text(
                        recurrenceLabel(availability.recurrence),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (deleting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = onDelete) {
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.Delete,
                        contentDescription = stringResource(Res.string.availability_delete),
                    )
                }
            }
        }
    }
}

private enum class DateTarget {
    Start,
    End,
}

private enum class TimeTarget {
    Start,
    End,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAvailabilityDialog(
    clubs: List<RefereeClub>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (RefereeAvailabilityDraft) -> Unit,
) {
    val dateLimits = remember { currentDatePickerLimits() }
    val today = dateLimits.minimumUtcMillis
    var selectedClub by remember(clubs) { mutableStateOf(clubs.firstOrNull()) }
    var clubMenuExpanded by remember { mutableStateOf(false) }
    var recurrenceMenuExpanded by remember { mutableStateOf(false) }
    var startDateMillis by remember { mutableStateOf(today) }
    var endDateMillis by remember { mutableStateOf(today + DAYS_PER_WEEK * DAY_MILLIS) }
    var startHour by remember { mutableIntStateOf(18) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(19) }
    var endMinute by remember { mutableIntStateOf(0) }
    var recurrence by remember {
        mutableStateOf(AvailabilityRecurrence.SingleDay)
    }
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    val uses24HourClock = platformUses24HourClock()
    val validTimes = endHour * 60 + endMinute > startHour * 60 + startMinute
    val validDates =
        startDateMillis >= today &&
            startDateMillis <= dateLimits.maximumUtcMillis &&
            endDateMillis <= dateLimits.maximumUtcMillis &&
            (
                recurrence == AvailabilityRecurrence.SingleDay ||
                    endDateMillis >= startDateMillis
                )
    val canSave = selectedClub != null && validTimes && validDates && !saving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.availability_add)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(Res.string.availability_club),
                    style = MaterialTheme.typography.labelLarge,
                )
                Box {
                    OutlinedButton(
                        onClick = { clubMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(selectedClub?.name.orEmpty())
                    }
                    DropdownMenu(
                        expanded = clubMenuExpanded,
                        onDismissRequest = { clubMenuExpanded = false },
                    ) {
                        clubs.forEach { club ->
                            DropdownMenuItem(
                                text = { Text(club.name) },
                                onClick = {
                                    selectedClub = club
                                    clubMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                PickerButton(
                    label = stringResource(Res.string.availability_start_date),
                    value = formatFixtureDate(startDateMillis),
                    onClick = { dateTarget = DateTarget.Start },
                )
                PickerButton(
                    label = stringResource(Res.string.availability_start_time),
                    value = clockTime(startHour, startMinute, uses24HourClock),
                    onClick = { timeTarget = TimeTarget.Start },
                )
                PickerButton(
                    label = stringResource(Res.string.availability_end_time),
                    value = clockTime(endHour, endMinute, uses24HourClock),
                    onClick = { timeTarget = TimeTarget.End },
                )
                Text(
                    stringResource(Res.string.availability_repeats),
                    style = MaterialTheme.typography.labelLarge,
                )
                Box {
                    OutlinedButton(
                        onClick = { recurrenceMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(recurrenceLabel(recurrence))
                    }
                    DropdownMenu(
                        expanded = recurrenceMenuExpanded,
                        onDismissRequest = { recurrenceMenuExpanded = false },
                    ) {
                        AvailabilityRecurrence.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(recurrenceLabel(option)) },
                                onClick = {
                                    recurrence = option
                                    recurrenceMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                if (recurrence != AvailabilityRecurrence.SingleDay) {
                    PickerButton(
                        label = stringResource(Res.string.availability_repeat_end_date),
                        value = formatFixtureDate(endDateMillis),
                        onClick = { dateTarget = DateTarget.End },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    selectedClub?.let { club ->
                        onSave(
                            RefereeAvailabilityDraft(
                                startDateEpochMillis = startDateMillis,
                                endDateEpochMillis = endDateMillis.takeIf {
                                    recurrence != AvailabilityRecurrence.SingleDay
                                },
                                startHour = startHour,
                                startMinute = startMinute,
                                endHour = endHour,
                                endMinute = endMinute,
                                recurrence = recurrence,
                                clubId = club.id,
                                timeZone = club.timeZone,
                            ),
                        )
                    }
                },
            ) {
                Text(stringResource(Res.string.availability_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )

    dateTarget?.let { target ->
        key(target) {
            val initial = if (target == DateTarget.Start) startDateMillis else endDateMillis
            val minimumDate = if (target == DateTarget.Start) today else startDateMillis
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = initial,
                selectableDates = remember(minimumDate) {
                    DatesBetween(
                        minimumUtcMillis = minimumDate,
                        maximumUtcMillis = dateLimits.maximumUtcMillis,
                    )
                },
            )
            DatePickerDialog(
                onDismissRequest = { dateTarget = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dateState.selectedDateMillis?.let { selected ->
                                if (target == DateTarget.Start) {
                                    startDateMillis = selected
                                    if (endDateMillis < selected) {
                                        endDateMillis = selected + DAYS_PER_WEEK * DAY_MILLIS
                                    }
                                } else {
                                    endDateMillis = selected
                                }
                            }
                            dateTarget = null
                        },
                    ) {
                        Text(stringResource(Res.string.action_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dateTarget = null }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                },
            ) {
                DatePicker(state = dateState)
            }
        }
    }

    timeTarget?.let { target ->
        key(target) {
            val timeState = rememberTimePickerState(
                initialHour = if (target == TimeTarget.Start) startHour else endHour,
                initialMinute = if (target == TimeTarget.Start) startMinute else endMinute,
                is24Hour = uses24HourClock,
            )
            AlertDialog(
                onDismissRequest = { timeTarget = null },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TimeInput(state = timeState)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (target == TimeTarget.Start) {
                                startHour = timeState.hour
                                startMinute = timeState.minute
                            } else {
                                endHour = timeState.hour
                                endMinute = timeState.minute
                            }
                            timeTarget = null
                        },
                    ) {
                        Text(stringResource(Res.string.action_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { timeTarget = null }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun PickerButton(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(value)
        }
    }
}

private fun clockTime(
    hour: Int,
    minute: Int,
    uses24HourClock: Boolean,
): String {
    val minuteText = minute.toString().padStart(2, '0')
    return if (uses24HourClock) {
        "${hour.toString().padStart(2, '0')}:$minuteText"
    } else {
        "${(hour % HOURS_PER_HALF_DAY).takeUnless { it == 0 } ?: HOURS_PER_HALF_DAY}:$minuteText " +
            if (hour < HOURS_PER_HALF_DAY) "AM" else "PM"
    }
}

@Composable
private fun recurrenceLabel(
    recurrence: AvailabilityRecurrence,
): String = stringResource(
    when (recurrence) {
        AvailabilityRecurrence.SingleDay ->
            Res.string.availability_does_not_repeat

        AvailabilityRecurrence.Weekly -> Res.string.availability_weekly

        AvailabilityRecurrence.Fortnightly -> Res.string.availability_fortnightly

        AvailabilityRecurrence.Monthly -> Res.string.availability_monthly
    },
)

private class DatesBetween(
    private val minimumUtcMillis: Long,
    private val maximumUtcMillis: Long,
) : SelectableDates {
    override fun isSelectableDate(
        utcTimeMillis: Long,
    ): Boolean = utcTimeMillis in minimumUtcMillis..maximumUtcMillis
}

private data class DatePickerLimits(
    val minimumUtcMillis: Long,
    val maximumUtcMillis: Long,
)

private fun currentDatePickerLimits(): DatePickerLimits {
    val currentDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    fun pickerMillis(
        date: kotlinx.datetime.LocalDate,
    ): Long = LocalDateTime(date, LocalTime(0, 0))
        .toInstant(TimeZone.UTC)
        .toEpochMilliseconds()
    return DatePickerLimits(
        minimumUtcMillis = pickerMillis(currentDate),
        maximumUtcMillis = pickerMillis(
            currentDate.plus(MAX_AVAILABILITY_MONTHS, DateTimeUnit.MONTH),
        ),
    )
}

@Preview(
    name = "Referee availability",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
private fun AvailabilityScreenPreview() {
    DroolTheme(mode = com.github.apkelly.drool.domain.model.ThemeMode.Light) {
        AvailabilityScreen(
            model = AvailabilityUiModel(
                profileId = "ethan",
                profileName = "Ethan",
                state = AvailabilityUiState.Ready(
                    clubs = listOf(
                        RefereeClub(
                            id = "club",
                            name = "Marrickville Referees",
                            logoUrl = null,
                            timeZone = "Australia/Sydney",
                        ),
                    ),
                    availabilities = listOf(
                        RefereeAvailability(
                            id = "availability",
                            profileId = "ethan",
                            startEpochMillis = 1_792_392_000_000,
                            endEpochMillis = 1_792_395_600_000,
                            clubId = "club",
                            clubName = "Marrickville Referees",
                            recurrence = AvailabilityRecurrence.Weekly,
                            recurrenceEndEpochMillis = 1_798_952_400_000,
                        ),
                    ),
                ),
            ),
            onBack = {},
            onLoad = {},
            onRefresh = {},
            onCreate = {},
            onDelete = {},
            onClearFailure = {},
        )
    }
}
