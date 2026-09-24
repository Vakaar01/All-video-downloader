package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DownloadEntity
import com.example.data.PlatformType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vakaar Cyber Downloader", appName)
    }

    @Test
    fun `empty URL triggers clear error state`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        viewModel.onUrlChanged("")
        viewModel.onStartExtraction()

        val error = viewModel.appError.value
        assertNotNull(error)
        assertEquals("URL EMPTY", error?.title)
        assertTrue(error?.message?.contains("khali") == true)
    }

    @Test
    fun `invalid URL format triggers error state`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        viewModel.onUrlChanged("some-random-broken-text")
        viewModel.onStartExtraction()

        val error = viewModel.appError.value
        assertNotNull(error)
        assertEquals("INVALID URL FORMAT", error?.title)
        assertTrue(error?.solution?.contains("Share Link") == true)
    }

    @Test
    fun `dismiss error clears error state`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        viewModel.onUrlChanged("")
        viewModel.onStartExtraction()
        assertNotNull(viewModel.appError.value)

        viewModel.onDismissError()
        assertNull(viewModel.appError.value)
    }

    @Test
    fun `auto detect platforms correctly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        viewModel.onUrlChanged("https://www.instagram.com/reel/C5ABCDEF123/")
        assertEquals(PlatformType.INSTAGRAM, viewModel.selectedPlatform.value)

        viewModel.onUrlChanged("https://www.tiktok.com/@user/video/1234567890")
        assertEquals(PlatformType.TIKTOK, viewModel.selectedPlatform.value)

        viewModel.onUrlChanged("https://youtu.be/dQw4w9WgXcQ")
        assertEquals(PlatformType.YOUTUBE, viewModel.selectedPlatform.value)
    }

    @Test
    fun `room database stores and retrieves downloaded files in vakaar folder`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val dao = db.downloadDao()

        val testItem = DownloadEntity(
            title = "Test Reel",
            originalUrl = "https://instagram.com/reel/123",
            platformName = "Instagram",
            formatName = "MP4 Video",
            fileSizeBytes = 1048576L,
            formattedSize = "1.0 MB",
            filePath = "/storage/emulated/0/Download/vakaar/vakaar_instagram_test.mp4",
            isAudio = false
        )

        dao.insertDownload(testItem)
        val all = dao.getAllDownloadsList()
        assertTrue(all.isNotEmpty())
        assertTrue(all.any { it.filePath.contains("Download/vakaar") })
    }
}
