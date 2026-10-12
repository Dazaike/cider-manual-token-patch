package com.dazaike.ciderpatcher.core

import com.cidercollective.cider.sidcloud.CloudSidException
import com.cidercollective.cider.sidcloud.GeminiClient
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

class GeminiClientTest {
    private class Seen(val path: String, val key: String?, val body: String)

    private lateinit var server: HttpServer
    private val seen = ArrayList<Seen>()
    private val responses = ArrayDeque<Pair<Int, String>>()

    @Before
    fun setUp() {
        // The client remembers "thinkingConfig unsupported" in a static; reset between tests.
        GeminiClient::class.java.getDeclaredField("thinkingUnsupported").apply { isAccessible = true }
            .setBoolean(null, false)
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            seen += Seen(ex.requestURI.path, ex.requestHeaders.getFirst("x-goog-api-key"), ex.requestBody.readBytes().toString(Charsets.UTF_8))
            val (status, body) = responses.removeFirst()
            val bytes = body.toByteArray()
            ex.sendResponseHeaders(status, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @After
    fun tearDown() = server.stop(0)

    private fun client() = GeminiClient("http://127.0.0.1:${server.address.port}")

    private fun ok(text: String, finish: String = "STOP") = 200 to
        org.json.JSONObject().put(
            "candidates",
            org.json.JSONArray().put(
                org.json.JSONObject()
                    .put("content", org.json.JSONObject().put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", text))))
                    .put("finishReason", finish)
            )
        ).toString()

    private fun failure(block: () -> Unit): CloudSidException {
        try {
            block()
        } catch (e: CloudSidException) {
            return e
        }
        fail("expected CloudSidException")
        throw AssertionError()
    }

    @Test
    fun structuredRequestShapeAndReply() {
        responses += ok("{\"a\":1}")
        val r = client().generate("KEY1", "", "hello", 0.5f, "{schema}")
        val s = seen.single()
        assertEquals("/v1beta/models/gemini-flash-latest:generateContent", s.path)
        assertEquals("KEY1", s.key)
        assertTrue(s.body, s.body.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(s.body, s.body.contains("matches this schema"))
        assertTrue(s.body, s.body.contains("{schema}"))
        assertTrue(s.body, s.body.contains("\"thinkingBudget\":0"))
        assertEquals("{\"a\":1}", r.text)
        assertEquals(0, r.finishReason)
    }

    @Test
    fun fencedReplyIsUnwrapped() {
        responses += ok("```json\n{\"a\":1}\n```")
        assertEquals("{\"a\":1}", client().generate("k", "m", "p", 0f, "{}").text)
    }

    @Test
    fun plainRequestHasNoSchemaOrMime() {
        responses += ok("hi")
        client().generate("k", "m", "p", 0f, null)
        assertFalse(seen.single().body.contains("responseMimeType"))
    }

    @Test
    fun maxTokensMapsToOne() {
        responses += ok("x", "MAX_TOKENS")
        assertEquals(1, client().generate("k", "m", "p", 0f, null).finishReason)
    }

    @Test
    fun thinkingRejectedIsRetriedAndRemembered() {
        responses += 400 to "{\"error\":{\"message\":\"Thinking budget not supported\"}}"
        responses += ok("a")
        responses += ok("b")
        assertEquals("a", client().generate("k", "m", "p", 0f, null).text)
        assertEquals(2, seen.size)
        assertTrue(seen[0].body.contains("thinkingConfig"))
        assertFalse(seen[1].body.contains("thinkingConfig"))
        client().generate("k", "m", "p", 0f, null)
        assertFalse(seen[2].body.contains("thinkingConfig"))
    }

    @Test
    fun errorsMapToSidCodes() {
        responses += 429 to "{}"
        assertEquals(CloudSidException.BUSY, failure { client().generate("k", "m", "p", 0f, null) }.code)

        responses += 401 to "{\"error\":{\"message\":\"API key not valid\"}}"
        val e = failure { client().generate("SECRETKEY", "m", "p", 0f, null) }
        assertEquals(CloudSidException.FAILED, e.code)
        assertTrue(e.message, e.message!!.contains("401"))
        assertFalse(e.message, e.message!!.contains("SECRETKEY"))

        responses += 200 to "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"
        assertEquals(CloudSidException.FAILED, failure { client().generate("k", "m", "p", 0f, null) }.code)
    }

    @Test
    fun invalidModelFailsBeforeAnyRequest() {
        assertEquals(CloudSidException.FAILED, failure { client().generate("k", "a/b", "p", 0f, null) }.code)
        assertTrue(seen.isEmpty())
    }
}
