package ru.itmo.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Draft(val amount: String, val date: LocalDate, val bank: Bank?, val category: Category?,
                 val note: String, val attempted: Boolean, val exists: Boolean, val missing: Boolean) {
    val invalidAmount: Boolean get() = attempted && parseAmount(amount) == null
}

@HiltViewModel
class FormViewModel @Inject constructor(private val repo: ExpenseRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val routeId = checkNotNull(saved.get<String>("id"))
    private val id = if (routeId == "new") saved.get<String>("draftId") ?: UUID.randomUUID().toString()
        .also { saved["draftId"] = it } else routeId
    private val original = repo.expenses.value.find { it.id == id }
    private val defaultDate = LocalDate.now().let { today ->
        val month = saved.get<String>("month")?.let(YearMonth::parse) ?: YearMonth.from(today)
        if (month == YearMonth.from(today)) today else month.atDay(1)
    }
    private fun draft() = Draft(
        saved["amount"] ?: original?.let { BigDecimal.valueOf(it.cents, 2).toPlainString().replace('.', ',') }.orEmpty(),
        LocalDate.parse(saved["date"] ?: original?.date?.toString() ?: defaultDate.toString()),
        saved.get<String>("bank")?.let(ExpenseCatalog::bank) ?: original?.bank,
        saved.get<String>("category")?.let(ExpenseCatalog::category) ?: original?.category,
        saved["note"] ?: original?.note.orEmpty(), saved["attempted"] ?: false,
        repo.expenses.value.any { it.id == id }, routeId != "new" && original == null
    )
    private val state = MutableStateFlow(draft())
    val ui = state.asStateFlow()
    private fun <T> field(key: String, value: T) { saved[key] = value; state.value = draft() }
    fun amount(value: String) = field("amount", value)
    fun date(value: LocalDate) = field("date", value.toString())
    fun bank(value: Bank) = field("bank", value.id)
    fun category(value: Category) = field("category", value.id)
    fun note(value: String) = field("note", value)
    fun submit(): String? {
        field("attempted", true)
        val d = state.value
        if (d.missing) return null
        val expense = Expense(id, parseAmount(d.amount) ?: return null, d.category ?: return null,
            d.bank ?: return null, d.date, d.note.trim())
        repo.save(expense)
        state.value = draft()
        return id
    }
    fun delete() = repo.delete(id)
}
