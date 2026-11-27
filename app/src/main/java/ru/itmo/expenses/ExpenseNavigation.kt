package ru.itmo.expenses

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.YearMonth

private const val HISTORY = "history"
private const val FORM = "form/{id}?month={month}"
private const val RESULT = "result/{id}"
private const val RETURN_MONTH = "returnMonth"

@Composable
internal fun ExpenseNavigation() {
    val nav = rememberNavController()
    val idArg = navArgument("id") { type = NavType.StringType }
    val monthArg = navArgument("month") {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    }

    NavHost(nav, startDestination = HISTORY) {
        composable(HISTORY) { entry ->
            val vm: HistoryViewModel = hiltViewModel()
            val ui by vm.ui.collectAsStateWithLifecycle()
            val period by entry.savedStateHandle.getStateFlow(RETURN_MONTH, "").collectAsStateWithLifecycle()
            LaunchedEffect(period) {
                if (period.isNotEmpty()) {
                    vm.showMonth(period)
                    entry.savedStateHandle[RETURN_MONTH] = ""
                }
            }
            HistoryScreen(ui, vm::move, vm::filter) { id ->
                if (nav.currentBackStackEntry == entry) {
                    nav.navigate("form/${Uri.encode(id)}?month=${ui.month}")
                }
            }
        }
        composable(FORM, arguments = listOf(idArg, monthArg)) { entry ->
            val vm: FormViewModel = hiltViewModel()
            FormScreen(vm, onBack = {
                if (nav.currentBackStackEntry == entry) nav.popBackStack()
            }, onSave = {
                if (nav.currentBackStackEntry == entry) {
                    vm.submit()?.let { id ->
                        val encodedId = Uri.encode(id)
                        if (entry.arguments?.getString("id") == "new") {
                            // Back from the result should reopen the saved expense by its real ID.
                            nav.navigate("form/$encodedId") { popUpTo(FORM) { inclusive = true } }
                        }
                        nav.navigate("result/$encodedId")
                    }
                }
            })
        }
        composable(RESULT, arguments = listOf(idArg)) { entry ->
            val vm: ResultViewModel = hiltViewModel()
            val ui by vm.ui.collectAsStateWithLifecycle()
            ResultScreen(ui, back = {
                if (nav.currentBackStackEntry == entry) nav.popBackStack()
            }, home = {
                if (nav.currentBackStackEntry == entry) {
                    ui.expense?.let { expense ->
                        nav.getBackStackEntry(HISTORY).savedStateHandle[RETURN_MONTH] =
                            YearMonth.from(expense.date).toString()
                    }
                    nav.popBackStack(HISTORY, inclusive = false)
                }
            })
        }
    }
}
