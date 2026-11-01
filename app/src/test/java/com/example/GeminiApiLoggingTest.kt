package com.example

import com.example.api.GeminiApiClient
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiLoggingTest {
  @Test
  fun networkCallsDoNotLogApiKeysOrPrivateContent() {
    val requestMarker = "synthetic-private-note-721"
    val responseMarker = "synthetic-private-response-812"
    val keyMarker = "synthetic-api-key-934"
    val messages = CopyOnWriteArrayList<String>()
    val logger = Logger.getLogger(OkHttpClient::class.java.name)
    val oldLevel = logger.level
    val oldParentHandlers = logger.useParentHandlers
    val handler = object : Handler() {
      override fun publish(record: LogRecord) { messages.add(record.message) }
      override fun flush() {}
      override fun close() {}
    }.apply { level = Level.ALL }
    val server = MockWebServer()

    logger.level = Level.ALL
    logger.useParentHandlers = false
    logger.addHandler(handler)
    try {
      server.start()
      server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
        .setBody("{\"text\":\"$responseMarker\"}"))
      // Use the actual client wired into the production Retrofit service.
      val field = GeminiApiClient::class.java.getDeclaredField("okHttpClient")
      field.isAccessible = true
      val client = field.get(GeminiApiClient) as OkHttpClient
      val url = server.url("/generateContent").newBuilder()
        .addQueryParameter("key", keyMarker).build()
      val body = "{\"text\":\"$requestMarker\"}".toRequestBody("application/json".toMediaType())
      client.newCall(Request.Builder().url(url).post(body).build()).execute().use { response ->
        assertEquals(200, response.code)
        assertTrue(response.body!!.string().contains(responseMarker))
      }
      val recorded = server.takeRequest(5, TimeUnit.SECONDS)!!
      assertEquals(keyMarker, recorded.requestUrl!!.queryParameter("key"))
      assertTrue(recorded.body.readUtf8().contains(requestMarker))
      val logs = messages.joinToString("\n")
      assertTrue("Credentials or private payloads appeared in HTTP logs",
        listOf(requestMarker, responseMarker, keyMarker).none(logs::contains))
    } finally {
      server.shutdown()
      logger.removeHandler(handler)
      logger.level = oldLevel
      logger.useParentHandlers = oldParentHandlers
    }
  }
}
