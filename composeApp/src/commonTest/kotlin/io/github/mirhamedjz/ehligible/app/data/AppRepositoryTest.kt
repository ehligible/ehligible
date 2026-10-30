package io.github.mirhamedjz.ehligible.app.data

import io.github.mirhamedjz.ehligible.core.model.Profile
import io.github.mirhamedjz.ehligible.core.model.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class AppRepositoryTest {
    private val path = "/data/${AppRepository.FILE_NAME}".toPath()

    @Test
    fun startsEmptyAndPersistsUpdates() = runTest {
        val fs = FakeFileSystem().apply { createDirectories("/data".toPath()) }
        val repository = AppRepository.create(path, backgroundScope, fs)
        assertNull(repository.current().profile)

        val profile = Profile(prDate = LocalDate(2024, 5, 1), trackingStart = LocalDate(2026, 10, 29))
        repository.update { it.copy(profile = profile, settings = Settings(marginDays = 14)) }

        assertTrue(fs.exists(path))
        assertEquals(profile, repository.current().profile)
        assertEquals(14, repository.current().settings.marginDays)
    }

    @Test
    fun anUnreadableFileIsKeptAsideAndTheAppStartsFresh() = runTest {
        val fs = FakeFileSystem().apply {
            createDirectories("/data".toPath())
            write(path) { writeUtf8("{ broken") }
        }
        val repository = AppRepository.create(path, backgroundScope, fs)
        assertNull(repository.current().profile)
        assertTrue(fs.exists("/data/${AppRepository.FILE_NAME}.corrupt".toPath()))
    }
}
