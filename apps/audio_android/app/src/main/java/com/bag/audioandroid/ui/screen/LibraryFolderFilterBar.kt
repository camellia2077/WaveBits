package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioFolder
import com.bag.audioandroid.ui.model.LibraryFolderFilter

@Composable
internal fun LibraryFolderFilterBar(
    folders: List<SavedAudioFolder>,
    selectedFilter: LibraryFolderFilter,
    onFilterSelected: (LibraryFolderFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryFolderChip(
            label = stringResource(R.string.common_all),
            selected = selectedFilter == LibraryFolderFilter.All,
            onClick = { onFilterSelected(LibraryFolderFilter.All) },
        )
        LibraryFolderChip(
            label = stringResource(R.string.library_folder_uncategorized),
            selected = selectedFilter == LibraryFolderFilter.Uncategorized,
            onClick = { onFilterSelected(LibraryFolderFilter.Uncategorized) },
        )
        folders.forEach { folder ->
            LibraryFolderChip(
                label = folder.name,
                selected = selectedFilter == LibraryFolderFilter.Folder(folder.folderId),
                onClick = { onFilterSelected(LibraryFolderFilter.Folder(folder.folderId)) },
            )
        }
    }
}

@Composable
private fun LibraryFolderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}
