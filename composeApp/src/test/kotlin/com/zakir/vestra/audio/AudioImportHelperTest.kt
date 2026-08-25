package com.zakir.vestra.audio

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** [AudioImportHelper.copyUriToCache] against a real `file://` URI — no ContentResolver needed for that scheme. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class AudioImportHelperTest {

    @Test
    fun copiesAFileUriIntoTheAudioRecordingsCache() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val sourceDir = File(context.cacheDir, "import_source").also { it.mkdirs() }
        val sourceFile = File(sourceDir, "clip.wav")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        val copiedPath = AudioImportHelper.copyUriToCache(context, Uri.fromFile(sourceFile))

        assertNotNull("expected a non-null copied path", copiedPath)
        val copiedFile = File(copiedPath!!)
        assertTrue("copied file should exist", copiedFile.exists())
        assertTrue("copied file should land under audio_recordings", copiedFile.path.contains("audio_recordings"))
        assertTrue("copied file should have the source bytes", copiedFile.readBytes().contentEquals(sourceFile.readBytes()))
    }

    @Test
    fun missingSourceFileReturnsNull() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val missing = File(context.cacheDir, "does_not_exist.wav")

        val copiedPath = AudioImportHelper.copyUriToCache(context, Uri.fromFile(missing))

        assertTrue(copiedPath == null)
    }
}
