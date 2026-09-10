package ir.arashyn.carbon

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.arashyn.carbon.model.*
import kotlinx.coroutines.delay

private val Navy = Color(0xFF071426)
private val SurfaceColor = Color(0xFF101F33)
private val SurfaceHighColor = Color(0xFF172A42)
private val Cyan = Color(0xFF1ECDF1)
private val Mint = Color(0xFF62E6C5)

@Composable
fun CarbonApplication(viewModel: CarbonViewModel) {
    val state by viewModel.state.collectAsState()
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1200); splash = false }

    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, secondary = Cyan, background = Navy, surface = SurfaceColor)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
            when {
                splash -> SplashScreen()
                !state.authenticated -> LoginScreen(state, viewModel::login, viewModel::submitMfa)
                else -> ChatScreen(state, viewModel)
            }
            state.error?.let { ErrorDialog(it, viewModel::clearError) }
            if (state.loading) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .32f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mint)
            }
        }
    }
}

@Composable
private fun SplashScreen() = Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
    Image(painterResource(R.drawable.carbon_brand), "CARBON", Modifier.fillMaxWidth(.86f), contentScale = ContentScale.Fit)
}

@Composable
private fun LoginScreen(state: CarbonState, onLogin: (String, String) -> Unit, onMfa: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.carbon_brand), "CARBON", Modifier.fillMaxWidth().height(230.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(16.dp))
        if (state.mfaTicket == null) {
            OutlinedTextField(email, { email = it }, label = { Text("ایمیل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(password, { password = it }, label = { Text("رمز عبور") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Button({ onLogin(email, password) }, Modifier.fillMaxWidth().height(52.dp), enabled = email.isNotBlank() && password.isNotBlank()) { Text("ورود به CARBON") }
        } else {
            Text("کد تأیید دومرحله‌ای را وارد کنید", color = Color.White)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(code, { code = it.filter(Char::isDigit).take(8) }, label = { Text("کد MFA") }, singleLine = true)
            Spacer(Modifier.height(20.dp))
            Button({ onMfa(code) }, enabled = code.length >= 6) { Text("تأیید") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(state: CarbonState, vm: CarbonViewModel) {
    var drawerOpen by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(state.selectedChannel?.name?.let { "# $it" } ?: "CARBON", fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton({ drawerOpen = !drawerOpen }) { Text("☰", fontSize = 24.sp) } },
            actions = { TextButton(vm::logout) { Text("خروج") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor),
        )
        Row(Modifier.weight(1f)) {
            if (drawerOpen) NavigationPane(state, vm, Modifier.width(280.dp).fillMaxHeight())
            MessagePane(state.messages, Modifier.weight(1f))
        }
        if (state.selectedChannel != null) Row(Modifier.fillMaxWidth().background(SurfaceColor).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, placeholder = { Text("پیام بنویسید…") }, modifier = Modifier.weight(1f), maxLines = 4)
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { vm.sendMessage(text); text = "" }, enabled = text.isNotBlank()) { Text("➤") }
        }
    }
}

@Composable
private fun NavigationPane(state: CarbonState, vm: CarbonViewModel, modifier: Modifier = Modifier) {
    Row(modifier.background(Color(0xFF09182A))) {
        LazyColumn(Modifier.width(72.dp).fillMaxHeight().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            items(state.guilds, key = { it.id }) { guild ->
                Box(Modifier.padding(6.dp).size(52.dp).clip(CircleShape).background(if (guild.id == state.selectedGuild?.id) Mint else SurfaceHighColor)
                    .clickable { vm.selectGuild(guild) }, contentAlignment = Alignment.Center) {
                    Text(guild.name.take(2).uppercase(), color = if (guild.id == state.selectedGuild?.id) Navy else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight().background(SurfaceColor).padding(8.dp)) {
            item { Text(state.selectedGuild?.name ?: "کانال‌ها", Modifier.padding(10.dp), color = Color.White, fontWeight = FontWeight.Bold) }
            items(state.channels, key = { it.id }) { channel ->
                Text("#  ${channel.name}", Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (channel.id == state.selectedChannel?.id) SurfaceHighColor else Color.Transparent)
                    .clickable { vm.selectChannel(channel) }.padding(12.dp), color = Color(0xFFD7E5F3))
            }
        }
    }
}

@Composable
private fun MessagePane(messages: List<Message>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 12.dp), reverseLayout = true) {
        items(messages.reversed(), key = { it.id }) { message ->
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(message.author.displayName, color = Mint, fontWeight = FontWeight.Bold)
                if (message.content.isNotBlank()) Text(message.content, color = Color(0xFFE8F1FA), lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun ErrorDialog(message: String, dismiss: () -> Unit) = AlertDialog(
    onDismissRequest = dismiss, confirmButton = { TextButton(dismiss) { Text("باشه") } },
    title = { Text("خطا") }, text = { Text(message) },
)
