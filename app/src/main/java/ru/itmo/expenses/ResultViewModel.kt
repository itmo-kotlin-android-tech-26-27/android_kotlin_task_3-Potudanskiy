package ru.itmo.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.*

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
