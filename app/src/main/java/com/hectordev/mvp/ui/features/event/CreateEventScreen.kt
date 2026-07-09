package com.hectordev.mvp.ui.features.event

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.hectordev.mvp.R
import com.hectordev.mvp.ui.core.extensions.toFormattedDate
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
fun CreateEventScreen(
    onEventCreated: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { viewModel.updateLocation(it.latitude, it.longitude) }
            }
        }
    }

    LaunchedEffect(uiState.isSuccess, uiState.errorMessage) {
        if (uiState.isSuccess) {
            onEventCreated()
            viewModel.resetUiState()
        }
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.resetUiState()
        }
    }

    CreateEventScreenContent(
        title = viewModel.title,
        startDate = viewModel.startDate,
        endDate = viewModel.endDate,
        locationLabel = viewModel.locationLabel,
        isLocationCaptured = viewModel.latitude != null,
        isLoading = uiState.isLoading,
        showErrors = viewModel.showErrors,
        onTitleChange = viewModel::updateTitle,
        onStartDateSelected = viewModel::updateStartDate,
        onEndDateSelected = viewModel::updateEndDate,
        onLocationLabelChange = viewModel::updateLocationLabel,
        onUseLocationClick = {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let { viewModel.updateLocation(it.latitude, it.longitude) }
                }
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        },
        onClearLocation = viewModel::clearLocation,
        onCreateClick = viewModel::createEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventScreenContent(
    title: String,
    startDate: Long,
    endDate: Long,
    locationLabel: String,
    isLocationCaptured: Boolean,
    isLoading: Boolean,
    showErrors: Boolean,
    onTitleChange: (String) -> Unit,
    onStartDateSelected: (Long) -> Unit,
    onEndDateSelected: (Long) -> Unit,
    onLocationLabelChange: (String) -> Unit,
    onUseLocationClick: () -> Unit,
    onClearLocation: () -> Unit,
    onCreateClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val todayMillis = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.takeIf { it > 0L }
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate.takeIf { it > 0L },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.create_event_screen_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val endDateError = when {
                showErrors && endDate == 0L -> stringResource(R.string.create_event_error_end_date_required)
                showErrors && endDate > 0L && endDate < todayMillis -> stringResource(R.string.create_event_error_end_date_past)
                showErrors && endDate > 0L && endDate < startDate -> stringResource(R.string.create_event_error_end_date_invalid)
                else -> null
            }

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.create_event_name_label)) },
                placeholder = { Text(stringResource(R.string.create_event_name_placeholder)) },
                isError = showErrors && title.isBlank(),
                supportingText = if (showErrors && title.isBlank()) {
                    { Text(stringResource(R.string.create_event_error_name_required)) }
                } else null,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = startDate.toFormattedDate()
                    .ifEmpty { stringResource(R.string.create_event_select_date) },
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.create_event_start_date_label)) },
                isError = showErrors && startDate == 0L,
                supportingText = if (showErrors && startDate == 0L) {
                    { Text(stringResource(R.string.create_event_error_start_date_required)) }
                } else null,
                trailingIcon = {
                    IconButton(
                        onClick = { showStartDatePicker = true },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endDate.toFormattedDate()
                    .ifEmpty { stringResource(R.string.create_event_select_date) },
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.create_event_end_date_label)) },
                isError = endDateError != null,
                supportingText = endDateError?.let { msg -> { Text(msg) } },
                trailingIcon = {
                    IconButton(
                        onClick = { showEndDatePicker = true },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = locationLabel,
                onValueChange = onLocationLabelChange,
                label = { Text(stringResource(R.string.create_event_location_label)) },
                placeholder = { Text(stringResource(R.string.create_event_location_placeholder)) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = if (isLocationCaptured) {
                    { Text(stringResource(R.string.create_event_location_captured)) }
                } else null
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onUseLocationClick,
                    enabled = !isLocationCaptured && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.create_event_use_location_btn))
                }
                if (isLocationCaptured) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onClearLocation,
                        enabled = !isLoading
                    ) {
                        Text(stringResource(R.string.create_event_clear_location_btn))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCreateClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.create_event_btn))
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let(onStartDateSelected)
                    showStartDatePicker = false
                }) { Text(stringResource(R.string.create_event_date_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.create_event_date_picker_cancel))
                }
            }
        ) { DatePicker(state = startDatePickerState) }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let(onEndDateSelected)
                    showEndDatePicker = false
                }) { Text(stringResource(R.string.create_event_date_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.create_event_date_picker_cancel))
                }
            }
        ) { DatePicker(state = endDatePickerState) }
    }
}

// --- SCREEN PREVIEWS ---

@Preview(showBackground = true, name = "Location not captured")
@Composable
private fun CreateEventScreenPreview() {
    MVPTheme {
        Surface {
            CreateEventScreenContent(
                title = "Summer Trip 2025",
                startDate = 1_750_000_000_000L,
                endDate = 1_750_500_000_000L,
                locationLabel = "",
                isLocationCaptured = false,
                isLoading = false,
                showErrors = false,
                onTitleChange = {},
                onStartDateSelected = {},
                onEndDateSelected = {},
                onLocationLabelChange = {},
                onUseLocationClick = {},
                onClearLocation = {},
                onCreateClick = {},
                onBack = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Validation errors")
@Composable
private fun CreateEventScreenLocationCapturedPreview() {
    MVPTheme {
        Surface {
            CreateEventScreenContent(
                title = "",
                startDate = 0L,
                endDate = 0L,
                locationLabel = "",
                isLocationCaptured = false,
                isLoading = false,
                showErrors = true,
                onTitleChange = {},
                onStartDateSelected = {},
                onEndDateSelected = {},
                onLocationLabelChange = {},
                onUseLocationClick = {},
                onClearLocation = {},
                onCreateClick = {},
                onBack = {}
            )
        }
    }
}