package com.gymmate.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.gymmate.app.data.ApiRepository
import com.gymmate.app.data.AuthStore
import com.gymmate.app.data.Exercise
import com.gymmate.app.data.TrainingPlan
import com.gymmate.app.data.TrendPoint
import com.gymmate.app.data.UserProfile
import com.gymmate.app.data.WorkoutHistoryItem
import com.gymmate.app.ui.screens.ExerciseDetailScreen
import com.gymmate.app.ui.screens.LoginScreen
import com.gymmate.app.ui.screens.ProfileSetupScreen
import com.gymmate.app.ui.screens.TrainingScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class Destination(val route: String, val label: String) {
    data object Login : Destination("login", "登录")
    data object ProfileSetup : Destination("profile_setup", "资料")
    data object Today : Destination("today", "今日")
    data object Explore : Destination("explore", "动作")
    data object History : Destination("history", "训练")
    data object Mine : Destination("mine", "我的")
}

@Composable
fun GymmateApp() {
    val context = LocalContext.current
    val authStore = remember(context) { AuthStore(context) }
    val navController = rememberNavController()
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val tabs = listOf(Destination.Today, Destination.Explore, Destination.History, Destination.Mine)
    var authToken by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        authToken = authStore.session.first().token
        if (authToken.isNotBlank()) {
            runCatching {
                withContext(Dispatchers.IO) { ApiRepository().getMe(authToken) }
            }.onSuccess {
                currentUser = it
                val destination = if (it.heightCm == null || it.fitnessGoal.isNullOrBlank()) {
                    Destination.ProfileSetup.route
                } else {
                    Destination.Today.route
                }
                navController.navigate(destination) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            val current = navController.currentBackStackEntryAsState().value?.destination?.route
            if (current in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        Destination.Today -> Icons.Outlined.Home
                                        Destination.Explore -> Icons.Outlined.Search
                                        Destination.History -> Icons.Outlined.AutoGraph
                                        Destination.Mine -> Icons.Outlined.PersonOutline
                                        else -> Icons.Outlined.Home
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (authToken.isBlank()) Destination.Login.route else Destination.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Login.route) {
                LoginScreen(
                    onLoggedIn = { token, user ->
                        currentUser = user
                        authToken = token
                        scope.launch { authStore.saveToken(token) }
                        val destination = if (user.heightCm == null || user.fitnessGoal.isNullOrBlank()) {
                            Destination.ProfileSetup.route
                        } else {
                            Destination.Today.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Destination.Login.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destination.ProfileSetup.route) {
                val user = currentUser
                if (user != null && authToken.isNotBlank()) {
                    ProfileSetupScreen(
                        token = authToken,
                        user = user,
                        onSaved = {
                            currentUser = it
                            navController.navigate(Destination.Today.route) {
                                popUpTo(Destination.ProfileSetup.route) { inclusive = true }
                            }
                        },
                    )
                }
            }
            composable(Destination.Today.route) {
                TodayScreen(
                    authToken = authToken,
                    currentUser = currentUser,
                    onOpenPlans = { navController.navigate(Destination.Mine.route) },
                    onOpenExercise = { navController.navigate("exercise/${it.id}") },
                    onStartTraining = { navController.navigate("training/${it.id}") },
                )
            }
            composable(Destination.Explore.route) {
                ExploreScreen(
                    onOpenExercise = { navController.navigate("exercise/${it.id}") },
                    onStartTraining = { navController.navigate("training/${it.id}") },
                )
            }
            composable(Destination.History.route) {
                HistoryScreen(authToken = authToken)
            }
            composable(Destination.Mine.route) {
                MineScreen(
                    authToken = authToken,
                    currentUser = currentUser,
                    onOpenPlans = { navController.navigate("plans") },
                )
            }
            composable(
                route = "plans",
            ) {
                PlansScreen(
                    authToken = authToken,
                    currentUser = currentUser,
                    onPlanSelected = { currentUser = it },
                )
            }
            composable(
                route = "exercise/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                ExerciseDetailScreen(
                    exerciseId = entry.arguments?.getInt("id") ?: 0,
                    onBack = { navController.popBackStack() },
                    onStartTraining = { exerciseId ->
                        navController.navigate("training/$exerciseId")
                    },
                )
            }
            composable(
                route = "training/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                TrainingScreen(
                    exerciseId = entry.arguments?.getInt("id") ?: 0,
                    authToken = authToken,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun TodayScreen(
    authToken: String,
    currentUser: UserProfile?,
    onOpenPlans: () -> Unit,
    onOpenExercise: (Exercise) -> Unit,
    onStartTraining: (Exercise) -> Unit,
) {
    val repository = remember { ApiRepository() }
    val exercises = remember { mutableStateListOf<Exercise>() }
    val historyItems = remember { mutableStateListOf<WorkoutHistoryItem>() }
    var currentPlan by remember { mutableStateOf<TrainingPlan?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(authToken) {
        loading = true
        runCatching {
            val exerciseList = withContext(Dispatchers.IO) { repository.getExercises() }
            val safeHistory = if (authToken.isNotBlank()) {
                runCatching { withContext(Dispatchers.IO) { repository.getHistory(authToken) } }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            val plan = if (authToken.isNotBlank()) {
                runCatching { withContext(Dispatchers.IO) { repository.getCurrentPlan(authToken) } }.getOrNull()
            } else {
                null
            }
            Triple(exerciseList, safeHistory, plan)
        }.onSuccess {
            exercises.clear()
            historyItems.clear()
            exercises.addAll(it.first.take(4))
            historyItems.addAll(it.second.take(3))
            currentPlan = it.third
        }
        loading = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    WelcomeHeader(
                        nickname = currentUser?.nickname ?: "训练者",
                        goal = currentUser?.fitnessGoal ?: "先把动作做标准",
                    )
                }
                item {
                    TodayPlanHero(
                        plan = currentPlan,
                        onOpenPlans = onOpenPlans,
                    )
                }
                item {
                    SectionHeader(
                        title = "今日推荐动作",
                        subtitle = "先从 2 到 3 个基础动作开始。",
                    )
                }
                items(exercises) { exercise ->
                    CompactExerciseCard(
                        exercise = exercise,
                        onOpen = { onOpenExercise(exercise) },
                        onTrain = { onStartTraining(exercise) },
                    )
                }
                item {
                    SectionHeader(
                        title = "上次训练反馈",
                        subtitle = "只看最关键的几个信号。",
                    )
                }
                if (historyItems.isEmpty()) {
                    item {
                        EmptyVisualCard(
                            imageRes = AppAssets.emptyHistory,
                            title = "还没有训练记录",
                            body = "今天做完第一组，这里就会开始长出你的反馈和趋势。",
                        )
                    }
                } else {
                    items(historyItems) { item ->
                        HistoryCompactCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader(nickname: String, goal: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("你好，$nickname", style = MaterialTheme.typography.headlineMedium)
        Text(
            goal,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun TodayPlanHero(
    plan: TrainingPlan?,
    onOpenPlans: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = AppAssets.planCover(plan?.goalType ?: "增肌入门"),
                contentDescription = "当前计划",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Text(plan?.name ?: "还没选择训练计划", style = MaterialTheme.typography.headlineMedium)
            Text(
                plan?.description ?: "先选一个计划，让首页真正变成今天该练什么。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusChip(Icons.Outlined.Bolt, plan?.goalType ?: "待选择")
                StatusChip(Icons.Outlined.SportsGymnastics, plan?.level ?: "beginner")
            }
            Button(onClick = onOpenPlans, modifier = Modifier.fillMaxWidth()) {
                Text(if (plan == null) "选择训练计划" else "查看并调整计划")
            }
        }
    }
}

@Composable
private fun ExploreScreen(
    onOpenExercise: (Exercise) -> Unit,
    onStartTraining: (Exercise) -> Unit,
) {
    val repository = remember { ApiRepository() }
    val exercises = remember { mutableStateListOf<Exercise>() }
    val categories = remember { mutableStateListOf<String>() }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun loadCategories() {
        runCatching {
            withContext(Dispatchers.IO) { repository.getExerciseCategories() }
        }.onSuccess {
            categories.clear()
            categories.add("全部")
            categories.addAll(it)
        }
    }

    suspend fun loadExercises() {
        loading = true
        error = null
        runCatching {
            withContext(Dispatchers.IO) {
                repository.getExercises(
                    query = query.ifBlank { null },
                    category = selectedCategory.takeUnless { it == "全部" },
                )
            }
        }.onSuccess {
            exercises.clear()
            exercises.addAll(it)
        }.onFailure {
            error = it.message ?: "加载失败"
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        loadCategories()
        loadExercises()
    }

    LaunchedEffect(query, selectedCategory) {
        delay(250)
        loadExercises()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "动作",
                subtitle = "搜索、筛选、直接开始。",
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索动作、肌群或错误标签") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                categories.forEach { category ->
                    FilterChip(
                        label = category,
                        selected = category == selectedCategory,
                        onClick = { selectedCategory = category },
                    )
                }
            }
        }
        item {
            CategoryPreviewRow(categories = categories.filter { it != "全部" }, selectedCategory = selectedCategory)
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (error != null) {
            item { Text(error.orEmpty(), color = Color(0xFFB95C4B)) }
        } else if (exercises.isEmpty()) {
            item {
                EmptyVisualCard(
                    imageRes = AppAssets.emptySearch,
                    title = "没有找到对应动作",
                    body = "试试换个关键词，或者切回“全部”分类继续找。",
                )
            }
        } else {
            items(exercises) { exercise ->
                CompactExerciseCard(
                    exercise = exercise,
                    onOpen = { onOpenExercise(exercise) },
                    onTrain = { onStartTraining(exercise) },
                )
            }
        }
    }
}

@Composable
private fun CategoryPreviewRow(categories: List<String>, selectedCategory: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        categories.forEach { category ->
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (category == selectedCategory) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
                modifier = Modifier.width(150.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(
                        model = AppAssets.categoryCover(category),
                        contentDescription = category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                    )
                    Text(
                        category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactExerciseCard(
    exercise: Exercise,
    onOpen: () -> Unit,
    onTrain: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = AppAssets.exerciseCover(exercise.name) ?: exercise.mediaUrl,
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    exercise.category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .clickable(onClick = onTrain)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MineScreen(
    authToken: String,
    currentUser: UserProfile?,
    onOpenPlans: () -> Unit,
) {
    val repository = remember { ApiRepository() }
    var currentPlan by remember { mutableStateOf<TrainingPlan?>(null) }

    LaunchedEffect(authToken) {
        if (authToken.isBlank()) return@LaunchedEffect
        currentPlan = runCatching {
            withContext(Dispatchers.IO) { repository.getCurrentPlan(authToken) }
        }.getOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = currentUser?.nickname ?: "我的",
                subtitle = currentUser?.fitnessGoal ?: "先把动作做对",
            )
        }
        item {
            ProfileSummaryCard(currentUser, currentPlan, onOpenPlans)
        }
        item {
            GuideCard(
                title = "推荐机位",
                body = "深蹲用侧前 45 度，推肩和侧平举用正前方，划船与下拉保证上半身完整入镜。",
                imageRes = AppAssets.cameraGuide(CameraGuide.Front45),
            )
        }
        item {
            GuideCard(
                title = "相机与隐私",
                body = "相机仅用于动作识别与骨架计算，不做视频上传。训练时优先保证环境光稳定。",
                imageRes = AppAssets.emptyCameraPermission,
            )
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    currentUser: UserProfile?,
    currentPlan: TrainingPlan?,
    onOpenPlans: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusChip(Icons.Outlined.FitnessCenter, currentPlan?.name ?: "未选择计划")
            Text("身高 ${currentUser?.heightCm ?: "--"} cm · 体重 ${currentUser?.weightKg ?: "--"} kg")
            Text(
                "等级 ${currentUser?.trainingLevel ?: "--"}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Button(onClick = onOpenPlans, modifier = Modifier.fillMaxWidth()) {
                Text("查看训练计划")
            }
        }
    }
}

@Composable
private fun GuideCard(title: String, body: String, imageRes: Int) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = imageRes,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
    }
}

@Composable
private fun StatusChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, color = content, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyVisualCard(imageRes: Int, title: String, body: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = imageRes,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PlansScreen(
    authToken: String,
    currentUser: UserProfile?,
    onPlanSelected: (UserProfile) -> Unit,
) {
    val repository = remember { ApiRepository() }
    val plans = remember { mutableStateListOf<TrainingPlan>() }
    var loading by remember { mutableStateOf(true) }
    var selectingId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { repository.getPlans() }
        }.onSuccess {
            plans.clear()
            plans.addAll(it)
        }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "训练计划",
                subtitle = "先用固定模板建立训练节奏。",
            )
        }
        if (loading) {
            item { CircularProgressIndicator() }
        } else {
            items(plans) { plan ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(
                            model = AppAssets.planCover(plan.goalType),
                            contentDescription = plan.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(172.dp),
                        )
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(plan.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${plan.goalType} · ${plan.level}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(plan.description, style = MaterialTheme.typography.bodyMedium)
                            plan.schedule.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                            val isCurrent = currentUser?.currentPlanId == plan.id
                            Button(
                                onClick = {
                                    if (authToken.isBlank()) return@Button
                                    selectingId = plan.id
                                    CoroutineScope(Dispatchers.Main).launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                repository.selectCurrentPlan(authToken, plan.id)
                                            }
                                        }.onSuccess { onPlanSelected(it) }
                                        selectingId = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = authToken.isNotBlank() && selectingId == null && !isCurrent,
                            ) {
                                Text(
                                    when {
                                        isCurrent -> "当前计划"
                                        selectingId == plan.id -> "设置中..."
                                        else -> "设为当前计划"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(authToken: String) {
    val repository = remember { ApiRepository() }
    val items = remember { mutableStateListOf<WorkoutHistoryItem>() }
    val trendItems = remember { mutableStateListOf<TrendPoint>() }
    var loading by remember { mutableStateOf(true) }
    var days by remember { mutableStateOf(7) }

    suspend fun load() {
        if (authToken.isBlank()) {
            loading = false
            return
        }
        loading = true
        runCatching {
            Pair(
                withContext(Dispatchers.IO) { repository.getHistory(authToken) },
                withContext(Dispatchers.IO) { repository.getTrends(authToken, days) },
            )
        }.onSuccess {
            items.clear()
            trendItems.clear()
            items.addAll(it.first)
            trendItems.addAll(it.second)
        }
        loading = false
    }

    LaunchedEffect(days, authToken) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "训练记录",
                subtitle = "频率、评分、错误趋势。",
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(label = "近 7 天", selected = days == 7) { days = 7 }
                FilterChip(label = "近 30 天", selected = days == 30) { days = 30 }
            }
        }
        if (loading) {
            item { CircularProgressIndicator() }
        } else if (items.isEmpty()) {
            item {
                EmptyVisualCard(
                    imageRes = AppAssets.emptyHistory,
                    title = "还没有训练记录",
                    body = "去做第一组训练，趋势和复盘会在这里出现。",
                )
            }
        } else {
            item {
                TrendCard(points = trendItems, days = days)
            }
            items(items) { item ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = AppAssets.exerciseCover(item.exerciseName),
                            contentDescription = item.exerciseName,
                            modifier = Modifier
                                .size(84.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.exerciseName, style = MaterialTheme.typography.titleLarge)
                            Text("评分 ${item.avgScore} · 次数 ${item.totalReps}", color = MaterialTheme.colorScheme.primary)
                            if (item.errorTags.isNotEmpty()) {
                                Text("主错误：${item.errorTags.first()}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCompactCard(item: WorkoutHistoryItem) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = AppAssets.exerciseCover(item.exerciseName),
                contentDescription = item.exerciseName,
                modifier = Modifier
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.exerciseName, style = MaterialTheme.typography.titleLarge)
                Text("评分 ${item.avgScore} · 次数 ${item.totalReps}", color = MaterialTheme.colorScheme.primary)
                if (item.errorTags.isNotEmpty()) {
                    Text("主错误：${item.errorTags.first()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TrendCard(points: List<TrendPoint>, days: Int) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("近 $days 天趋势", style = MaterialTheme.typography.titleLarge)
            if (points.isEmpty()) {
                Text("暂无趋势数据", style = MaterialTheme.typography.bodyMedium)
            } else {
                val maxScore = (points.maxOfOrNull { it.avgScore } ?: 100).coerceAtLeast(1)
                val maxSessions = (points.maxOfOrNull { it.sessions } ?: 1).coerceAtLeast(1)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = if (points.size > 1) width / (points.size - 1) else width
                    drawRoundRect(
                        color = Color(0x143F6A4B),
                        cornerRadius = CornerRadius(28f, 28f),
                    )
                    repeat(3) { index ->
                        val y = height * (index + 1) / 4f
                        drawLine(
                            color = Color(0x224F6C59),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 2f,
                        )
                    }

                    val scorePath = Path()
                    val scoreOffsets = points.mapIndexed { index, point ->
                        val x = stepX * index
                        val scoreY = height - (point.avgScore / maxScore.toFloat()) * height * 0.82f - 8f
                        val barHeight = (point.sessions / maxSessions.toFloat()) * height * 0.34f
                        drawRoundRect(
                            color = Color(0x3364A674),
                            topLeft = Offset(x - 14f, height - barHeight),
                            size = androidx.compose.ui.geometry.Size(28f, barHeight),
                            cornerRadius = CornerRadius(18f, 18f),
                        )
                        if (index == 0) scorePath.moveTo(x, scoreY) else scorePath.lineTo(x, scoreY)
                        Offset(x, scoreY)
                    }
                    drawPath(
                        path = scorePath,
                        color = Color(0xFF7ACB8C),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7f, cap = StrokeCap.Round),
                    )
                    scoreOffsets.forEach { offset ->
                        drawCircle(color = Color(0xFF7ACB8C), radius = 8f, center = offset)
                        drawCircle(color = Color.White, radius = 4f, center = offset)
                    }
                }
                Text(
                    "最近平均评分：${points.lastOrNull()?.avgScore ?: 0} · 最近训练次数：${points.sumOf { it.sessions }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
    }
}
