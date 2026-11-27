package ru.itmo.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.*

data class HistoryUi(val month: YearMonth, val category: Category?, val rows: List<Expense>, val amount: Long)

@HiltViewModel
class HistoryViewModel @Inject constructor(repo: ExpenseRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val month = saved.getStateFlow("month", YearMonth.now().toString())
    private val filter = saved.getStateFlow("filter", "")
    private fun history(items: List<Expense>, period: String, key: String): HistoryUi {
        val chosen = ExpenseCatalog.categories.find { it.id == key }
        val rows = inMonth(items, YearMonth.parse(period)).filter { chosen == null || it.category == chosen }
        return HistoryUi(YearMonth.parse(period), chosen, rows, totals(rows).total)
    }
    val ui = combine(repo.expenses, month, filter, ::history)
        .stateIn(viewModelScope, SharingStarted.Eagerly, history(repo.expenses.value, month.value, filter.value))
    fun move(offset: Long) { saved["month"] = YearMonth.parse(month.value).plusMonths(offset).toString() }
    fun filter(category: Category?) { saved["filter"] = category?.id.orEmpty() }
    fun showMonth(period: String) { saved["month"] = period; filter(null) }
}
