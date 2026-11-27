package ru.itmo.expenses

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class ExpenseFlowTest {
    @Test
    fun addExpenseRotateAndReturnWithoutDuplicate() = runEmptyComposeUiTest {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val note = "Покупка для проверки " + UUID.randomUUID().toString().take(8)
            onNodeWithText("Учет расходов").assertIsDisplayed()
            capture("01-history")
            onNodeWithText("+  Добавить расход").performClick()
            onNodeWithText("Сохранить расход").performScrollTo().performClick()
            onNodeWithText("Введите сумму от 0,01 до 999 999,99 ₽").assertExists()
            onNodeWithText("Сумма, ₽").performTextInput("590,50")
            onNodeWithText("Категория · Выбрать").performClick()
            onNodeWithText("Продукты").performClick()
            onNodeWithText("Банк · Выбрать").performClick()
            onNodeWithText("Альфа-Банк").performClick()
            onNodeWithText("Комментарий (необязательно)").performTextInput(note)
            scenario.recreate()
            onNodeWithText(note).assertExists()
            capture("02-form")
            onNodeWithText("Сохранить расход").performScrollTo().performClick()
            onNodeWithText("Итоги месяца").assertIsDisplayed()
            capture("03-result")
            scenario.recreate()
            onNodeWithText("Итоги месяца").assertIsDisplayed()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            onNodeWithText("Изменить расход").assertExists()
            onNodeWithText(note).assertExists()
            onNodeWithText("Сохранить расход").performScrollTo().performClick()
            onNodeWithText("К истории расходов").performScrollTo().performClick()
            onAllNodesWithText(note).assertCountEquals(1)
            onNodeWithText(note).performClick()
            onNodeWithText("Удалить расход", substring = false).performScrollTo().performClick()
            onNodeWithText("Удалить", substring = false).performClick()
            onNodeWithText(note).assertDoesNotExist()
        }
    }

    private fun ComposeUiTest.capture(name: String) {
        waitForIdle()
        val bitmap = onRoot().captureToImage().asAndroidBitmap()
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.externalMediaDirs.first(), "lab3-screens")
        check(folder.isDirectory || folder.mkdirs())
        File(folder, "$name.png").outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
}
