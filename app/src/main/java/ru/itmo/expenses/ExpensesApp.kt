package ru.itmo.expenses

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

@HiltAndroidApp
class ExpensesApp : Application()

interface ExpenseRepository {
    val expenses: StateFlow<List<Expense>>
    fun save(expense: Expense)
    fun delete(id: String)
}

@Singleton
class LocalExpenseRepository @Inject constructor(@ApplicationContext context: Context) : ExpenseRepository {
    private val storage = context.getSharedPreferences("expense_book_v1", Context.MODE_PRIVATE)
    private val items = MutableStateFlow(load())
    override val expenses = items.asStateFlow()

    private fun load(): List<Expense> {
        val saved = storage.getString("entries", null)
        if (saved == null) return demoExpenses(LocalDate.now()).also(::persist)
        val array = JSONArray(saved)
        return List(array.length()) { index ->
            val row = array.getJSONObject(index)
            Expense(row.getString("id"), row.getLong("cents"), ExpenseCatalog.category(row.getString("category")),
                ExpenseCatalog.bank(row.getString("bank")), LocalDate.parse(row.getString("date")), row.getString("note"))
        }
    }

    private fun persist(entries: List<Expense>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(JSONObject().put("id", e.id).put("cents", e.cents).put("category", e.category.id)
                .put("bank", e.bank.id).put("date", e.date.toString()).put("note", e.note))
        }
        storage.edit().putString("entries", array.toString()).apply()
    }

    @Synchronized
    private fun update(transform: (List<Expense>) -> List<Expense>) {
        val next = transform(items.value)
        persist(next)
        items.value = next
    }

    override fun save(expense: Expense) = update { list -> listOf(expense) + list.filterNot { it.id == expense.id } }
    override fun delete(id: String) = update { list -> list.filterNot { it.id == id } }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ExpenseModule {
    @Binds abstract fun repository(source: LocalExpenseRepository): ExpenseRepository
}
