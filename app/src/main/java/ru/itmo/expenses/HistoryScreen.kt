package ru.itmo.expenses

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun HistoryScreen(ui: HistoryUi, move: (Long) -> Unit, filter: (Category?) -> Unit, open: (String) -> Unit) {
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
                    ExpenseCatalog.categories.forEach { category ->
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
