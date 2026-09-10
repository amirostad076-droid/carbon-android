package ir.arashyn.carbon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.arashyn.carbon.data.CarbonApi
import ir.arashyn.carbon.data.GatewayClient
import ir.arashyn.carbon.data.SessionStore
import ir.arashyn.carbon.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CarbonState(
    val loading: Boolean = false,
    val authenticated: Boolean = false,
    val user: CarbonUser? = null,
    val guilds: List<Guild> = emptyList(),
    val selectedGuild: Guild? = null,
    val channels: List<Channel> = emptyList(),
    val selectedChannel: Channel? = null,
    val messages: List<Message> = emptyList(),
    val mfaTicket: String? = null,
    val error: String? = null,
)

class CarbonViewModel(application: Application) : AndroidViewModel(application) {
    private val session = SessionStore(application)
    private val api = CarbonApi(session)
    private val gateway = GatewayClient(api) { session.token }
    private val _state = MutableStateFlow(CarbonState())
    val state = _state.asStateFlow()

    init {
        gateway.onDispatch = { event, data ->
            if (event == "MESSAGE_CREATE") {
                val channelId = data.optString("channel_id")
                if (channelId == _state.value.selectedChannel?.id) refreshMessages()
            }
        }
        if (session.token != null) restoreSession()
    }

    fun login(email: String, password: String) = launchLoading {
        when (val outcome = api.login(email, password)) {
            is LoginOutcome.Success -> openSession(outcome.result.user)
            is LoginOutcome.Mfa -> _state.value = _state.value.copy(mfaTicket = outcome.challenge.ticket)
        }
    }

    fun submitMfa(code: String) = launchLoading {
        val ticket = _state.value.mfaTicket ?: return@launchLoading
        api.loginTotp(ticket, code)
        openSession(api.me())
    }

    private fun restoreSession() = launchLoading { openSession(api.me()) }

    private suspend fun openSession(user: CarbonUser) {
        val guilds = api.guilds()
        _state.value = _state.value.copy(authenticated = true, user = user, guilds = guilds, mfaTicket = null)
        gateway.connect()
        guilds.firstOrNull()?.let { selectGuildInternal(it) }
    }

    fun selectGuild(guild: Guild) = launchLoading { selectGuildInternal(guild) }

    private suspend fun selectGuildInternal(guild: Guild) {
        val channels = api.channels(guild.id)
        _state.value = _state.value.copy(selectedGuild = guild, channels = channels, selectedChannel = null, messages = emptyList())
        channels.firstOrNull()?.let { selectChannelInternal(it) }
    }

    fun selectChannel(channel: Channel) = launchLoading { selectChannelInternal(channel) }

    private suspend fun selectChannelInternal(channel: Channel) {
        _state.value = _state.value.copy(selectedChannel = channel, messages = api.messages(channel.id))
    }

    fun sendMessage(content: String) {
        val channel = _state.value.selectedChannel ?: return
        if (content.isBlank()) return
        launchLoading {
            val created = api.sendMessage(channel.id, content.trim())
            _state.value = _state.value.copy(messages = _state.value.messages + created)
        }
    }

    fun refreshMessages() {
        val channel = _state.value.selectedChannel ?: return
        viewModelScope.launch {
            runCatching { api.messages(channel.id) }.onSuccess { _state.value = _state.value.copy(messages = it) }
        }
    }

    fun logout() {
        gateway.disconnect(); session.clear(); _state.value = CarbonState()
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    private fun launchLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { block() }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "خطای نامشخص") }
            _state.value = _state.value.copy(loading = false)
        }
    }

    override fun onCleared() { gateway.disconnect(); super.onCleared() }
}
