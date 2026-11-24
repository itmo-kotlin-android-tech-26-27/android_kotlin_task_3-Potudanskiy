package ru.itmo.expenses

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun addExpenseRotateAndReturnWithoutDuplicate() {
        val note = "Покупка для проверки"
        rule.onNodeWithText("Учет расходов").assertIsDisplayed()
        capture("01-history")
        rule.onNodeWithText("+  Добавить расход").performClick()
        rule.onNodeWithText("Сохранить расход").performScrollTo().performClick()
        rule.onNodeWithText("Введите сумму от 0,01 до 999 999,99 ₽").assertExists()
        rule.onNodeWithText("Сумма, ₽").performTextInput("590,50")
        rule.onNodeWithText("Категория · Выбрать").performClick()
        rule.onNodeWithText("Продукты").performClick()
        rule.onNodeWithText("Банк · Выбрать").performClick()
        rule.onNodeWithText("Альфа-Банк").performClick()
        rule.onNodeWithText("Комментарий (необязательно)").performTextInput(note)
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText(note).assertExists()
        capture("02-form")
        rule.onNodeWithText("Сохранить расход").performScrollTo().performClick()
        rule.onNodeWithText("Итоги месяца").assertIsDisplayed()
        capture("03-result")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Изменить расход").assertExists()
        rule.onNodeWithText("Сохранить расход").performScrollTo().performClick()
        rule.onNodeWithText("К истории расходов").performScrollTo().performClick()
        rule.onAllNodesWithText(note).assertCountEquals(1)
        rule.onNodeWithText(note).performClick()
        rule.onNodeWithText("Удалить расход", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Удалить", substring = false).performClick()
        rule.onNodeWithText(note).assertDoesNotExist()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val folder = File(rule.activity.externalMediaDirs.first(), "lab3-screens")
        check(folder.isDirectory || folder.mkdirs())
        File(folder, "$name.png").outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
}
