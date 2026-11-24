package ru.itmo.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.*

data class HistoryUi(val month: YearMonth, val category: Category?, val rows: List<Expense>, val amount: Long)

@HiltViewModel
class HistoryViewModel @Inject constructor(repo: ExpenseRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val month = saved.getStateFlow("month", YearMonth.now().toString())
    private val filter = saved.getStateFlow("filter", "")
    private fun history(items: List<Expense>, period: String, key: String): HistoryUi {
        val chosen = Category.entries.find { it.name == key }
        val rows = inMonth(items, YearMonth.parse(period)).filter { chosen == null || it.category == chosen }
        return HistoryUi(YearMonth.parse(period), chosen, rows, totals(rows).total)
    }
    val ui = combine(repo.expenses, month, filter, ::history)
        .stateIn(viewModelScope, SharingStarted.Eagerly, history(repo.expenses.value, month.value, filter.value))
    fun move(offset: Long) { saved["month"] = YearMonth.parse(month.value).plusMonths(offset).toString() }
    fun filter(category: Category?) { saved["filter"] = category?.name.orEmpty() }
    fun showMonth(period: String) { saved["month"] = period; filter(null) }
}

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
        saved.get<String>("bank")?.let(Bank::valueOf) ?: original?.bank,
        saved.get<String>("category")?.let(Category::valueOf) ?: original?.category,
        saved["note"] ?: original?.note.orEmpty(), saved["attempted"] ?: false,
        repo.expenses.value.any { it.id == id }, routeId != "new" && original == null
    )
    private val state = MutableStateFlow(draft())
    val ui = state.asStateFlow()
    private fun <T> field(key: String, value: T) { saved[key] = value; state.value = draft() }
    fun amount(value: String) = field("amount", value)
    fun date(value: LocalDate) = field("date", value.toString())
    fun bank(value: Bank) = field("bank", value.name)
    fun category(value: Category) = field("category", value.name)
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

data class ResultUi(val expense: Expense?, val totals: Totals)
@HiltViewModel
class ResultViewModel @Inject constructor(repo: ExpenseRepository, saved: SavedStateHandle) : ViewModel() {
    private val id = checkNotNull(saved.get<String>("id"))
    private fun result(items: List<Expense>): ResultUi {
        val e = items.find { it.id == id }
        return ResultUi(e, totals(e?.let { inMonth(items, YearMonth.from(it.date)) }.orEmpty()))
    }
    val ui = repo.expenses.map(::result).stateIn(viewModelScope, SharingStarted.Eagerly, result(repo.expenses.value))
}
