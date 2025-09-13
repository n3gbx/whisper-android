package org.n3gbx.whisper.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commandiron.wheel_picker_compose.WheelDatePicker
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatePickerDialog(
    modifier: Modifier = Modifier,
    currentDate: LocalDate?,
    yearsRange: IntRange? = IntRange(1970, LocalDate.now().year),
    minDate: LocalDate = LocalDate.MIN,
    maxDate: LocalDate = LocalDate.MAX,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var localDate by rememberSaveable { mutableStateOf(currentDate ?: LocalDate.now()) }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier.padding(PaddingValues(all = 24.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        modifier = modifier,
                        text = "Select date",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .align(Alignment.Start)
                ) {
                    WheelDatePicker(
                        startDate = localDate,
                        minDate = minDate,
                        maxDate = maxDate,
                        yearsRange = yearsRange,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        selectorProperties = WheelPickerDefaults.selectorProperties(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            border = null,
                        ),
                        onSnappedDate = {
                            localDate = it
                        }
                    )
                }
                Box(
                    modifier = Modifier.align(Alignment.End)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text(text = "Cancel")
                        }
                        TextButton(
                            onClick = {
                                onSelect(localDate)
                            }
                        ) {
                            Text(text = "Select")
                        }
                    }
                }
            }
        }
    }
}