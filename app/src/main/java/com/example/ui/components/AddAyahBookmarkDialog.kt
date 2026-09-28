package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FamousAyah
import com.example.model.Surah
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAyahBookmarkDialog(
    surah: Surah,
    initialTimestampMs: Long = 0L,
    onSaveBookmark: (ayahNumber: Int, note: String, timestampMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var ayahInput by remember { mutableStateOf("1") }
    var noteInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    // Popular ayahs for this surah (if any)
    val popularInSurah = remember(surah.number) {
        FamousAyah.POPULAR_AYAHS.filter { it.surahNumber == surah.number }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Bookmark Ayah",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${surah.nameEnglish} • ${surah.totalVerses} Verses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Popular Ayahs suggestions (if available in this Surah)
                if (popularInSurah.isNotEmpty()) {
                    Text(
                        text = "Famous Ayahs in this Surah:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        popularInSurah.forEach { famous ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        ayahInput = famous.ayahNumber.toString()
                                        if (noteInput.isBlank()) {
                                            noteInput = famous.name
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ayah ${famous.ayahNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Ayah number input
                OutlinedTextField(
                    value = ayahInput,
                    onValueChange = {
                        ayahInput = it
                        inputError = null
                    },
                    label = { Text("Ayah Number (1 to ${surah.totalVerses})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = inputError != null,
                    supportingText = inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ayah_number_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Custom note/reflection input
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note / Title (Optional)") },
                    placeholder = { Text("e.g., Ayat al-Kursi, Dua, Reflection") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ayah_note_input")
                )

                if (initialTimestampMs > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📍 Captured at timestamp: ${formatTimeMs(initialTimestampMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldAccent
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ayahNum = ayahInput.trim().toIntOrNull()
                    if (ayahNum == null || ayahNum < 1 || ayahNum > surah.totalVerses) {
                        inputError = "Please enter an Ayah between 1 and ${surah.totalVerses}"
                    } else {
                        onSaveBookmark(ayahNum, noteInput.trim(), initialTimestampMs)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_ayah_bookmark_button")
            ) {
                Text("Save Bookmark", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
