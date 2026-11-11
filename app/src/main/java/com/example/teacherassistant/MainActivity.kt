package com.example.teacherassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teacherassistant.data.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(this)
        setContent {
            MaterialTheme {
                TeacherAssistantApp(db)
            }
        }
    }
}

private enum class Section(val title: String) {
    Schools("مدارس"), Classes("کلاس‌ها"), Lessons("درس‌ها")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherAssistantApp(db: AppDatabase) {
    var section by remember { mutableStateOf(Section.Schools) }
    val schools by db.schoolDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val classes by db.classDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val lessons by db.lessonDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var selectedSchoolId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("دستیار معلم") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newName = ""
                selectedSchoolId = schools.firstOrNull()?.id
                showAdd = true
            }) { Text("+") }
        },
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = { },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(section.title, style = MaterialTheme.typography.headlineSmall)
            when (section) {
                Section.Schools -> {
                    if (schools.isEmpty()) EmptyMessage("هنوز مدرسه‌ای ثبت نشده است.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(schools, key = { it.id }) { item ->
                            ListItem(headlineContent = { Text(item.name) })
                            HorizontalDivider()
                        }
                    }
                }
                Section.Classes -> {
                    if (classes.isEmpty()) EmptyMessage("هنوز کلاسی ثبت نشده است.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(classes, key = { it.id }) { item ->
                            val schoolName = schools.firstOrNull { it.id == item.schoolId }?.name ?: "مدرسه نامشخص"
                            ListItem(headlineContent = { Text(item.name) }, supportingContent = { Text(schoolName) })
                            HorizontalDivider()
                        }
                    }
                }
                Section.Lessons -> {
                    if (lessons.isEmpty()) EmptyMessage("هنوز درسی ثبت نشده است.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(lessons, key = { it.id }) { item ->
                            ListItem(headlineContent = { Text(item.name) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(when (section) {
                Section.Schools -> "افزودن مدرسه"
                Section.Classes -> "افزودن کلاس"
                Section.Lessons -> "افزودن درس"
            }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(when (section) {
                            Section.Schools -> "نام مدرسه"
                            Section.Classes -> "نام کلاس"
                            Section.Lessons -> "نام درس"
                        }) },
                        singleLine = true
                    )
                    if (section == Section.Classes) {
                        if (schools.isEmpty()) {
                            Text("ابتدا یک مدرسه ثبت کن.")
                        } else {
                            Text("مدرسه مربوطه")
                            schools.forEach { school ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedSchoolId == school.id,
                                        onClick = { selectedSchoolId = school.id }
                                    )
                                    Text(school.name)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            when (section) {
                                Section.Schools -> db.schoolDao().insert(School(name = name))
                                Section.Classes -> selectedSchoolId?.let {
                                    db.classDao().insert(SchoolClass(schoolId = it, name = name))
                                }
                                Section.Lessons -> db.lessonDao().insert(Lesson(name = name))
                            }
                        }
                        showAdd = false
                    }
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
