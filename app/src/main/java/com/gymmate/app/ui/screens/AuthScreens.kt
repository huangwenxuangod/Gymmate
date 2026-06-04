package com.gymmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gymmate.app.BuildConfig
import com.gymmate.app.data.ApiRepository
import com.gymmate.app.data.UserProfile
import com.gymmate.app.ui.AppAssets
import retrofit2.HttpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    onLoggedIn: (token: String, user: UserProfile) -> Unit,
) {
    val repository = remember { ApiRepository() }
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var devCode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val apiBaseUrl = BuildConfig.API_BASE_URL
    val buildLabel = "v${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})"

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AuthHeroCard(
                    imageRes = AppAssets.loginHero,
                    title = "Gymmate",
                    subtitle = "极简训练助手。先登录，再把今天该练什么变得明确。",
                )
            }
            item {
                HintCard(
                    title = "当前连接",
                    body = "$apiBaseUrl\n$buildLabel",
                )
            }
            item {
                FormCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("手机号登录", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("手机号") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("验证码") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { repository.sendCode(phone) }
                                }.onSuccess {
                                    devCode = it.devCode
                                }.onFailure {
                                    error = formatRequestError("验证码发送失败", it)
                                }
                                loading = false
                            }
                        },
                        enabled = phone.length == 11 && !loading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("获取验证码")
                    }
                    Button(
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { repository.login(phone, code) }
                                }.onSuccess {
                                    onLoggedIn(it.token, it.user)
                                }.onFailure {
                                    error = formatRequestError("登录失败", it)
                                }
                                loading = false
                            }
                        },
                        enabled = phone.length == 11 && code.length == 6 && !loading,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (loading) CircularProgressIndicator() else Text("进入")
                    }
                }
            }
            if (devCode != null) {
                item {
                    HintCard("开发模式验证码", devCode.orEmpty())
                }
            }
            if (error != null) {
                item {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun formatRequestError(prefix: String, throwable: Throwable): String {
    val httpCode = (throwable as? HttpException)?.code()
    val detail = throwable.message?.takeIf { it.isNotBlank() }
    return buildString {
        append(prefix)
        if (httpCode != null) append(" · HTTP $httpCode")
        if (detail != null) append("\n").append(detail)
    }
}

@Composable
fun ProfileSetupScreen(
    token: String,
    user: UserProfile,
    onSaved: (UserProfile) -> Unit,
) {
    val repository = remember { ApiRepository() }
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    var nickname by rememberSaveable { mutableStateOf(user.nickname) }
    var gender by rememberSaveable { mutableStateOf(user.gender ?: "男") }
    var height by rememberSaveable { mutableStateOf(user.heightCm?.toString().orEmpty()) }
    var weight by rememberSaveable { mutableStateOf(user.weightKg?.toString().orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(user.fitnessGoal ?: "增肌入门") }
    var level by rememberSaveable { mutableStateOf(user.trainingLevel ?: "beginner") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("完善资料", style = MaterialTheme.typography.headlineSmall)
            Text(
                "信息都放在首屏，不需要下滑。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )

            FormCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("昵称") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.weight(1f),
                            label = { Text("身高") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.weight(1f),
                            label = { Text("体重") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                        )
                    }
                    CompactOptionGroup(
                        title = "目标",
                        options = listOf("增肌入门", "减脂塑形", "基础力量提升"),
                        selected = goal,
                        onSelected = { goal = it },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactOptionGroup(
                            title = "等级",
                            options = listOf("beginner", "intermediate"),
                            labels = mapOf("beginner" to "新手", "intermediate" to "轻进阶"),
                            selected = level,
                            modifier = Modifier.weight(1f),
                            onSelected = { level = it },
                        )
                        CompactOptionGroup(
                            title = "性别",
                            options = listOf("男", "女"),
                            selected = gender,
                            modifier = Modifier.weight(1f),
                            onSelected = { gender = it },
                        )
                    }
                }
            }

            Text(
                "当前：$goal · ${if (level == "beginner") "新手" else "轻进阶"} · $gender",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )

            if (error != null) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                repository.updateMe(
                                    token = token,
                                    nickname = nickname.ifBlank { user.nickname },
                                    gender = gender,
                                    heightCm = height.toIntOrNull(),
                                    weightKg = weight.toIntOrNull(),
                                    fitnessGoal = goal,
                                    trainingLevel = level,
                                )
                            }
                        }.onSuccess {
                            onSaved(it)
                        }.onFailure {
                            error = it.message ?: "保存失败，请重试"
                        }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Text("保存并进入今日训练")
                }
            }
        }
    }
}

@Composable
private fun AuthHeroCard(imageRes: Int, title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(
                model = imageRes,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun FormCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun HintCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun OptionGroup(
    title: String,
    options: List<String>,
    selected: String,
    labels: Map<String, String> = emptyMap(),
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { option ->
                val active = option == selected
                Text(
                    text = labels[option] ?: option,
                    modifier = Modifier
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(999.dp),
                        )
                        .clickable { onSelected(option) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun CompactOptionGroup(
    title: String,
    options: List<String>,
    selected: String,
    labels: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val active = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelected(option) }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labels[option] ?: option,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
