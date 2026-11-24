package ru.itmo.expenses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

private val purple = Color(0xFF594397)
private val pale = Color(0xFFF9F6FC)
private const val formRoute = "form/{id}?month={month}"
private val colors = lightColorScheme(primary = purple, background = pale, surface = pale,
    secondaryContainer = Color(0xFFE8DFF7), primaryContainer = Color(0xFFE8DFF7))

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = colors) {
                Surface(Modifier.fillMaxSize(), color = pale) {
                    Box(Modifier.safeDrawingPadding().imePadding()) { ExpenseNavigation() }
                }
            }
        }
    }
}

@Composable
private fun ExpenseNavigation() {
    val nav = rememberNavController()
    val idArg = listOf(navArgument("id") { type = NavType.StringType })
    NavHost(nav, startDestination = "history") {
        composable("history") { entry ->
            val vm: HistoryViewModel = hiltViewModel()
            val ui by vm.ui.collectAsStateWithLifecycle()
            val period by entry.savedStateHandle.getStateFlow("returnMonth", "").collectAsStateWithLifecycle()
            LaunchedEffect(period) {
                if (period.isNotEmpty()) {
                    vm.showMonth(period)
                    entry.savedStateHandle["returnMonth"] = ""
                }
            }
            HistoryScreen(ui, vm::move, vm::filter) { id ->
                if (nav.currentDestination?.route == "history") nav.navigate("form/$id?month=${ui.month}")
            }
        }
        composable(formRoute, arguments = idArg + navArgument("month") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }) {
            val vm: FormViewModel = hiltViewModel()
            FormScreen(vm, onBack = { nav.popBackStack() }, onSaved = { id ->
                if (nav.currentDestination?.route == formRoute) nav.navigate("result/$id") { launchSingleTop = true }
            })
        }
        composable("result/{id}", arguments = idArg) {
            val vm: ResultViewModel = hiltViewModel()
            val ui by vm.ui.collectAsStateWithLifecycle()
            ResultScreen(ui, back = { nav.popBackStack() }) {
                ui.expense?.let { expense ->
                    nav.getBackStackEntry("history").savedStateHandle["returnMonth"] = YearMonth.from(expense.date).toString()
                }
                nav.popBackStack("history", inclusive = false)
            }
        }
    }
}

@Composable
private fun TotalCard(title: String, amount: Long) {
    Surface(shape = RoundedCornerShape(20.dp), color = purple, contentColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(rubles(amount), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HistoryScreen(ui: HistoryUi, move: (Long) -> Unit, filter: (Category?) -> Unit, open: (String) -> Unit) {
    Scaffold(containerColor = pale, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { open("new") }, containerColor = purple, contentColor = Color.White) {
                Text("+  Добавить расход")
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Учет расходов", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { move(-1) }) { Text("← Месяц") }
                    Text(ui.month.format(monthFormat), fontWeight = FontWeight.Medium)
                    TextButton(onClick = { move(1) }) { Text("Месяц →") }
                }
                TotalCard(ui.category?.title ?: "Расходы за месяц", ui.amount)
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = ui.category == null, onClick = { filter(null) }, label = { Text("Все") })
                    Category.entries.forEach { category ->
                        FilterChip(selected = ui.category == category, onClick = { filter(category) },
                            label = { Text(category.title) })
                    }
                }
                Text("История", style = MaterialTheme.typography.titleLarge)
            }
            if (ui.rows.isEmpty()) item {
                Text("За этот период расходов по выбранному фильтру нет.", Modifier.padding(vertical = 20.dp))
                TextButton(onClick = { open("new") }) { Text("Добавить расход") }
            }
            items(ui.rows, key = { it.id }) { e ->
                Surface(onClick = { open(e.id) }, shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(e.note.ifBlank { e.category.title }, fontWeight = FontWeight.Medium)
                            Text("${e.bank.title} · ${e.category.title}", style = MaterialTheme.typography.bodySmall)
                            Text(e.date.format(dayFormat), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rubles(e.cents), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, back: () -> Unit) {
    TextButton(onClick = back) { Text("← Назад") }
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Choice(label: String, selected: String?, options: List<String>, invalid: Boolean, choose: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label · ${selected ?: "Выбрать"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, text ->
                DropdownMenuItem(text = { Text(text) }, onClick = { choose(i); expanded = false })
            }
        }
    }
    if (invalid) Text("Выберите значение", color = MaterialTheme.colorScheme.error)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormScreen(vm: FormViewModel, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val d by vm.ui.collectAsStateWithLifecycle()
    var calendar by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header(if (d.exists) "Изменить расход" else "Новый расход", onBack)
        if (d.missing) {
            Text("Расход не найден.")
            return@Column
        }
        val amountError = d.invalidAmount
        OutlinedTextField(d.amount, vm::amount, Modifier.fillMaxWidth(), label = { Text("Сумма, ₽") },
            singleLine = true, isError = amountError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = if (amountError) {{ Text("Введите сумму от 0,01 до 999 999,99 ₽") }} else null)
        Choice("Категория", d.category?.title, Category.entries.map { it.title }, d.attempted && d.category == null) {
            vm.category(Category.entries[it])
        }
        Choice("Банк", d.bank?.title, Bank.entries.map { it.title }, d.attempted && d.bank == null) { vm.bank(Bank.entries[it]) }
        OutlinedButton(onClick = { calendar = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Дата · ${d.date.format(dayFormat)} ${d.date.year}")
        }
        OutlinedTextField(d.note, vm::note, Modifier.fillMaxWidth(), label = { Text("Комментарий (необязательно)") })
        Button(onClick = { vm.submit()?.let(onSaved) }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить расход") }
        if (d.exists) TextButton(onClick = { deleting = true }) { Text("Удалить расход", color = MaterialTheme.colorScheme.error) }
    }
    if (calendar) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = d.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { calendar = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { vm.date(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                calendar = false
            }) { Text("Выбрать") }
        }, dismissButton = { TextButton(onClick = { calendar = false }) { Text("Отмена") } }) {
            DatePicker(state = picker, showModeToggle = false)
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Удалить расход?") },
        text = { Text("Сумма месяца изменится.") },
        confirmButton = { TextButton(onClick = { deleting = false; vm.delete(); onBack() }) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Отмена") } })
}

@Composable
private fun ResultScreen(ui: ResultUi, back: () -> Unit, home: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header("Итоги месяца", back)
        val expense = ui.expense
        if (expense != null) {
            Text("Расход сохранен · ${rubles(expense.cents)}")
            TotalCard(expense.date.format(monthFormat), ui.totals.total)
            Text("Всего в категории «${expense.category.title}»", style = MaterialTheme.typography.titleMedium)
            Text(rubles(ui.totals.categories.getValue(expense.category)), style = MaterialTheme.typography.headlineSmall)
            HorizontalDivider()
            Text("По категориям", style = MaterialTheme.typography.titleMedium)
            ui.totals.categories.entries.sortedByDescending { it.value }.forEach { (category, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category.title)
                    Text(rubles(value))
                }
            }
        } else Text("Запись не найдена.")
        Button(onClick = home, modifier = Modifier.fillMaxWidth()) { Text("К истории расходов") }
    }
}
