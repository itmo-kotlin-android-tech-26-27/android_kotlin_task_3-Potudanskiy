package ru.itmo.expenses

import java.time.LocalDate

fun demoExpenses(today: LocalDate): List<Expense> {
    val firstDay = today.withDayOfMonth(1)
    return listOf(
        Expense("demo1", 42000, ExpenseCatalog.category("FOOD"), ExpenseCatalog.bank("T"), today, "Кофе и перекус"),
        Expense("demo2", 18000, ExpenseCatalog.category("TRANSIT"), ExpenseCatalog.bank("SBER"), today, "Метро и автобус"),
        Expense("demo3", 235000, ExpenseCatalog.category("FOOD"), ExpenseCatalog.bank("ALFA"), firstDay, "Продукты на неделю"),
        Expense("demo4", 120000, ExpenseCatalog.category("FUN"), ExpenseCatalog.bank("VTB"), firstDay, "Кино")
    )
}
