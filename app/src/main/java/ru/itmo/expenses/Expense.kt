package ru.itmo.expenses

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val russian = Locale.forLanguageTag("ru-RU")
val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", russian)
val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", russian)

enum class Bank(val title: String) {
    T("Т-Банк"), SBER("Сбер"), ALFA("Альфа-Банк"), VTB("ВТБ")
}

enum class Category(val title: String) {
    FOOD("Продукты"), TRANSIT("Транспорт"), HOME("Жилье"), FUN("Развлечения"),
    HEALTH("Здоровье"), OTHER("Другое")
}

data class Expense(
    val id: String, val cents: Long, val category: Category, val bank: Bank,
    val date: LocalDate, val note: String
)

fun parseAmount(input: String): Long? {
    val value = input.trim().replace(',', '.')
    if (!Regex("""[0-9]{1,6}(\.[0-9]{1,2})?""").matches(value)) return null
    return BigDecimal(value).movePointRight(2).longValueExact().takeIf { it > 0 }
}

fun rubles(cents: Long): String = NumberFormat.getCurrencyInstance(russian)
    .format(BigDecimal.valueOf(cents, 2))

data class Totals(val total: Long, val categories: Map<Category, Long>)
fun totals(items: List<Expense>): Totals = Totals(
    items.sumOf { it.cents },
    items.groupBy { it.category }.mapValues { (_, group) -> group.sumOf { it.cents } }
)

fun inMonth(items: List<Expense>, month: YearMonth) =
    items.filter { YearMonth.from(it.date) == month }.sortedByDescending { it.date }

fun demoExpenses(today: LocalDate): List<Expense> {
    val firstDay = today.withDayOfMonth(1)
    return listOf(
        Expense("demo1", 42000, Category.FOOD, Bank.T, today, "Кофе и перекус"),
        Expense("demo2", 18000, Category.TRANSIT, Bank.SBER, today, "Метро и автобус"),
        Expense("demo3", 235000, Category.FOOD, Bank.ALFA, firstDay, "Продукты на неделю"),
        Expense("demo4", 120000, Category.FUN, Bank.VTB, firstDay, "Кино")
    )
}
