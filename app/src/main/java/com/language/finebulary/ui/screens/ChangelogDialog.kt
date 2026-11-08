package com.language.finebulary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.language.finebulary.R

@Composable
fun ChangelogDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = {
            Text(
                text = stringResource(R.string.view_changelog),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Version 2.1
                ChangelogVersionSection(
                    versionTitle = stringResource(R.string.changelog_v2_1_title),
                    items = listOf(
                        stringResource(R.string.changelog_v2_1_item_1),
                        stringResource(R.string.changelog_v2_1_item_2)
                    )
                )

                // Version 2.0
                ChangelogVersionSection(
                    versionTitle = stringResource(R.string.changelog_v2_0_title),
                    items = listOf(
                        stringResource(R.string.changelog_v2_0_item_1),
                        stringResource(R.string.changelog_v2_0_item_2),
                        stringResource(R.string.changelog_v2_0_item_3),
                        stringResource(R.string.changelog_v2_0_item_4),
                        stringResource(R.string.changelog_v2_0_item_5),
                        stringResource(R.string.changelog_v2_0_item_6),
                        stringResource(R.string.changelog_v2_0_item_7),
                        stringResource(R.string.changelog_v2_0_item_8),
                        stringResource(R.string.changelog_v2_0_item_9),
                        stringResource(R.string.changelog_v2_0_item_10)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.changelog_got_it), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ChangelogVersionSection(versionTitle: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = versionTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        items.forEach { item ->
            Row(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = "•", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
