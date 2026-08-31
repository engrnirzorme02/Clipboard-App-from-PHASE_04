package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.CaptureResult
import com.example.domain.model.ClipItem
import com.example.domain.model.ClipSource
import com.example.domain.model.SensitivityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Clipboard Vault", appName)
  }

  @Test
  fun `clip item creation and short code generation`() {
    val clip = ClipItem(
      text = "https://example.com/api/test",
      normalizedHash = "abc123hash",
      title = "Example Link",
      tags = listOf("link", "api"),
      source = ClipSource.KEYBOARD
    )
    assertNotNull(clip.id)
    assertTrue(clip.shortCode.startsWith("CLP-"))
    assertEquals(SensitivityLevel.NORMAL, clip.sensitivity)
  }
}
