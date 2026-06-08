package com.example.moodit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moodit.analysis.AiAnalysisClient
import com.example.moodit.analysis.MoodAnalysisEngine
import com.example.moodit.data.ExpenseCategory
import com.example.moodit.data.ExpenseRecord
import com.example.moodit.data.ExpenseSubCategory
import com.example.moodit.data.MoodType
import com.example.moodit.data.MooditDatabase
import com.example.moodit.data.defaultSubCategory
import com.example.moodit.data.toEntity
import com.example.moodit.data.toRecord
import com.example.moodit.ui.theme.MooditTheme
import kotlinx.coroutines.launch
import java.util.Calendar

private val MooditPurple = Color(0xFF7A5CE6)
private val MooditPurpleSoft = Color(0xFFF0EBFF)
private val MooditBackground = Color(0xFFF9F8FD)
private const val MonthlyGoalAmount = 400000

private data class TabItem(
    val title: String,
    val icon: String
)

private data class DailyTotal(
    val label: String,
    val amount: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = MooditDatabase.getDatabase(this)

        setContent {
            MooditTheme {
                MooditApp(database)
            }
        }
    }
}

@Composable
fun MooditApp(database: MooditDatabase) {
    val dao = remember(database) { database.expenseDao() }
    val scope = rememberCoroutineScope()
    val entities by dao.getAllExpenses().collectAsState(initial = emptyList())
    val records = entities.map { it.toRecord() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var aiReport by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MooditBackground,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val tabs = listOf(
                    TabItem("홈", "⌂"),
                    TabItem("기록", "✎"),
                    TabItem("분석", "⌕"),
                    TabItem("통계", "▦"),
                    TabItem("마이", "☺")
                )

                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MooditPurple,
                            selectedTextColor = MooditPurple,
                            unselectedIconColor = Color(0xFF888888),
                            unselectedTextColor = Color(0xFF888888)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MooditBackground)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(records)
                1 -> RecordScreen { record ->
                    scope.launch {
                        dao.insertExpense(record.toEntity())
                        selectedTab = 0
                    }
                }
                2 -> AnalysisScreen(
                    records = records,
                    aiReport = aiReport,
                    aiLoading = aiLoading,
                    onAiAnalyze = {
                        scope.launch {
                            aiLoading = true
                            aiReport = AiAnalysisClient.analyze(records).getOrElse { error ->
                                "AI 분석 서버에 연결하지 못했어요.\n\n${error.message ?: "서버 상태를 확인해주세요."}"
                            }
                            aiLoading = false
                        }
                    },
                    onDelete = { record ->
                        scope.launch {
                            dao.deleteExpenseById(record.id)
                        }
                    }
                )
                3 -> StatisticsScreen(records)
                4 -> MyPageScreen(
                    records = records,
                    onImportTossHistory = {
                        scope.launch {
                            tossSampleRecords().forEach { record ->
                                dao.insertExpense(record.toEntity())
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(records: List<ExpenseRecord>) {
    val result = MoodAnalysisEngine.analyzeMonthly(records, 31)
    var selectedMood by remember { mutableStateOf(MoodType.HAPPY) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Moodit",
                style = MaterialTheme.typography.headlineLarge,
                color = MooditPurple,
                fontWeight = FontWeight.Bold
            )
            Text("안녕하세요, sunny님", color = Color(0xFF777777))
        }

        item {
            Text(
                text = "오늘 기분은 어떤가요?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            MoodChipRow(
                selectedMood = selectedMood,
                onSelected = { selectedMood = it }
            )
        }

        item {
            AppCard {
                Text("오늘도 기록해주셔서 고마워요", color = MooditPurple, fontWeight = FontWeight.SemiBold)
                Text("소비 패턴을 함께 분석해볼게요.")
            }
        }

        item {
            SummaryCard(
                title = "이번 달 지출",
                value = "${result.totalAmount}원",
                subtitle = "감정 소비 비율 ${result.emotionalSpendingRate}%"
            )
        }

        item {
            TrendCard(
                title = "지출 추이 (고정 지출 제외)",
                data = buildDailyTotalsWithDates(records)
            )
        }

        item {
            DonutSummaryCard(
                title = "카테고리별 지출 (고정 지출 제외)",
                categoryTotals = categoryTotalsWithoutFixed(records)
            )
        }

        item {
            FixedExpenseSummaryCard(records)
        }

        val fixedEntries = fixedExpenseTotals(records).entries.toList()
        if (fixedEntries.isNotEmpty()) {
            item {
                SectionTitle("고정 지출 내역")
            }

            items(fixedEntries) { entry ->
                ProgressCard(
                    title = "${entry.key.emoji} ${entry.key.label}",
                    amount = entry.value,
                    total = fixedExpenseAmount(records)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(onSave: (ExpenseRecord) -> Unit) {
    val focusManager = LocalFocusManager.current
    var amountText by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(todayDateText()) }
    var timeText by remember { mutableStateOf(currentTimeText()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateTextToMillis(dateText)
    )
    var fixedExpenseMode by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var selectedSubCategory by remember { mutableStateOf(defaultSubCategory(ExpenseCategory.FOOD)) }
    var selectedMood by remember { mutableStateOf(MoodType.HAPPY) }
    val fixedExpense = fixedExpenseMode || isFixedExpense(selectedSubCategory)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            dateText = millisToDateText(selectedMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            ScreenTitle("소비 기록")
        }

        item {
            SectionTitle("기록 종류")
            RecordTypeChipRow(
                fixedExpenseMode = fixedExpenseMode,
                onModeChanged = { fixedMode ->
                    fixedExpenseMode = fixedMode
                    if (fixedMode) {
                        selectedSubCategory = ExpenseSubCategory.LIVING_RENT
                        selectedCategory = selectedSubCategory.parentCategory
                    } else {
                        selectedCategory = ExpenseCategory.FOOD
                        selectedSubCategory = defaultSubCategory(ExpenseCategory.FOOD)
                    }
                }
            )
        }

        item {
            AppCard {
                Text("금액", fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (amountText.isBlank()) "0원" else "${amountText}원",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MooditPurple,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() } },
                    label = { Text("금액") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnTab(focusManager)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { input -> dateText = input.filter { it.isDigit() }.take(8) },
                        label = { Text("날짜 (예: 20260608)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .moveFocusOnTab(focusManager)
                    )
                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MooditPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("날짜 선택")
                    }
                }
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { input -> timeText = input.filter { it.isDigit() }.take(4) },
                    label = { Text("시간 (예: 1430)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnTab(focusManager)
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnTab(focusManager, clearOnTab = true)
                )
            }
        }

        item {
            if (fixedExpenseMode) {
                SectionTitle("고정 지출")
                FixedExpenseChipGrid(
                    selectedSubCategory = selectedSubCategory,
                    onSelected = { subCategory ->
                        selectedSubCategory = subCategory
                        selectedCategory = subCategory.parentCategory
                    }
                )
            } else {
                SectionTitle("카테고리")
                CategoryChipGrid(
                    selectedCategory = selectedCategory,
                    onSelected = { category ->
                        selectedCategory = category
                        selectedSubCategory = defaultSubCategory(category)
                    }
                )
            }
        }

        if (!fixedExpenseMode) {
            item {
                SectionTitle("세부 카테고리")
                SubCategoryChipGrid(
                    category = selectedCategory,
                    selectedSubCategory = selectedSubCategory,
                    onSelected = { selectedSubCategory = it }
                )
            }
        }

        item {
            SectionTitle("감정")
            if (fixedExpense) {
                AppCard {
                    Text("고정 지출", color = MooditPurple, fontWeight = FontWeight.SemiBold)
                    Text("월세, 통신비, 세금 같은 고정 지출은 감정 분석에서 제외돼요.")
                }
            } else {
                MoodChipRow(
                    selectedMood = selectedMood,
                    onSelected = { selectedMood = it }
                )
            }
        }

        item {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    if (amount > 0) {
                        val now = System.currentTimeMillis()
                        val selectedDateTime = dateTimeTextToMillis(dateText, timeText) ?: now
                        onSave(
                            ExpenseRecord(
                                now,
                                amount,
                                selectedCategory,
                                selectedSubCategory,
                                if (fixedExpense) MoodType.NORMAL else selectedMood,
                                memo,
                                selectedDateTime
                            )
                        )

                        amountText = ""
                        memo = ""
                        dateText = todayDateText()
                        timeText = currentTimeText()
                        selectedCategory = ExpenseCategory.FOOD
                        selectedSubCategory = defaultSubCategory(ExpenseCategory.FOOD)
                        selectedMood = MoodType.HAPPY
                        fixedExpenseMode = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MooditPurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("저장하기")
            }
        }
    }
}
@Composable
fun AnalysisScreen(
    records: List<ExpenseRecord>,
    aiReport: String?,
    aiLoading: Boolean,
    onAiAnalyze: () -> Unit,
    onDelete: (ExpenseRecord) -> Unit
) {
    val result = MoodAnalysisEngine.analyzeMonthly(records, 31)
    var selectedDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            ScreenTitle("감정 분석")
        }

        item {
            MonthHeader("2026년 6월")
        }

        item {
            CalendarCard(
                records = records,
                selectedDay = selectedDay,
                onDaySelected = { selectedDay = it }
            )
        }

        item {
            DayExpenseList(
                selectedDay = selectedDay,
                records = records.filter { dayOfMonth(it.createdAt) == selectedDay },
                onDelete = onDelete
            )
        }

        item {
            SummaryCard(
                title = "위험 시간대",
                value = riskyTimePeriod(records),
                subtitle = regretWarningMessage(records)
            )
        }

        item {
            SectionTitle("AI 분석 리포트")
        }

        item {
            Button(
                onClick = onAiAnalyze,
                enabled = !aiLoading && records.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MooditPurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (aiLoading) "AI 분석 중..." else "AI 분석하기")
            }
        }

        item {
            AppCard {
                Text("분석 리포트", color = MooditPurple, fontWeight = FontWeight.SemiBold)
                Text(aiReport ?: result.reportMessages.joinToString("\n\n"))
            }
        }
    }
}

@Composable
fun StatisticsScreen(records: List<ExpenseRecord>) {
    val result = MoodAnalysisEngine.analyzeMonthly(records, 31)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            ScreenTitle("통계")
        }

        item {
            TrendCard(
                title = "지출 추이 (고정 지출 제외)",
                data = buildDailyTotalsWithDates(records)
            )
        }

        item {
            DonutSummaryCard(
                title = "카테고리별 지출 (고정 지출 제외)",
                categoryTotals = categoryTotalsWithoutFixed(records)
            )
        }

        item {
            FixedExpenseSummaryCard(records)
        }

        val fixedEntries = fixedExpenseTotals(records).entries.toList()
        if (fixedEntries.isNotEmpty()) {
            item {
                SectionTitle("고정 지출 내역")
            }

            items(fixedEntries) { entry ->
                ProgressCard(
                    title = "${entry.key.emoji} ${entry.key.label}",
                    amount = entry.value,
                    total = fixedExpenseAmount(records)
                )
            }
        }

        item {
            SummaryCard(
                title = "시간대별 소비 패턴",
                value = topSpendingTimePeriod(records),
                subtitle = "중간보고서 기준 시간대 분석"
            )
        }

        item {
            SectionTitle("감정별 지출")
        }

        items(result.moodTotal.entries.toList()) { entry ->
            ProgressCard(
                title = entry.key.label,
                amount = entry.value,
                total = result.totalAmount
            )
        }
    }
}

@Composable
fun MyPageScreen(
    records: List<ExpenseRecord>,
    onImportTossHistory: () -> Unit
) {
    val goalAmount = MonthlyGoalAmount
    val controllableAmount = controllableSpendingAmount(records)
    val goalRate = if (goalAmount == 0) 0 else controllableAmount * 100 / goalAmount
    val moodTemperature = moodTemperature(records)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            ScreenTitle("마이페이지")
        }

        item {
            AppCard {
                Text("sunny", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Moodit와 함께한 지 12일째", color = Color(0xFF777777))
            }
        }

        item {
            Button(
                onClick = onImportTossHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MooditPurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("토스 내역 넣기")
            }
        }

        item {
            SummaryCard(
                title = "감정 소비 온도계",
                value = "%.1f℃".format(moodTemperature),
                subtitle = "기본 36.5℃ / 행복은 따뜻하게, 우울·스트레스는 차갑게 반영"
            )
        }

        item {
            ProgressCard(
                title = "이번 달 목표 (고정 지출 제외)",
                amount = controllableAmount,
                total = goalAmount
            )
        }

        item {
            SummaryCard(
                title = "목표 사용률",
                value = "$goalRate%",
                subtitle = "고정 지출 제외 / 목표 금액 ${goalAmount}원"
            )
        }
    }
}

@Composable
private fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MooditPurple,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

private fun Modifier.moveFocusOnTab(
    focusManager: androidx.compose.ui.focus.FocusManager,
    clearOnTab: Boolean = false
): Modifier {
    return onPreviewKeyEvent { event ->
        if (event.key == Key.Tab && event.type == KeyEventType.KeyUp) {
            if (clearOnTab) {
                focusManager.clearFocus()
            } else {
                focusManager.moveFocus(FocusDirection.Down)
            }
            true
        } else {
            false
        }
    }
}

@Composable
private fun AppCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun SummaryCard(title: String, value: String, subtitle: String) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MooditPurpleSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF555555))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF8A6EAA))
            }
            MiniTrendLine()
        }
    }
}

