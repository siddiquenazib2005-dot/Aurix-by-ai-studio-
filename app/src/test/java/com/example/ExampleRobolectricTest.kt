package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.aurix.tools.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals("AURIX", appName)
    }

    @Test
    fun `tool registry initializes core tools`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = ToolRegistry(context)
        val tools = registry.getAllTools()

        assertTrue("Registry should have tools registered", tools.size >= 15)
        assertNotNull(registry.getTool("FLASHLIGHT"))
        assertNotNull(registry.getTool("VOLUME"))
        assertNotNull(registry.getTool("DEVICE_STATUS"))
        assertNotNull(registry.getTool("CALL_CONTACT"))
        assertNotNull(registry.getTool("SEND_SMS"))
        assertNotNull(registry.getTool("CONTACT_SEARCH"))
        assertNotNull(registry.getTool("LOCATION"))
        assertNotNull(registry.getTool("CAMERA"))
        assertNotNull(registry.getTool("FILE_SEARCH"))
        assertNotNull(registry.getTool("APP_AUTOMATION"))
        assertNotNull(registry.getTool("SCREEN_ANALYSIS"))
    }
}
