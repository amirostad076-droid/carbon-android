package ir.arashyn.carbon.model

data class CarbonUser(val id: String, val username: String, val displayName: String, val avatar: String?)
data class Guild(val id: String, val name: String, val icon: String?)
data class Channel(val id: String, val guildId: String?, val name: String, val type: Int, val position: Int)
data class Message(val id: String, val channelId: String, val content: String, val author: CarbonUser, val timestamp: String)
data class LoginResult(val token: String, val user: CarbonUser)
data class MfaRequired(val ticket: String)

sealed interface LoginOutcome {
    data class Success(val result: LoginResult) : LoginOutcome
    data class Mfa(val challenge: MfaRequired) : LoginOutcome
}
