package ru.itmo.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ResultScreen(ui: ResultUi, back: () -> Unit, home: () -> Unit) {
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