@Composable
private fun MoodChipRow(selectedMood: MoodType, onSelected: (MoodType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MoodType.values().forEach { mood ->
            val selected = selectedMood == mood

            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .height(86.dp)
                    .clickable { onSelected(mood) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (selected) MooditPurpleSoft else Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = mood.emoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = mood.label,
                        color = if (selected) MooditPurple else Color(0xFF666666),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordTypeChipRow(
    fixedExpenseMode: Boolean,
    onModeChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !fixedExpenseMode,
            onClick = { onModeChanged(false) },
            label = { Text("일반 소비") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = fixedExpenseMode,
            onClick = { onModeChanged(true) },
            label = { Text("고정 지출") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryChipGrid(
    selectedCategory: ExpenseCategory,
    onSelected: (ExpenseCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpenseCategory.values().toList().chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { category ->
                    CategoryCard(
                        selected = selectedCategory == category,
                        onClick = { onSelected(category) },
                        emoji = category.emoji,
                        label = category.label,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubCategoryChipGrid(
    category: ExpenseCategory,
    selectedSubCategory: ExpenseSubCategory,
    onSelected: (ExpenseSubCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpenseSubCategory.values()
            .filter { it.parentCategory == category && it != ExpenseSubCategory.NONE }
            .chunked(3)
            .forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { subCategory ->
                        CategoryCard(
                            selected = selectedSubCategory == subCategory,
                            onClick = { onSelected(subCategory) },
                            emoji = subCategory.emoji,
                            label = subCategory.label,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
    }
}

@Composable
private fun FixedExpenseChipGrid(
    selectedSubCategory: ExpenseSubCategory,
    onSelected: (ExpenseSubCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fixedExpenseSubCategories().chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { subCategory ->
                    CategoryCard(
                        selected = selectedSubCategory == subCategory,
                        onClick = { onSelected(subCategory) },
                        emoji = subCategory.emoji,
                        label = subCategory.label,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    selected: Boolean,
    onClick: () -> Unit,
    emoji: String,
    label: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .height(78.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MooditPurpleSoft else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = label,
                color = if (selected) MooditPurple else Color(0xFF666666),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun MonthHeader(month: String) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("<", style = MaterialTheme.typography.headlineSmall)
            Text(month, fontWeight = FontWeight.SemiBold)
            Text(">", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun CalendarCard(
    records: List<ExpenseRecord>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val highlightedDays = remember(records) {
        records.map {
            Calendar.getInstance().apply { timeInMillis = it.createdAt }.get(Calendar.DAY_OF_MONTH)
        }.toSet()
    }

    AppCard {
        Text("감정 캘린더", fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                Text(it, color = Color(0xFF888888), style = MaterialTheme.typography.labelSmall)
            }
        }
        (1..30).chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    val hasRecord = highlightedDays.contains(day)
                    val selected = selectedDay == day
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                when {
                                    selected -> MooditPurple
                                    hasRecord -> MooditPurpleSoft
                                    else -> Color.Transparent
                                },
                                CircleShape
                            )
                            .clickable { onDaySelected(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            color = if (selected) Color.White else if (hasRecord) MooditPurple else Color(0xFF999999),
                            fontWeight = if (selected || hasRecord) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

@Composable
private fun DayExpenseList(
    selectedDay: Int,
    records: List<ExpenseRecord>,
    onDelete: (ExpenseRecord) -> Unit
) {
    AppCard {
        Text("${selectedDay}일 소비 내역", color = MooditPurple, fontWeight = FontWeight.SemiBold)

        if (records.isEmpty()) {
            Text("이 날짜에는 아직 기록이 없어요.", color = Color(0xFF777777))
        } else {
            records.sortedByDescending { it.createdAt }.forEach { record ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${record.category.emoji} ${record.category.label} · ${record.subCategory.emoji} ${record.subCategory.label}",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${moodDisplayText(record)} · ${timeOfDayText(record.createdAt)}",
                            color = Color(0xFF777777),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (record.memo.isNotBlank()) {
                            Text(record.memo, color = Color(0xFF777777), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "${record.amount}원",
                            color = MooditPurple,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { onDelete(record) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("삭제")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendCard(title: String, data: List<DailyTotal>) {
    AppCard {
        Text(title, fontWeight = FontWeight.SemiBold)
        TrendLineChart(data)
    }
}

@Composable
private fun DonutSummaryCard(title: String, categoryTotals: Map<ExpenseCategory, Int>) {
    val entries = categoryTotals.entries.sortedByDescending { it.value }.take(4)
    val total = entries.sumOf { it.value }.coerceAtLeast(1)

    AppCard {
        Text(title, fontWeight = FontWeight.SemiBold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DonutChart(
                values = entries.map { it.value },
                colors = entries.map { categoryColor(it.key) },
                modifier = Modifier.size(110.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entries.forEach { entry ->
                    val percent = (entry.value.toFloat() / total.toFloat() * 100f).toInt()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(categoryColor(entry.key), CircleShape)
                        )
                        Text("${entry.key.label} $percent%", color = Color(0xFF666666))
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedExpenseSummaryCard(records: List<ExpenseRecord>) {
    val fixedAmount = fixedExpenseAmount(records)
    val count = records.count { isFixedExpense(it) }

    SummaryCard(
        title = "고정 지출",
        value = "${fixedAmount}원",
        subtitle = "월세, 통신비, 구독, OTT, 세금 ${count}건"
    )
}

@Composable
private fun ProgressCard(title: String, amount: Int, total: Int) {
    val progress = if (total <= 0) 0f else (amount.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text("${amount}원", color = MooditPurple, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MooditPurple,
            trackColor = MooditPurpleSoft
        )
    }
}

@Composable
private fun MiniTrendLine() {
    Canvas(modifier = Modifier.size(width = 88.dp, height = 40.dp)) {
        val points = listOf(0.25f, 0.55f, 0.35f, 0.7f)
        val stepX = size.width / (points.size - 1)
        points.zipWithNext().forEachIndexed { index, pair ->
            drawLine(
                color = MooditPurple,
                start = Offset(stepX * index, size.height * (1f - pair.first)),
                end = Offset(stepX * (index + 1), size.height * (1f - pair.second)),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun TrendLineChart(data: List<DailyTotal>) {
    val chartData = remember(data) {
        if (data.isEmpty()) {
            listOf(DailyTotal("-", 0))
        } else {
            data.takeLast(12)
        }
    }

    val normalized = remember(data) {
        val values = chartData.map { it.amount }
        val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
        values.map { it.toFloat() / max.toFloat() }
    }
    val maxAmount = chartData.maxOfOrNull { it.amount }?.coerceAtLeast(1) ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(46.dp)
                    .height(150.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(formatAmountShort(maxAmount), color = Color(0xFF777777), style = MaterialTheme.typography.labelSmall)
                Text(formatAmountShort(maxAmount / 2), color = Color(0xFF777777), style = MaterialTheme.typography.labelSmall)
                Text("0", color = Color(0xFF777777), style = MaterialTheme.typography.labelSmall)
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
            ) {
                if (normalized.isEmpty()) return@Canvas
                val chartHeight = size.height * 0.85f
                val stepX = if (normalized.size == 1) 0f else size.width / (normalized.size - 1)

                if (normalized.size == 1) {
                    drawCircle(
                        color = MooditPurple,
                        radius = 7f,
                        center = Offset(size.width / 2f, chartHeight * (1f - normalized.first()) + 12f)
                    )
                    return@Canvas
                }

                normalized.zipWithNext().forEachIndexed { index, pair ->
                    drawLine(
                        color = MooditPurple,
                        start = Offset(stepX * index, chartHeight * (1f - pair.first) + 12f),
                        end = Offset(stepX * (index + 1), chartHeight * (1f - pair.second) + 12f),
                        strokeWidth = 7f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.width(54.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                visibleTrendLabels(chartData).forEach { label ->
                    Text(
                        text = label,
                        color = Color(0xFF777777),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    values: List<Int>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val total = values.sum().coerceAtLeast(1)
        val diameter = size.minDimension
        val strokeWidth = diameter * 0.18f
        var startAngle = -90f

        values.forEachIndexed { index, value ->
            val sweep = value.toFloat() / total.toFloat() * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f),
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

private fun todayDateText(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "%04d%02d%02d".format(year, month, day)
}

private fun dateTextToMillis(dateText: String): Long? {
    if (dateText.length != 8) return null

    val year = dateText.substring(0, 4).toIntOrNull() ?: return null
    val month = dateText.substring(4, 6).toIntOrNull() ?: return null
    val day = dateText.substring(6, 8).toIntOrNull() ?: return null

    if (month !in 1..12 || day !in 1..31) return null

    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun millisToDateText(timeMillis: Long): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timeMillis
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "%04d%02d%02d".format(year, month, day)
}

private fun currentTimeText(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    return "%02d%02d".format(hour, minute)
}

private fun dateTimeTextToMillis(dateText: String, timeText: String): Long? {
    if (dateText.length != 8 || timeText.length != 4) return null

    val year = dateText.substring(0, 4).toIntOrNull() ?: return null
    val month = dateText.substring(4, 6).toIntOrNull() ?: return null
    val day = dateText.substring(6, 8).toIntOrNull() ?: return null
    val hour = timeText.substring(0, 2).toIntOrNull() ?: return null
    val minute = timeText.substring(2, 4).toIntOrNull() ?: return null

    if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59) return null

    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun tossSampleRecords(): List<ExpenseRecord> {
    return listOf(
        tossRecord(202606050718L, 21250, "20260605", "0718", "05전기요금", ExpenseCategory.LIVING, ExpenseSubCategory.LIVING_SUBSCRIPTION, MoodType.NORMAL),
        tossRecord(202606032204L, 15900, "20260603", "2204", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.STRESS),
        tossRecord(202606011908L, 26600, "20260601", "1908", "토스페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605300210L, 23052, "20260530", "0210", "네이버파이낸셜", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605300205L, 12580, "20260530", "0205", "스마일페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605291159L, 20000, "20260529", "1159", "최주형", ExpenseCategory.ETC, ExpenseSubCategory.ETC_UNCLASSIFIED, MoodType.NORMAL),
        tossRecord(202605281919L, 11180, "20260528", "1919", "(주)이마트", ExpenseCategory.FOOD, ExpenseSubCategory.FOOD_GROCERY, MoodType.NORMAL),
        tossRecord(202605251722L, 16400, "20260525", "1722", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605221236L, 12340, "20260522", "1236", "최주형_JB(주)", ExpenseCategory.ETC, ExpenseSubCategory.ETC_UNCLASSIFIED, MoodType.NORMAL),
        tossRecord(202605201846L, 13410, "20260520", "1846", "스마일페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605200020L, 7900, "20260520", "0020", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.STRESS),
        tossRecord(202605190024L, 27880, "20260519", "0024", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.STRESS),
        tossRecord(202605181025L, 7890, "20260518", "1025", "쿠팡와우월회비", ExpenseCategory.LIVING, ExpenseSubCategory.LIVING_SUBSCRIPTION, MoodType.NORMAL),
        tossRecord(202605180942L, 2794, "20260518", "0942", "한국장학재단", ExpenseCategory.LIVING, ExpenseSubCategory.LIVING_EDUCATION, MoodType.NORMAL),
        tossRecord(202605171219L, 29500, "20260517", "1219", "토스페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605162113L, 380000, "20260516", "2113", "정대호", ExpenseCategory.LIVING, ExpenseSubCategory.LIVING_RENT, MoodType.NORMAL),
        tossRecord(202605152309L, 9000, "20260515", "2309", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.STRESS),
        tossRecord(202605141712L, 79784, "20260514", "1712", "네이버파이낸셜", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605140307L, 11200, "20260514", "0307", "스마일페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.STRESS),
        tossRecord(202605140306L, 8250, "20260514", "0307", "스마일페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.STRESS),
        tossRecord(202605140305L, 30230, "20260514", "0306", "간편결제_에스에", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.STRESS),
        tossRecord(202605132055L, 6900, "20260513", "2055", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605131504L, 13940, "20260513", "1504", "스마일페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605131443L, 18540, "20260513", "1443", "스마일페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605130921L, 133890, "20260513", "0921", "무신사페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_CLOTHES, MoodType.NORMAL),
        tossRecord(202605122113L, 9500, "20260512", "2113", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605121959L, 36950, "20260512", "1959", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605121316L, 5000, "20260512", "1316", "현대카드", ExpenseCategory.ETC, ExpenseSubCategory.ETC_UNCLASSIFIED, MoodType.NORMAL),
        tossRecord(202605110723L, 20000, "20260511", "0723", "최철주", ExpenseCategory.ETC, ExpenseSubCategory.ETC_UNCLASSIFIED, MoodType.NORMAL),
        tossRecord(202605102250L, 8505, "20260510", "2250", "토스페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.STRESS),
        tossRecord(202605101214L, 4990, "20260510", "1214", "토스페이", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605072310L, 50000, "20260507", "2310", "류영재", ExpenseCategory.ETC, ExpenseSubCategory.ETC_UNCLASSIFIED, MoodType.NORMAL),
        tossRecord(202605060716L, 46900, "20260506", "0716", "04전기요금", ExpenseCategory.LIVING, ExpenseSubCategory.LIVING_SUBSCRIPTION, MoodType.NORMAL),
        tossRecord(202605051946L, 15300, "20260505", "1946", "네이버파이낸셜", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605041847L, 28013, "20260504", "1847", "네이버파이낸셜", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL),
        tossRecord(202605041832L, 15850, "20260504", "1832", "쿠팡", ExpenseCategory.SHOPPING, ExpenseSubCategory.SHOPPING_DAILY_GOODS, MoodType.NORMAL)
    )
}

private fun tossRecord(
    id: Long,
    amount: Int,
    dateText: String,
    timeText: String,
    memo: String,
    category: ExpenseCategory,
    subCategory: ExpenseSubCategory,
    mood: MoodType
): ExpenseRecord {
    val createdAt = dateTimeTextToMillis(dateText, timeText) ?: id
    return ExpenseRecord(id, amount, category, subCategory, mood, memo, createdAt)
}

private fun dayOfMonth(timeMillis: Long): Int {
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
    }.get(Calendar.DAY_OF_MONTH)
}

private fun timeOfDayText(timeMillis: Long): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timeMillis
    }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    return "%02d:%02d".format(hour, minute)
}

private fun moodDisplayText(record: ExpenseRecord): String {
    return if (isFixedExpense(record)) {
        "고정 지출"
    } else {
        "${record.mood.emoji} ${record.mood.label}"
    }
}

private fun moodTemperature(records: List<ExpenseRecord>): Float {
    val moodRecords = records.filterNot { isFixedExpense(it) }
    if (moodRecords.isEmpty()) return 36.5f

    val total = moodRecords.size.toFloat()
    val happyRatio = moodRecords.count { it.mood == MoodType.HAPPY } / total
    val normalRatio = moodRecords.count { it.mood == MoodType.NORMAL } / total
    val sadRatio = moodRecords.count { it.mood == MoodType.SAD } / total
    val stressRatio = moodRecords.count { it.mood == MoodType.STRESS } / total

    val temperature = 36.5f +
        (happyRatio * 4.0f) +
        (normalRatio * 0.5f) -
        (sadRatio * 2.5f) -
        (stressRatio * 4.0f)

    return temperature.coerceIn(32.0f, 42.0f)
}

private fun categoryColor(category: ExpenseCategory): Color {
    return when (category) {
        ExpenseCategory.FOOD -> Color(0xFFFF9800)
        ExpenseCategory.SHOPPING -> Color(0xFFEC407A)
        ExpenseCategory.TRANSPORT -> Color(0xFF2196F3)
        ExpenseCategory.CAFE -> Color(0xFF8D6E63)
        ExpenseCategory.GAME -> Color(0xFF9C27B0)
        ExpenseCategory.CULTURE -> Color(0xFFE53935)
        ExpenseCategory.LIVING -> Color(0xFF43A047)
        ExpenseCategory.ETC -> Color(0xFF9E9E9E)
    }
}

private fun controllableSpendingAmount(records: List<ExpenseRecord>): Int {
    return records
        .filterNot { isFixedExpense(it) }
        .sumOf { it.amount }
}

private fun topSpendingTimePeriod(records: List<ExpenseRecord>): String {
    if (records.isEmpty()) return "기록 없음"

    val totals = linkedMapOf(
        "새벽" to 0,
        "오전" to 0,
        "오후" to 0,
        "저녁" to 0
    )

    val calendar = Calendar.getInstance()
    records.forEach { record ->
        calendar.timeInMillis = record.createdAt
        val label = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..5 -> "새벽"
            in 6..11 -> "오전"
            in 12..17 -> "오후"
            else -> "저녁"
        }
        totals[label] = (totals[label] ?: 0) + record.amount
    }

    val top = totals.maxByOrNull { it.value } ?: return "기록 없음"
    return "${top.key} ${top.value}원"
}

private fun riskyTimePeriod(records: List<ExpenseRecord>): String {
    if (records.isEmpty()) return "기록 없음"

    val totals = riskByTimePeriod(records)
    val top = totals.maxByOrNull { it.value } ?: return "기록 없음"

    return if (top.value == 0) {
        "주의 시간대 없음"
    } else {
        "${top.key} 위험도 ${top.value}/100"
    }
}

private fun regretWarningMessage(records: List<ExpenseRecord>): String {
    if (records.isEmpty()) return "소비 기록을 추가하면 후회 소비 위험도를 알려드려요."

    val regretRate = overallRegretRiskScore(records)

    return when {
        regretRate >= 80 -> "후회 소비 위험이 높아요. 밤 10시 이후 식비 결제는 10분만 멈춰보세요."
        regretRate >= 40 -> "후회 소비가 조금 보여요. 감정이 강한 시간대에는 소액만 써보세요."
        else -> "현재 후회 소비 위험은 낮은 편이에요."
    }
}

private fun riskByTimePeriod(records: List<ExpenseRecord>): LinkedHashMap<String, Int> {
    val scores = linkedMapOf(
        "새벽" to mutableListOf<Int>(),
        "오전" to mutableListOf(),
        "오후" to mutableListOf(),
        "저녁" to mutableListOf()
    )
    val nonFixedRecords = records.filterNot { isFixedExpense(it) }
    val concentrationBonus = timeConcentrationBonus(nonFixedRecords)
    val goalBonus = goalOveruseBonus(records)
    val lateNightBonus = lateNightFoodRepeatBonus(records)

    val calendar = Calendar.getInstance()
    nonFixedRecords.forEach { record ->
        calendar.timeInMillis = record.createdAt
        val label = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..5 -> "새벽"
            in 6..11 -> "오전"
            in 12..17 -> "오후"
            else -> "저녁"
        }
        scores.getValue(label).add(recordRiskScore(record))
    }

    return linkedMapOf(
        "새벽" to periodRiskScore("새벽", scores, concentrationBonus, goalBonus, 0),
        "오전" to periodRiskScore("오전", scores, concentrationBonus, goalBonus, 0),
        "오후" to periodRiskScore("오후", scores, concentrationBonus, goalBonus, 0),
        "저녁" to periodRiskScore("저녁", scores, concentrationBonus, goalBonus, lateNightBonus)
    )
}

private fun overallRegretRiskScore(records: List<ExpenseRecord>): Int {
    val nonFixedRecords = records.filterNot { isFixedExpense(it) }
    if (nonFixedRecords.isEmpty()) return 0

    val averageRecordRisk = nonFixedRecords.sumOf { recordRiskScore(it) } / nonFixedRecords.size
    return (averageRecordRisk +
        goalOveruseBonus(records) +
        lateNightFoodRepeatBonus(records) +
        (timeConcentrationBonus(nonFixedRecords).values.maxOrNull() ?: 0)
    ).coerceIn(0, 100)
}

private fun periodRiskScore(
    label: String,
    scores: Map<String, List<Int>>,
    concentrationBonus: Map<String, Int>,
    goalBonus: Int,
    lateNightBonus: Int
): Int {
    val periodScores = scores[label].orEmpty()
    if (periodScores.isEmpty()) return 0

    val average = periodScores.sum() / periodScores.size
    return (average +
        (concentrationBonus[label] ?: 0) +
        goalBonus +
        lateNightBonus
    ).coerceIn(0, 100)
}

private fun recordRiskScore(record: ExpenseRecord): Int {
    if (isFixedExpense(record)) return 0

    val calendar = Calendar.getInstance().apply { timeInMillis = record.createdAt }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val isRegretMood = record.mood == MoodType.SAD || record.mood == MoodType.STRESS
    val isLateNightFood = hour >= 22 && record.category == ExpenseCategory.FOOD

    var risk = 0
    if (isRegretMood) risk += 25
    if (isLateNightFood) risk += 20
    risk += record.subCategory.baseRiskWeight

    return risk.coerceIn(0, 100)
}

private fun lateNightFoodRepeatBonus(records: List<ExpenseRecord>): Int {
    val count = records.count { record ->
        !isFixedExpense(record) &&
            record.category == ExpenseCategory.FOOD &&
            Calendar.getInstance().apply { timeInMillis = record.createdAt }
                .get(Calendar.HOUR_OF_DAY) >= 22
    }

    return if (count <= 4) 0 else ((count - 4) * 8).coerceAtMost(32)
}

private fun goalOveruseBonus(records: List<ExpenseRecord>): Int {
    val totalAmount = records
        .filterNot { isFixedExpense(it) }
        .sumOf { it.amount }

    if (totalAmount <= MonthlyGoalAmount) return 0

    val overRate = (totalAmount - MonthlyGoalAmount) * 100 / MonthlyGoalAmount
    return (15 + overRate / 2).coerceAtMost(35)
}

private fun timeConcentrationBonus(records: List<ExpenseRecord>): Map<String, Int> {
    val totalAmount = records.sumOf { it.amount }
    if (totalAmount <= 0) {
        return mapOf("새벽" to 0, "오전" to 0, "오후" to 0, "저녁" to 0)
    }

    val totals = spendingByTimePeriod(records, regretsOnly = false)
    return totals.mapValues { entry ->
        val share = entry.value * 100 / totalAmount
        when {
            share >= 60 -> 25
            share >= 45 -> 15
            share >= 35 -> 8
            else -> 0
        }
    }
}

private fun isFixedExpense(record: ExpenseRecord): Boolean {
    return isFixedExpense(record.subCategory) ||
        isLegacyFixedExpense(record) ||
        isFixedExpenseMemo(record.memo)
}

private fun isLegacyFixedExpense(record: ExpenseRecord): Boolean {
    return record.category == ExpenseCategory.LIVING &&
        record.subCategory == ExpenseSubCategory.NONE
}

private fun isFixedExpenseMemo(memo: String): Boolean {
    val fixedKeywords = listOf("월세", "통신비", "세금", "구독")
    return fixedKeywords.any { keyword -> memo.contains(keyword, ignoreCase = true) }
}

private fun isFixedExpense(subCategory: ExpenseSubCategory): Boolean {
    return fixedExpenseSubCategories().contains(subCategory)
}

private fun fixedExpenseSubCategories(): List<ExpenseSubCategory> {
    return listOf(
        ExpenseSubCategory.LIVING_RENT,
        ExpenseSubCategory.LIVING_PHONE,
        ExpenseSubCategory.LIVING_SUBSCRIPTION,
        ExpenseSubCategory.CULTURE_OTT,
        ExpenseSubCategory.ETC_TAX
    )
}

private fun fixedExpenseAmount(records: List<ExpenseRecord>): Int {
    return records.filter { isFixedExpense(it) }.sumOf { it.amount }
}

private fun fixedExpenseTotals(records: List<ExpenseRecord>): Map<ExpenseSubCategory, Int> {
    return records
        .filter { isFixedExpense(it) }
        .groupBy { it.subCategory }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .filterValues { it > 0 }
}

private fun categoryTotalsWithoutFixed(records: List<ExpenseRecord>): Map<ExpenseCategory, Int> {
    return ExpenseCategory.values().associateWith { category ->
        records
            .filter { it.category == category && !isFixedExpense(it) }
            .sumOf { it.amount }
    }
}

private fun spendingByTimePeriod(
    records: List<ExpenseRecord>,
    regretsOnly: Boolean
): LinkedHashMap<String, Int> {
    val totals = linkedMapOf(
        "새벽" to 0,
        "오전" to 0,
        "오후" to 0,
        "저녁" to 0
    )

    val calendar = Calendar.getInstance()
    records
        .filter { record ->
            !regretsOnly || record.mood == MoodType.SAD || record.mood == MoodType.STRESS
        }
        .forEach { record ->
            calendar.timeInMillis = record.createdAt
            val label = when (calendar.get(Calendar.HOUR_OF_DAY)) {
                in 0..5 -> "새벽"
                in 6..11 -> "오전"
                in 12..17 -> "오후"
                else -> "저녁"
            }
            totals[label] = (totals[label] ?: 0) + record.amount
        }

    return totals
}

private fun buildDailyTotalsWithDates(records: List<ExpenseRecord>): List<DailyTotal> {
    val trendRecords = records.filterNot { isFixedExpense(it) }
    if (trendRecords.isEmpty()) return listOf(DailyTotal("-", 0))

    val calendar = Calendar.getInstance()
    val totalsByDate = linkedMapOf<Int, Int>()

    trendRecords.forEach { record ->
        calendar.timeInMillis = record.createdAt
        val dateKey = calendar.get(Calendar.YEAR) * 10000 +
            (calendar.get(Calendar.MONTH) + 1) * 100 +
            calendar.get(Calendar.DAY_OF_MONTH)

        totalsByDate[dateKey] = (totalsByDate[dateKey] ?: 0) + record.amount
    }

    return totalsByDate.toSortedMap().map { entry ->
        DailyTotal(
            label = trendDateLabel(entry.key),
            amount = entry.value
        )
    }
}

private fun trendDateLabel(dateKey: Int): String {
    val month = dateKey / 100 % 100
    val day = dateKey % 100
    return "${month}/${day}"
}

private fun formatAmountShort(amount: Int): String {
    return when {
        amount >= 10000 -> "${amount / 10000}만"
        amount >= 1000 -> "${amount / 1000}천"
        else -> amount.toString()
    }
}

private fun visibleTrendLabels(data: List<DailyTotal>): List<String> {
    if (data.size <= 6) return data.map { it.label }

    val lastIndex = data.lastIndex
    return listOf(
        data[0].label,
        data[lastIndex / 4].label,
        data[lastIndex / 2].label,
        data[lastIndex * 3 / 4].label,
        data[lastIndex].label
    ).distinct()
}
