package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Surah
import com.example.ui.theme.GoldAccent

@Composable
fun GlobalAddAyahDialog(
    allSurahs: List<Surah> = Surah.ALL_SURAHS,
    onSaveBookmark: (surahNumber: Int, ayahNumber: Int, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSurah by remember { mutableStateOf(allSurahs.firstOrNull() ?: Surah.ALL_SURAHS[0]) }
    var showSurahDropdown by remember { mutableStateOf(false) }
    var ayahInput by remember { mutableStateOf("1") }
    var noteInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

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
                Text(
                    text = "Bookmark an Ayah",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Surah",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Surah selector button with dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showSurahDropdown = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_surah_dropdown_trigger")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedSurah.number}. ${selectedSurah.nameEnglish} (${selectedSurah.nameArabic})",
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showSurahDropdown,
                        onDismissRequest = { showSurahDropdown = false },
                        modifier = Modifier.height(280.dp)
                    ) {
                        allSurahs.forEach { surah ->
                            DropdownMenuItem(
                                text = {
                                    Text("${surah.number}. ${surah.nameEnglish} - ${surah.totalVerses} Ayahs")
                                },
                                onClick = {
                                    selectedSurah = surah
                                    showSurahDropdown = false
                                    ayahInput = "1"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ayah input
                OutlinedTextField(
                    value = ayahInput,
                    onValueChange = {
                        ayahInput = it
                        inputError = null
                    },
                    label = { Text("Ayah Number (1 to ${selectedSurah.totalVerses})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = inputError != null,
                    supportingText = inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_ayah_number_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Note input
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note / Reflection (Optional)") },
                    placeholder = { Text("e.g. Favorite Ayah, Daily Morning Dua") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_ayah_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ayahNum = ayahInput.trim().toIntOrNull()
                    if (ayahNum == null || ayahNum < 1 || ayahNum > selectedSurah.totalVerses) {
                        inputError = "Please enter an Ayah between 1 and ${selectedSurah.totalVerses}"
                    } else {
                        onSaveBookmark(selectedSurah.number, ayahNum, noteInput.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_global_ayah_bookmark_button")
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
