package ru.itmo.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneOffset

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
internal fun FormScreen(vm: FormViewModel, onBack: () -> Unit, onSave: () -> Unit) {
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
        Choice("Категория", d.category?.title, ExpenseCatalog.categories.map { it.title }, d.attempted && d.category == null) {
            vm.category(ExpenseCatalog.categories[it])
        }
        Choice("Банк", d.bank?.title, ExpenseCatalog.banks.map { it.title }, d.attempted && d.bank == null) { vm.bank(ExpenseCatalog.banks[it]) }
        OutlinedButton(onClick = { calendar = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Дата · ${d.date.format(dayFormat)} ${d.date.year}")
        }
        OutlinedTextField(d.note, vm::note, Modifier.fillMaxWidth(), label = { Text("Комментарий (необязательно)") })
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Сохранить расход") }
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
