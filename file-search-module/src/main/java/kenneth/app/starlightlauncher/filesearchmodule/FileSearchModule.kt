package kenneth.app.starlightlauncher.filesearchmodule

import java.util.Collections.synchronizedList
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.SpotlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kotlinx.coroutines.*
import java.nio.file.Paths
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.filesearchmodule"

class FileSearchModule : SearchModule {
    override lateinit var metadata: SearchModule.Metadata
        private set

    override lateinit var adapter: SearchResultAdapter
        private set

    private lateinit var mainContext: Context
    private lateinit var preferences: FileSearchModulePreferences

    override fun initialize(launcher: SpotlightLauncherApi) {
        mainContext = launcher.context
        preferences = FileSearchModulePreferences.getInstance(mainContext)
        metadata = SearchModule.Metadata(
            extensionName = mainContext.getString(R.string.file_search_module_name),
            displayName = mainContext.getString(R.string.file_search_module_display_name),
            description = mainContext.getString(R.string.file_search_module_description),
        )
        adapter = FileSearchResultAdapter(preferences, launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult {
        val paths = preferences.includedPaths
        return when {
            paths.isEmpty() -> Result(keyword, emptyList())
            else -> Result(
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
            DocumentFile.fromTreeUri(mainContext, Uri.parse(path))?.let {
                CoroutineScope(Dispatchers.IO)
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

    class Result(query: String, val files: List<DocumentFile>) : SearchResult(query, EXTENSION_NAME)
}