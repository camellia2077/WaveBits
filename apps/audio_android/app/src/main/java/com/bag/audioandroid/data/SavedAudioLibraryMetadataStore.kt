package com.bag.audioandroid.data

import android.content.Context
import android.content.SharedPreferences
import com.bag.audioandroid.domain.SavedAudioFolder
import com.bag.audioandroid.domain.SavedAudioFolderMutationResult
import com.bag.audioandroid.domain.SavedAudioLibraryMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SavedAudioLibraryMetadataStore(
    appContext: Context,
) {
    private val sharedPreferences: SharedPreferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun readMetadata(): SavedAudioLibraryMetadata {
        val folders = readFolders()
        val folderIds = folders.map { it.folderId }.toSet()
        val assignments =
            readAssignments().filterValues { folderId ->
                folderId in folderIds
            }
        return SavedAudioLibraryMetadata(
            folders = folders,
            itemFolderAssignments = assignments,
        )
    }

    fun createFolder(name: String): SavedAudioFolderMutationResult {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return SavedAudioFolderMutationResult.Failed
        }
        val folders = readFolders().toMutableList()
        if (folders.any { it.name.equals(normalizedName, ignoreCase = true) }) {
            return SavedAudioFolderMutationResult.DuplicateName
        }
        val folder =
            SavedAudioFolder(
                folderId = UUID.randomUUID().toString(),
                name = normalizedName,
                createdAtEpochMillis = System.currentTimeMillis(),
            )
        folders += folder
        return if (writeFolders(folders)) {
            SavedAudioFolderMutationResult.Success(folder)
        } else {
            SavedAudioFolderMutationResult.Failed
        }
    }

    fun renameFolder(
        folderId: String,
        name: String,
    ): SavedAudioFolderMutationResult {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return SavedAudioFolderMutationResult.Failed
        }
        val folders = readFolders()
        val currentFolder = folders.firstOrNull { it.folderId == folderId } ?: return SavedAudioFolderMutationResult.Failed
        if (folders.any { it.folderId != folderId && it.name.equals(normalizedName, ignoreCase = true) }) {
            return SavedAudioFolderMutationResult.DuplicateName
        }
        val updatedFolder = currentFolder.copy(name = normalizedName)
        val updatedFolders = folders.map { folder -> if (folder.folderId == folderId) updatedFolder else folder }
        return if (writeFolders(updatedFolders)) {
            SavedAudioFolderMutationResult.Success(updatedFolder)
        } else {
            SavedAudioFolderMutationResult.Failed
        }
    }

    fun deleteFolder(folderId: String): Boolean {
        val folders = readFolders()
        if (folders.none { it.folderId == folderId }) {
            return false
        }
        val updatedFolders = folders.filterNot { it.folderId == folderId }
        val updatedAssignments = readAssignments().filterValues { assignedFolderId -> assignedFolderId != folderId }
        return sharedPreferences.edit()
            .putString(KEY_FOLDERS, foldersToJson(updatedFolders).toString())
            .putString(KEY_ASSIGNMENTS, assignmentsToJson(updatedAssignments).toString())
            .commit()
    }

    fun assignItemsToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ): Boolean {
        val sanitizedItemIds = itemIds.map(String::trim).filter(String::isNotEmpty).toSet()
        if (sanitizedItemIds.isEmpty()) {
            return false
        }
        val folders = readFolders()
        if (folderId != null && folders.none { it.folderId == folderId }) {
            return false
        }
        val updatedAssignments = readAssignments().toMutableMap()
        sanitizedItemIds.forEach { itemId ->
            if (folderId == null) {
                updatedAssignments.remove(itemId)
            } else {
                updatedAssignments[itemId] = folderId
            }
        }
        return writeAssignments(updatedAssignments)
    }

    fun pruneItemAssignments(validItemIds: Set<String>) {
        val currentAssignments = readAssignments()
        val prunedAssignments = currentAssignments.filterKeys { itemId -> itemId in validItemIds }
        if (prunedAssignments != currentAssignments) {
            writeAssignments(prunedAssignments)
        }
    }

    private fun readFolders(): List<SavedAudioFolder> {
        val rawValue = sharedPreferences.getString(KEY_FOLDERS, null).orEmpty()
        if (rawValue.isBlank()) {
            return emptyList()
        }
        return runCatching {
            val array = JSONArray(rawValue)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val folderId = item.optString(JSON_FOLDER_ID).trim()
                    val name = item.optString(JSON_NAME).trim()
                    val createdAtEpochMillis = item.optLong(JSON_CREATED_AT)
                    if (folderId.isBlank() || name.isBlank()) {
                        continue
                    }
                    add(
                        SavedAudioFolder(
                            folderId = folderId,
                            name = name,
                            createdAtEpochMillis = createdAtEpochMillis,
                        ),
                    )
                }
            }.sortedBy { it.createdAtEpochMillis }
        }.getOrDefault(emptyList())
    }

    private fun readAssignments(): Map<String, String> {
        val rawValue = sharedPreferences.getString(KEY_ASSIGNMENTS, null).orEmpty()
        if (rawValue.isBlank()) {
            return emptyMap()
        }
        return runCatching {
            val jsonObject = JSONObject(rawValue)
            buildMap {
                jsonObject.keys().forEach { itemId ->
                    val folderId = jsonObject.optString(itemId).trim()
                    if (itemId.isNotBlank() && folderId.isNotBlank()) {
                        put(itemId, folderId)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun writeFolders(folders: List<SavedAudioFolder>): Boolean =
        sharedPreferences.edit()
            .putString(KEY_FOLDERS, foldersToJson(folders).toString())
            .commit()

    private fun writeAssignments(assignments: Map<String, String>): Boolean =
        sharedPreferences.edit()
            .putString(KEY_ASSIGNMENTS, assignmentsToJson(assignments).toString())
            .commit()

    private fun foldersToJson(folders: List<SavedAudioFolder>): JSONArray =
        JSONArray().apply {
            folders.forEach { folder ->
                put(
                    JSONObject()
                        .put(JSON_FOLDER_ID, folder.folderId)
                        .put(JSON_NAME, folder.name)
                        .put(JSON_CREATED_AT, folder.createdAtEpochMillis),
                )
            }
        }

    private fun assignmentsToJson(assignments: Map<String, String>): JSONObject =
        JSONObject().apply {
            assignments.toSortedMap().forEach { (itemId, folderId) ->
                put(itemId, folderId)
            }
        }

    private companion object {
        const val PREFERENCES_NAME = "saved_audio_library_metadata"
        const val KEY_FOLDERS = "folders"
        const val KEY_ASSIGNMENTS = "assignments"
        const val JSON_FOLDER_ID = "folderId"
        const val JSON_NAME = "name"
        const val JSON_CREATED_AT = "createdAtEpochMillis"
    }
}
