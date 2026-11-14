
package com.example.teacherassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ClassroomScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "مدیریت کلاس",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("در این بخش می‌توانیم حضور و غیاب، نمرات و عملکرد دانش‌آموزان را مدیریت کنیم.")
    }
}
