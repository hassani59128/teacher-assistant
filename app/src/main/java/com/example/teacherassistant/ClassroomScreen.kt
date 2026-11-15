
package com.example.teacherassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.teacherassistant.data.AppDatabase

@Composable
fun ClassroomScreen(db: AppDatabase) {

    val schools by db.schoolDao().observeAll()
        .collectAsState(initial = emptyList())

    val classes by db.classDao().observeAll()
        .collectAsState(initial = emptyList())

    val lessons by db.lessonDao().observeAll()
        .collectAsState(initial = emptyList())

    var selectedSchoolId by remember { mutableStateOf<Long?>(null) }
    var selectedClassId by remember { mutableStateOf<Long?>(null) }
    var selectedLessonId by remember { mutableStateOf<Long?>(null) }

    var lessonIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(schools) {
        if (schools.none { it.id == selectedSchoolId }) {
            selectedSchoolId = schools.firstOrNull()?.id
            selectedClassId = null
            selectedLessonId = null
        }
    }

    val schoolClasses = classes.filter {
        it.schoolId == selectedSchoolId
    }

    LaunchedEffect(schoolClasses) {
        if (schoolClasses.none { it.id == selectedClassId }) {
            selectedClassId = schoolClasses.firstOrNull()?.id
            selectedLessonId = null
        }
    }

    LaunchedEffect(selectedClassId) {
        lessonIds = if (selectedClassId != null) {
            db.classLessonDao().lessonIdsForClass(selectedClassId!!)
        } else {
            emptyList()
        }
        selectedLessonId = null
    }

    val classLessons = lessons.filter { it.id in lessonIds }

    LaunchedEffect(classLessons) {
        if (classLessons.none { it.id == selectedLessonId }) {
            selectedLessonId = classLessons.firstOrNull()?.id
        }
    }

    val studentsFlow = remember(selectedClassId) {
        db.studentDao().observeByClass(selectedClassId ?: 0L)
    }

    val students by studentsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "مدیریت کلاس",
            style = MaterialTheme.typography.headlineMedium
        )

        IdDropdown(
            label = "انتخاب مدرسه",
            selectedId = selectedSchoolId,
            options = schools.map { it.id to it.name },
            onSelected = {
                selectedSchoolId = it
                selectedClassId = null
                selectedLessonId = null
            }
        )

        IdDropdown(
            label = "انتخاب کلاس",
            selectedId = selectedClassId,
            options = schoolClasses.map { it.id to it.name },
            onSelected = {
                selectedClassId = it
                selectedLessonId = null
            }
        )

        IdDropdown(
            label = "انتخاب درس",
            selectedId = selectedLessonId,
            options = classLessons.map { it.id to it.name },
            onSelected = {
                selectedLessonId = it
            }
        )

        HorizontalDivider()

        Text(
            text = "دانش‌آموزان",
            style = MaterialTheme.typography.titleLarge
        )

        if (selectedClassId == null) {
            Text("ابتدا مدرسه و کلاس را انتخاب کنید.")
        } else if (students.isEmpty()) {
            Text("برای این کلاس دانش‌آموزی ثبت نشده است.")
        } else {
            students.forEachIndexed { index, student ->
                Text(
                    text = "${index + 1}. ${student.firstName} ${student.lastName}"
                )
            }
        }
    }
}

@Composable
private fun IdDropdown(
    label: String,
    selectedId: Long?,
    options: List<Pair<Long, String>>,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedText = options.firstOrNull {
        it.first == selectedId
    }?.second ?: "انتخاب کنید"

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label)

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedText)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            onSelected(option.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
