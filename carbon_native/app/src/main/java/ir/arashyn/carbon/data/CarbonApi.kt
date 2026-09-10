package ir.arashyn.carbon.data

import ir.arashyn.carbon.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class CarbonApi(private val sessionStore: SessionStore) {
    companion object {
        const val ORIGIN = "https://fluxer.arashyn.ir"
        const val API = "$ORIGIN/api"
        const val GATEWAY = "wss://fluxer.arashyn.ir/gateway?v=1&encoding=json"
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun login(email: String, password: String): LoginOutcome = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val json = execute("POST", "/auth/login", body, authenticated = false)
        if (json.optBoolean("mfa")) return@withContext LoginOutcome.Mfa(MfaRequired(json.getString("ticket")))
        val userJson = json.getJSONObject("user")
        val result = LoginResult(json.getString("token"), parseUser(userJson))
        sessionStore.token = result.token
        LoginOutcome.Success(result)
    }

    suspend fun loginTotp(ticket: String, code: String): String = withContext(Dispatchers.IO) {
        val json = execute("POST", "/auth/login/mfa/totp", JSONObject().put("ticket", ticket).put("code", code), false)
        json.getString("token").also { sessionStore.token = it }
    }

    suspend fun me(): CarbonUser = withContext(Dispatchers.IO) { parseUser(execute("GET", "/users/@me")) }

    suspend fun guilds(): List<Guild> = withContext(Dispatchers.IO) {
        parseArray(executeArray("GET", "/users/@me/guilds?limit=200")) { item ->
            Guild(item.getString("id"), item.optString("name", "Server"), item.optNullable("icon"))
        }
    }

    suspend fun channels(guildId: String): List<Channel> = withContext(Dispatchers.IO) {
        parseArray(executeArray("GET", "/guilds/$guildId/channels")) { item ->
            Channel(item.getString("id"), item.optNullable("guild_id"), item.optString("name", "channel"), item.optInt("type"), item.optInt("position"))
        }.filter { it.type == 0 || it.type == 5 }.sortedBy { it.position }
    }

    suspend fun messages(channelId: String): List<Message> = withContext(Dispatchers.IO) {
        parseArray(executeArray("GET", "/channels/$channelId/messages?limit=50")) { parseMessage(it) }.reversed()
    }

    suspend fun sendMessage(channelId: String, content: String): Message = withContext(Dispatchers.IO) {
        val body = JSONObject().put("content", content).put("nonce", UUID.randomUUID().toString())
        parseMessage(execute("POST", "/channels/$channelId/messages", body))
    }

    private fun request(method: String, path: String, body: JSONObject?, authenticated: Boolean): Request {
        val builder = Request.Builder().url(API + path).header("Accept", "application/json")
        if (authenticated) sessionStore.token?.let { builder.header("Authorization", it) }
        val requestBody = body?.toString()?.toRequestBody(jsonType)
        return when (method) {
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody()).build()
            else -> builder.get().build()
        }
    }

    private fun execute(method: String, path: String, body: JSONObject? = null, authenticated: Boolean = true): JSONObject {
        client.newCall(request(method, path, body, authenticated)).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull().orEmpty()
                throw IOException(if (message.isBlank()) "Server error ${response.code}" else message)
            }
            return JSONObject(text)
        }
    }

    private fun executeArray(method: String, path: String): JSONArray {
        client.newCall(request(method, path, null, true)).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Server error ${response.code}")
            return JSONArray(text)
        }
    }

    private fun parseMessage(json: JSONObject): Message = Message(
        id = json.getString("id"), channelId = json.getString("channel_id"),
        content = json.optString("content"), author = parseUser(json.getJSONObject("author")),
        timestamp = json.optString("timestamp"),
    )

    private fun parseUser(json: JSONObject) = CarbonUser(
        id = json.getString("id"), username = json.optString("username", "user"),
        displayName = json.optNullable("global_name") ?: json.optString("username", "user"),
        avatar = json.optNullable("avatar"),
    )

    private fun <T> parseArray(array: JSONArray, transform: (JSONObject) -> T): List<T> =
        (0 until array.length()).map { transform(array.getJSONObject(it)) }

    private fun JSONObject.optNullable(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
