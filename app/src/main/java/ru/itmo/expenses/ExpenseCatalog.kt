package ru.itmo.expenses

data class Bank(val id: String, val title: String)
data class Category(val id: String, val title: String)

object ExpenseCatalog {
    val banks = listOf(
        Bank("T", "Т-Банк"),
        Bank("SBER", "Сбер"),
        Bank("ALFA", "Альфа-Банк"),
        Bank("VTB", "ВТБ")
    )
    val categories = listOf(
        Category("FOOD", "Продукты"),
        Category("TRANSIT", "Транспорт"),
        Category("HOME", "Жилье"),
        Category("FUN", "Развлечения"),
        Category("HEALTH", "Здоровье"),
        Category("OTHER", "Другое")
    )

    fun bank(id: String): Bank = banks.find { it.id == id } ?: Bank(id, id)
    fun category(id: String): Category = categories.find { it.id == id } ?: Category(id, id)
}
