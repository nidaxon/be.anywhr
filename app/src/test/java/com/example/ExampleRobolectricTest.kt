package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.io.GeoDataExporter
import com.example.data.model.BookmarkEntity
import com.example.data.model.LocationPoint
import org.junit.Assert.assertEquals
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
        assertEquals("be.anywhr", appName)
    }

    @Test
    fun `test distance calculation`() {
        val p1 = LocationPoint(-6.175392, 106.827153)
        val p2 = LocationPoint(-6.175392, 106.827153)
        assertEquals(0.0, p1.distanceTo(p2), 0.1)
    }

    @Test
    fun `test csv export and import`() {
        val list = listOf(
            BookmarkEntity(
                name = "Eiffel Tower",
                latitude = 48.8584,
                longitude = 2.2945,
                category = "Travel",
                description = "Paris Landmark"
            )
        )
        val csv = GeoDataExporter.exportBookmarksToCsv(list)
        assertTrue(csv.contains("Eiffel Tower"))

        val parsed = GeoDataExporter.parseCsvBookmarks(csv)
        assertEquals(1, parsed.size)
        assertEquals("Eiffel Tower", parsed[0].name)
        assertEquals(48.8584, parsed[0].latitude, 0.0001)
    }
}
