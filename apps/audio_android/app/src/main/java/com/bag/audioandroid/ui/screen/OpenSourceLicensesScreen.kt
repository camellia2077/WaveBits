package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val libraries by rememberLibraries(R.raw.aboutlibraries)
    var searchQuery by remember { mutableStateOf("") }
    val filteredLibraries =
        remember(libraries, searchQuery) {
            val currentLibraries = libraries ?: return@remember null
            currentLibraries.filterByQuery(searchQuery)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.common_back), color = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        val contentModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)

        Column(modifier = contentModifier) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search libraries") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (filteredLibraries == null) {
                Text(
                    text = stringResource(R.string.licenses_loading),
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                LibrariesContainer(
                    libraries = filteredLibraries,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxSize()
                            .padding(top = 16.dp),
                )
            }
        }
    }
}

private fun Library.matchesQuery(query: String): Boolean {
    val candidateTexts =
        listOfNotNull(
            name,
            artifactId,
            uniqueId,
        )
    return candidateTexts.any { candidate ->
        candidate.contains(query, ignoreCase = true)
    }
}

private fun Libs.filterByQuery(query: String): Libs {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) {
        return this
    }
    return copy(
        libraries =
            libraries
                .filter { library ->
                    library.matchesQuery(normalizedQuery)
                }.toImmutableList(),
    )
}
