package com.swordfish.lemuroid.app.mobile.feature.gamemenu.cheats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.cheats.Cheat
import com.swordfish.lemuroid.app.shared.cheats.CheatsManager
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.lib.library.db.entity.Game

private const val NEW_CHEAT_INDEX = -1

@Composable
fun GameMenuCheatsScreen(game: Game) {
    val context = LocalContext.current
    val cheatsManager = remember { CheatsManager(context) }
    val cheats = remember { mutableStateListOf<Cheat>().apply { addAll(cheatsManager.getCheats(game)) } }

    // null = no dialog, NEW_CHEAT_INDEX = new cheat, otherwise index of the cheat being edited
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    fun persist() {
        cheatsManager.saveCheats(game, cheats.toList())
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(R.string.cheats_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(R.string.cheats_add)) },
            icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cheats_add)) },
            onClick = { editingIndex = NEW_CHEAT_INDEX },
        )

        Divider()

        if (cheats.isEmpty()) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.cheats_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        cheats.forEachIndexed { index, cheat ->
            CheatRow(
                cheat = cheat,
                onClick = { editingIndex = index },
                onToggle = { enabled ->
                    cheats[index] = cheat.copy(enabled = enabled)
                    persist()
                },
            )
        }
    }

    val currentIndex = editingIndex
    if (currentIndex != null) {
        val existing = cheats.getOrNull(currentIndex)
        CheatEditDialog(
            initial = existing,
            defaultName = stringResource(R.string.cheats_default_name, cheats.size + 1),
            onDismiss = { editingIndex = null },
            onSave = { cheat ->
                if (existing == null) {
                    cheats.add(cheat)
                } else {
                    cheats[currentIndex] = cheat
                }
                persist()
                editingIndex = null
            },
            onDelete =
                if (existing != null) {
                    {
                        cheats.removeAt(currentIndex)
                        persist()
                        editingIndex = null
                    }
                } else {
                    null
                },
        )
    }
}

@Composable
private fun CheatRow(
    cheat: Cheat,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cheat.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = CheatsManager.normalizeCode(cheat.code).replace("+", " "),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = cheat.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun CheatEditDialog(
    initial: Cheat?,
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (Cheat) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    stringResource(
                        if (initial == null) R.string.cheats_add else R.string.cheats_edit,
                    ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.cheats_name)) },
                    placeholder = { Text(defaultName) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = code,
                    onValueChange = {
                        code = it
                        showError = false
                    },
                    minLines = 4,
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    label = { Text(stringResource(R.string.cheats_code)) },
                    placeholder = { Text("XXXXXXXX YYYYYYYY") },
                    isError = showError,
                    supportingText =
                        if (showError) {
                            { Text(stringResource(R.string.cheats_invalid_code)) }
                        } else {
                            null
                        },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!CheatsManager.isValidCode(code)) {
                        showError = true
                    } else {
                        onSave(
                            Cheat(
                                name = name.trim().ifEmpty { defaultName },
                                code = code.trim(),
                                enabled = initial?.enabled ?: true,
                            ),
                        )
                    }
                },
            ) {
                Text(stringResource(R.string.cheats_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.cheats_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cheats_cancel))
                }
            }
        },
    )
}
