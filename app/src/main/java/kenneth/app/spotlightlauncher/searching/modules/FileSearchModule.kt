package kenneth.app.spotlightlauncher.searching.modules

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
import kenneth.app.spotlightlauncher.searching.SearchResult
import kenneth.app.spotlightlauncher.searching.compareStringsWithRegex

class FileSearchModule(
    private val context: Context,
    private val filePreferenceManager: FilePreferenceManager
) : SearchModule {
    override fun search(keyword: String, keywordRegex: Regex): SearchResult {
        val paths = filePreferenceManager.includedPaths

        if (paths.size == 0) {
            return SearchResult.Files(keyword, null)
        }

        return SearchResult.Files(
            keyword,
            files = paths
                .fold(listOf<DocumentFile>()) { allFiles, path ->
                    if (path == "") return@fold allFiles
                    val doc = DocumentFile.fromTreeUri(context, Uri.parse(path))
                    if (doc != null) allFiles + getFilesRecursive(doc) else allFiles
                }
                .filter { it.name?.contains(keywordRegex) ?: false }
                .sortedWith { file1, file2 ->
                    compareStringsWithRegex(
                        file1.name!!,
                        file2.name!!,
                        keywordRegex
                    )
                }
        )
    }

    private fun getFilesRecursive(root: DocumentFile): List<DocumentFile> {
        val files = mutableListOf<DocumentFile>()

        root.listFiles().forEach { file ->
            if (file.isDirectory) {
                files.addAll(getFilesRecursive(file))
            } else {
                files.add(file)
            }
        }

        return files
    }
}
