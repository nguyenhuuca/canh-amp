package com.canhamp.ui.eq

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import com.canhamp.R
import com.canhamp.data.eq.SaveResult
import com.canhamp.ui.theme.Deck

private const val MAX_INPUT = 64

/**
 * Name dialog for a user preset (B48). [onSave] runs the save and reports the result; inline
 * errors show under the field, and a duplicate asks to overwrite.
 */
@Composable
fun SavePresetDialog(
    onSave: (name: String, overwrite: Boolean, onResult: (SaveResult) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Deck.colors
    var name by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<SaveResult?>(null) }
    var overwriteName by remember { mutableStateOf<String?>(null) }

    fun handle(result: SaveResult) {
        when (result) {
            is SaveResult.Saved -> onDismiss()
            SaveResult.InvalidName, SaveResult.BuiltInName -> problem = result
            is SaveResult.DuplicateName -> overwriteName = result.name
        }
    }

    val overwrite = overwriteName
    if (overwrite != null) {
        AlertDialog(
            onDismissRequest = { overwriteName = null },
            containerColor = c.surfaceContainer,
            title = { Text(stringResource(R.string.eq_overwrite, overwrite.uppercase()), style = Deck.type.labelLg, color = c.onSurface) },
            confirmButton = {
                TextButton(onClick = {
                    overwriteName = null
                    onSave(name, true, { handle(it) })
                }) { Text(stringResource(R.string.pl_yes), style = Deck.type.labelLg, color = c.primaryContainer) }
            },
            dismissButton = {
                TextButton(onClick = { overwriteName = null }) {
                    Text(stringResource(R.string.pl_cancel), style = Deck.type.labelLg, color = c.onSurfaceVariant)
                }
            },
        )
        return
    }

    val errorText = when (problem) {
        SaveResult.InvalidName -> stringResource(R.string.eq_name_invalid)
        SaveResult.BuiltInName -> stringResource(R.string.eq_name_builtin)
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceContainer,
        title = { Text(stringResource(R.string.eq_save_title), style = Deck.type.labelLg, color = c.onSurface) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(MAX_INPUT)
                        problem = null
                    },
                    singleLine = true,
                    isError = errorText != null,
                    label = { Text(stringResource(R.string.eq_name_label), style = Deck.type.labelSm) },
                    supportingText = errorText?.let { msg ->
                        { Text(msg, style = Deck.type.labelSm, color = c.error, modifier = Modifier.semantics { error(msg) }) }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, false, { handle(it) }) }) {
                Text(stringResource(R.string.eq_save), style = Deck.type.labelLg, color = c.primaryContainer)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pl_cancel), style = Deck.type.labelLg, color = c.onSurfaceVariant)
            }
        },
    )
}
