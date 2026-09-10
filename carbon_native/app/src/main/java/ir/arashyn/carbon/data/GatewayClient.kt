package ir.arashyn.carbon.data

import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class GatewayClient(private val api: CarbonApi, private val token: () -> String?) {
    private var socket: WebSocket? = null
    private var heartbeat: Timer? = null
    private var sequence: Long? = null
    var onDispatch: ((String, JSONObject) -> Unit)? = null

    fun connect() {
        disconnect()
        socket = api.client.newWebSocket(Request.Builder().url(CarbonApi.GATEWAY).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val payload = JSONObject(text)
                if (!payload.isNull("s")) sequence = payload.optLong("s")
                when (payload.optInt("op")) {
                    10 -> {
                        val interval = payload.getJSONObject("d").getLong("heartbeat_interval")
                        identify(webSocket)
                        heartbeat?.cancel()
                        heartbeat = fixedRateTimer("carbon-heartbeat", daemon = true, initialDelay = interval, period = interval) {
                            webSocket.send(JSONObject().put("op", 1).put("d", sequence ?: JSONObject.NULL).toString())
                        }
                    }
                    0 -> onDispatch?.invoke(payload.optString("t"), payload.optJSONObject("d") ?: JSONObject())
                    1 -> webSocket.send(JSONObject().put("op", 1).put("d", sequence ?: JSONObject.NULL).toString())
                    7 -> connect()
                }
            }
        })
    }

    private fun identify(webSocket: WebSocket) {
        val properties = JSONObject().put("os", "android").put("browser", "carbon-native").put("device", "carbon-native")
        val data = JSONObject().put("token", token()).put("properties", properties).put("flags", 0)
        webSocket.send(JSONObject().put("op", 2).put("d", data).toString())
    }

    fun disconnect() {
        heartbeat?.cancel(); heartbeat = null
        socket?.close(1000, "Client closed"); socket = null
    }
}
