package com.yukuza.launcher.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.yukuza.launcher.data.remote.GeocodingResult
import com.yukuza.launcher.ui.components.glass.GlassCard

@Composable
fun CityPickerPopup(
    cityQuery: String,
    citySuggestions: List<GeocodingResult>,
    cityName: String,
    onCityQueryChange: (String) -> Unit,
    onCitySelected: (GeocodingResult) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        GlassCard(
            Modifier
                .padding(top = 80.dp, end = 40.dp)
                .fillMaxWidth(0.35f),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (cityName.isNotBlank()) "City: $cityName" else "Select city",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )

                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = onCityQueryChange,
                    placeholder = { Text("Search city…", color = Color.White.copy(0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(0.6f),
                        unfocusedBorderColor = Color.White.copy(0.2f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (citySuggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(citySuggestions) { result ->
                            CityRow(
                                result = result,
                                onClick = {
                                    onCitySelected(result)
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityRow(result: GeocodingResult, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val subtitle = listOfNotNull(result.admin1, result.country).joinToString(", ")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                color = if (isFocused) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp),
            )
            .then(
                if (isFocused) Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                ) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            Text(
                text = result.name,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = if (isFocused) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 4.dp),
        color = Color.White.copy(alpha = 0.06f),
    )
}
