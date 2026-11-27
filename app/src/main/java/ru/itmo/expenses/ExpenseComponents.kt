package ru.itmo.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val purple = Color(0xFF594397)
internal val pale = Color(0xFFF9F6FC)
internal val colors = lightColorScheme(primary = purple, background = pale, surface = pale,
    secondaryContainer = Color(0xFFE8DFF7), primaryContainer = Color(0xFFE8DFF7))

@Composable
internal fun TotalCard(title: String, amount: Long) {
    Surface(shape = RoundedCornerShape(20.dp), color = purple, contentColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(rubles(amount), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun Header(title: String, back: () -> Unit) {
    TextButton(onClick = back) { Text("← Назад") }
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}
