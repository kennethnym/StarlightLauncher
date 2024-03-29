package kenneth.app.starlightlauncher.filesearchmodule

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Collections.synchronizedList

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.filesearchmodule"

class FileSearchModule(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SearchModule(context) {
    override val metadata = Metadata(
        extensionName = context.getString(R.string.file_search_module_name),
        displayName = context.getString(R.string.file_search_module_display_name),
        description = context.getString(R.string.file_search_module_description),
    )

    override lateinit var adapter: SearchResultAdapter
        private set

    private lateinit var preferences: FileSearchModulePreferences

    override fun initialize(launcher: StarlightLauncherApi) {
        preferences = FileSearchModulePreferences.getInstance(launcher.dataStore)
        adapter = FileSearchResultAdapter(launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult {
        val paths = preferences.includedPaths.first()
        return when {
            paths.isEmpty() -> Result.NoSearchPathsAdded(keyword)
            else -> Result.Some(
                query = keyword,
                files = collectAllFilesInPaths(paths, keywordRegex)
            )
        }
    }

    private suspend fun collectAllFilesInPaths(
        paths: Collection<String>,
        regex: Regex
    ): List<DocumentFile> {
        val files = synchronizedList(mutableListOf<DocumentFile>())
        val jobs = mutableListOf<Job>()
        paths.forEach { path ->
            DocumentFile.fromTreeUri(context, Uri.parse(path))?.let {
                CoroutineScope(ioDispatcher)
                    .launch { files += collectAllFiles(it, regex) }
                    .also { jobs += it }
            }
        }
        jobs.forEach { it.join() }

        return files
    }

    private fun collectAllFiles(doc: DocumentFile, regex: Regex): List<DocumentFile> {
        val files = mutableListOf<DocumentFile>()
        doc.listFiles().forEach { file ->
            when {
                file.isDirectory -> {
                    files.addAll(collectAllFiles(file, regex))
                }
                file.name?.contains(regex) == true -> {
                    files.add(file)
                }
                else -> {}
            }
        }
        return files
    }

    sealed class Result(query: String) :
        SearchResult(query, EXTENSION_NAME) {
        class Some(query: String, val files: List<DocumentFile>) : Result(query)

        class NoSearchPathsAdded(query: String) : Result(query)
    }
}
