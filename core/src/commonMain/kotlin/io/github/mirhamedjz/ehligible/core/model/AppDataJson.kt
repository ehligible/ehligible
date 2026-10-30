package io.github.mirhamedjz.ehligible.core.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** JSON form of [AppData], used for storage and for backup files. */
object AppDataJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: AppData): String = json.encodeToString(AppData.serializer(), data)

    /** @throws InvalidBackupException if [text] isn't a backup this version of the app can read. */
    fun decode(text: String): AppData {
        val data = try {
            json.decodeFromString(AppData.serializer(), text)
        } catch (e: SerializationException) {
            throw InvalidBackupException("Not an Eh-ligible backup", e)
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException("Backup contains invalid data", e)
        }
        if (data.schemaVersion > AppData.CURRENT_SCHEMA_VERSION) {
            throw InvalidBackupException("Backup was made by a newer version of Eh-ligible")
        }
        return data
    }
}

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
