package io.github.mirhamedjz.ehligible.app.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import io.github.mirhamedjz.ehligible.core.model.AppData
import io.github.mirhamedjz.ehligible.core.model.AppDataJson
import io.github.mirhamedjz.ehligible.core.model.InvalidBackupException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

/** The single source of truth for [AppData]: one JSON file, written atomically by DataStore. */
class AppRepository(private val store: DataStore<AppData>) {
    val data: Flow<AppData> get() = store.data

    suspend fun current(): AppData = store.data.first()

    suspend fun update(transform: (AppData) -> AppData): AppData = store.updateData { transform(it) }

    companion object {
        const val FILE_NAME: String = "ehligible.json"

        /** Only one repository may exist per file; platforms keep it in their app-wide graph. */
        fun create(path: Path, scope: CoroutineScope, fileSystem: FileSystem = FileSystem.SYSTEM): AppRepository {
            val store = DataStoreFactory.create(
                storage = OkioStorage(fileSystem, AppDataSerializer, producePath = { path }),
                corruptionHandler = ReplaceFileCorruptionHandler {
                    // Keep the unreadable file for recovery instead of silently losing it.
                    path.parent?.let { dir -> fileSystem.copy(path, dir / "$FILE_NAME.corrupt") }
                    AppData()
                },
                scope = scope,
            )
            return AppRepository(store)
        }
    }
}

internal object AppDataSerializer : OkioSerializer<AppData> {
    override val defaultValue: AppData = AppData()

    override suspend fun readFrom(source: BufferedSource): AppData = try {
        AppDataJson.decode(source.readUtf8())
    } catch (e: InvalidBackupException) {
        throw CorruptionException("Unreadable data file", e)
    }

    override suspend fun writeTo(t: AppData, sink: BufferedSink) {
        sink.writeUtf8(AppDataJson.encode(t))
    }
}
