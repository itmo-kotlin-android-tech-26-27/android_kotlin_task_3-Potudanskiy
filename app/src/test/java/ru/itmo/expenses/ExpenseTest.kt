package ru.itmo.expenses

import java.time.LocalDate
import java.time.YearMonth
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExpenseTest {
    private val day = LocalDate.of(2026, 11, 24)
    private val month = YearMonth.from(day)
    private val seed = demoExpenses(day)

    @Test fun kopecksStayExact() {
        assertEquals(1L, parseAmount("0,01"))
        assertEquals(59050L, parseAmount("590.50"))
        assertEquals(99999999L, parseAmount("999999,99"))
    }
    @Test fun rejectsInvalidAmounts() {
        listOf("", "0", "-10", "0.001", "1000000", "1e3", "abc", "NaN").forEach {
            assertNull(parseAmount(it), it)
        }
    }
    @Test fun seedCoversFourBanksInTheCurrentMonth() {
        for (today in listOf(day, day.withDayOfMonth(1))) {
            val demo = demoExpenses(today)
            assertEquals(4, demo.map { it.bank }.distinct().size)
            assertTrue(demo.all { !it.date.isAfter(today) && YearMonth.from(it.date) == month })
        }
    }
    @Test fun addingPurchaseUpdatesBothTotals() {
        val purchase = Expense("new-id", 59050, ExpenseCatalog.category("FOOD"), ExpenseCatalog.bank("ALFA"), day, "Магазин")
        val november = totals(inMonth(seed + purchase + purchase.copy(date = day.minusMonths(1)), month))
        assertEquals(474050L, november.total)
        assertEquals(336050L, november.categories[ExpenseCatalog.category("FOOD")])
    }
    @Test fun monthWithoutEntriesHasZeroTotal() {
        val empty = inMonth(seed, month.minusMonths(1))
        assertTrue(empty.isEmpty())
        assertEquals(0L, totals(empty).total)
    }
}
