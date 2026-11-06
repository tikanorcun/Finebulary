package com.language.finebulary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.language.finebulary.R
import com.language.finebulary.data.model.Word

@Composable
fun EditWordDialog(
    word: Word,
    onDismiss: () -> Unit,
    onConfirm: (Word) -> Unit
) {
    var front by remember { mutableStateOf(word.front) }
    var back by remember { mutableStateOf(word.backsPacked) }
    var definition by remember { mutableStateOf(word.definition ?: "") }
    var example by remember { mutableStateOf(word.example ?: "") }
    var synonyms by remember { mutableStateOf(word.synonyms ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.edit_word), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text(stringResource(R.string.word_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text(stringResource(R.string.meaning_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text(stringResource(R.string.definition_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text(stringResource(R.string.example_sentence)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = synonyms,
                    onValueChange = { synonyms = it },
                    label = { Text(stringResource(R.string.synonyms_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        word.copy(
                            front = front,
                            backsPacked = back,
                            definition = definition.ifBlank { null },
                            example = example.ifBlank { null },
                            synonyms = synonyms.ifBlank { null }
                        )
                    )
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(R.string.save_changes), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
