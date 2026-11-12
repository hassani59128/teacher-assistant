
package com.example.teacherassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teacherassistant.data.*
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

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
    Schools("مدارس"),
    Classes("کلاس‌ها"),
    Lessons("درس‌ها"),
    Students("دانش‌آموزان")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherAssistantApp(db: AppDatabase) {
    var section by remember { mutableStateOf(Section.Schools) }

    val schools by db.schoolDao().observeAll()
        .collectAsStateWithLifecycle(emptyList())

    val classes by db.classDao().observeAll()
        .collectAsStateWithLifecycle(emptyList())

    val lessons by db.lessonDao().observeAll()
        .collectAsStateWithLifecycle(emptyList())

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var excelRows by remember {
    mutableStateOf<List<ExcelStudentRow>>(emptyList())
}

val openExcelFile =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null) {
            try {
                excelRows = ExcelStudentReader.read(context, uri)

                Toast.makeText(
                    context,
                    "تعداد ${excelRows.size} ردیف از فایل خوانده شد.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "خطا در خواندن فایل: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

val createExcelTemplate =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->

        if (uri != null) {
            try {
                ExcelTemplateWriter.write(context, uri)

                Toast.makeText(
                    context,
                    "قالب Excel با موفقیت ذخیره شد.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    context,
                    "خطا در ساخت فایل Excel: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var selectedSchoolId by remember { mutableStateOf<Long?>(null) }

    var studentSchoolId by remember { mutableStateOf<Long?>(null) }
    var studentClassId by remember { mutableStateOf<Long?>(null) }

    var students by remember { mutableStateOf(emptyList<Student>()) }
    var studentSearch by remember { mutableStateOf("") }

    var showStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var studentError by remember { mutableStateOf<String?>(null) }

    var studentScreenMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(schools) {
        if (schools.none { it.id == studentSchoolId }) {
            studentSchoolId = schools.firstOrNull()?.id
        }
    }

    val classesForStudentSchool = classes.filter {
        it.schoolId == studentSchoolId
    }

    LaunchedEffect(studentSchoolId, classes) {
        if (classesForStudentSchool.none { it.id == studentClassId }) {
            studentClassId = classesForStudentSchool.firstOrNull()?.id
        }
    }

    LaunchedEffect(studentClassId) {
        val classId = studentClassId

        if (classId == null) {
            students = emptyList()
        } else {
            db.studentDao().observeByClass(classId).collect {
                students = it
            }
        }
    }

    val filteredStudents = students.filter {
        val query = studentSearch.trim()
        query.isEmpty() ||
            it.firstName.contains(query, ignoreCase = true) ||
            it.lastName.contains(query, ignoreCase = true) ||
            it.studentNumber.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دستیار معلم") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (section == Section.Students) {
                        if (studentClassId == null) {
                            studentScreenMessage =
                                "ابتدا مدرسه و کلاس را انتخاب کن."
                        } else {
                            editingStudent = null
                            lastName = ""
                            firstName = ""
                            studentNumber = ""
                            fatherName = ""
                            studentError = null
                            showStudentDialog = true
                        }
                    } else {
                        newName = ""
                        selectedSchoolId = schools.firstOrNull()?.id
                        showAdd = true
                    }
                }
            ) {
                Text("+")
            }
        },
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = {
                            section = item
                            studentScreenMessage = null
                        },
                        icon = {},
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                section.title,
                style = MaterialTheme.typography.headlineSmall
            )

            when (section) {

                Section.Schools -> {
                    if (schools.isEmpty()) {
                        EmptyMessage("هنوز مدرسه‌ای ثبت نشده است.")
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(schools, key = { it.id }) { item ->
                            ListItem(
                                headlineContent = { Text(item.name) }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                Section.Classes -> {
                    if (classes.isEmpty()) {
                        EmptyMessage("هنوز کلاسی ثبت نشده است.")
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(classes, key = { it.id }) { item ->
                            val schoolName =
                                schools.firstOrNull {
                                    it.id == item.schoolId
                                }?.name ?: "مدرسه نامشخص"

                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text(schoolName) }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                Section.Lessons -> {
                    if (lessons.isEmpty()) {
                        EmptyMessage("هنوز درسی ثبت نشده است.")
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(lessons, key = { it.id }) { item ->
                            ListItem(
                                headlineContent = { Text(item.name) }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                Section.Students -> {

                    Button(
    onClick = {
        createExcelTemplate.launch("قالب دانش‌آموزان.xlsx")
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("دریافت قالب Excel")
}

Button(
    onClick = {
        openExcelFile.launch(
            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        )
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("ورود دانش‌آموزان از Excel")
}

val duplicateStudentNumbers = excelRows
    .filter { it.isValid }
    .groupingBy { it.studentNumber }
    .eachCount()
    .filter { it.value > 1 }
    .keys
if (excelRows.isNotEmpty()) {
    Text(
        text = "پیش‌نمایش اطلاعات (${excelRows.size} ردیف)",
        style = MaterialTheme.typography.titleMedium
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(excelRows, key = { it.rowNumber }) { row ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
               
Column(
    modifier = Modifier.padding(12.dp)
) {
    Text("ردیف اکسل: ${row.rowNumber}")
    Text("نام خانوادگی: ${row.lastName}")
    Text("نام: ${row.firstName}")
    Text("شماره دانش‌آموزی: ${row.studentNumber}")
    Text("نام پدر: ${row.fatherName}")

    when {
        !row.isValid -> {
            Text(
                text = "وضعیت: ${row.error}",
                color = MaterialTheme.colorScheme.error
            )
        }

        row.studentNumber in duplicateStudentNumbers -> {
            Text(
                text = "هشدار: شماره دانش‌آموزی در فایل تکراری است.",
                color = MaterialTheme.colorScheme.error
            )
        }

        else -> {
            Text(
                text = "وضعیت: معتبر",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    TextButton(
        onClick = {
            excelRows = excelRows.filterNot {
                it.rowNumber == row.rowNumber
            }
        }
    ) {
        Text(
            "حذف از فهرست",
            color = MaterialTheme.colorScheme.error
        )
    }
}


                }
            }
        }
    }
}


                    SelectionDropdown(
                        label = "انتخاب مدرسه",
                        selected = schools.firstOrNull {
                            it.id == studentSchoolId
                        }?.name ?: "مدرسه‌ای وجود ندارد",
                        options = schools.map {
                            it.id to it.name
                        },
                        onSelected = {
                            studentSchoolId = it
                        }
                    )

                    SelectionDropdown(
                        label = "انتخاب کلاس",
                        selected = classesForStudentSchool.firstOrNull {
                            it.id == studentClassId
                        }?.name ?: "کلاسی وجود ندارد",
                        options = classesForStudentSchool.map {
                            it.id to it.name
                        },
                        onSelected = {
                            studentClassId = it
                        }
                    )

                    if (studentScreenMessage != null) {
                        Text(
                            studentScreenMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (studentClassId != null) {

Button(
    onClick = {
        val targetClassId = studentClassId

        if (targetClassId != null) {
            scope.launch {
                try {
                    var addedCount = 0
                    var duplicateCount = 0

                    val validRows = excelRows.filter { it.isValid }

                    validRows.forEach { row ->
                        val result = db.studentDao().insert(
                            Student(
                                classId = targetClassId,
                                lastName = row.lastName,
                                firstName = row.firstName,
                                studentNumber = row.studentNumber,
                                fatherName = row.fatherName
                            )
                        )

                        if (result == -1L) {
                            duplicateCount++
                        } else {
                            addedCount++
                        }
                    }

                    Toast.makeText(
                        context,
                        "$addedCount دانش‌آموز ثبت شد؛ $duplicateCount مورد تکراری بود.",
                        Toast.LENGTH_LONG
                    ).show()

                    excelRows = emptyList()

                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "خطا در ثبت اطلاعات: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    },
    modifier = Modifier.fillMaxWidth(),
    enabled = excelRows.any { it.isValid }
) {
    Text("ثبت دانش‌آموزان معتبر در این کلاس")
}

                        OutlinedTextField(
                            value = studentSearch,
                            onValueChange = {
                                studentSearch = it
                            },
                            label = { Text("جست‌وجوی دانش‌آموز") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("تعداد دانش‌آموزان: ${students.size}")

                        if (filteredStudents.isEmpty()) {
                            EmptyMessage(
                                if (students.isEmpty())
                                    "هنوز دانش‌آموزی در این کلاس ثبت نشده است."
                                else
                                    "دانش‌آموزی با این مشخصات پیدا نشد."
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                filteredStudents,
                                key = { it.id }
                            ) { student ->

                                ListItem(
                                    headlineContent = {
                                        Text(
                                            "${student.firstName} ${student.lastName}"
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            "شماره دانش‌آموزی: ${student.studentNumber}\n" +
                                                "نام پدر: ${student.fatherName}"
                                        )
                                    },
                                    trailingContent = {
                                        Row(
                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    editingStudent = student
                                                    lastName = student.lastName
                                                    firstName = student.firstName
                                                    studentNumber = student.studentNumber
                                                    fatherName = student.fatherName
                                                    studentError = null
                                                    showStudentDialog = true
                                                }
                                            ) {
                                                Text("ویرایش")
                                            }

                                            TextButton(
                                                onClick = {
                                                    studentToDelete = student
                                                }
                                            ) {
                                                Text(
                                                    "حذف",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                )

                                HorizontalDivider()
                            }
                        }
                    } else {
                        EmptyMessage(
                            "برای مشاهده دانش‌آموزان، ابتدا مدرسه و کلاس را انتخاب کن."
                        )
                    }
                }
            }
        }
    }

    // Dialog for adding school, class or lesson

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = {
                Text(
                    when (section) {
                        Section.Schools -> "افزودن مدرسه"
                        Section.Classes -> "افزودن کلاس"
                        Section.Lessons -> "افزودن درس"
                        Section.Students -> "افزودن دانش‌آموز"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = {
                            Text(
                                when (section) {
                                    Section.Schools -> "نام مدرسه"
                                    Section.Classes -> "نام کلاس"
                                    Section.Lessons -> "نام درس"
                                    Section.Students -> "نام دانش‌آموز"
                                }
                            )
                        },
                        singleLine = true
                    )

                    if (section == Section.Classes) {
                        if (schools.isEmpty()) {
                            Text("ابتدا یک مدرسه ثبت کن.")
                        } else {
                            Text("مدرسه مربوطه")

                            schools.forEach { school ->
                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected =
                                            selectedSchoolId == school.id,
                                        onClick = {
                                            selectedSchoolId = school.id
                                        }
                                    )
                                    Text(school.name)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()

                        if (name.isNotEmpty()) {
                            scope.launch {
                                when (section) {
                                    Section.Schools ->
                                        db.schoolDao().insert(
                                            School(name = name)
                                        )

                                    Section.Classes ->
                                        selectedSchoolId?.let {
                                            db.classDao().insert(
                                                SchoolClass(
                                                    schoolId = it,
                                                    name = name
                                                )
                                            )
                                        }

                                    Section.Lessons ->
                                        db.lessonDao().insert(
                                            Lesson(name = name)
                                        )

                                    Section.Students -> Unit
                                }
                            }

                            showAdd = false
                        }
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAdd = false }
                ) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog for adding or editing a student

    if (showStudentDialog) {
        AlertDialog(
            onDismissRequest = {
                showStudentDialog = false
            },
            title = {
                Text(
                    if (editingStudent == null)
                        "افزودن دانش‌آموز"
                    else
                        "ویرایش دانش‌آموز"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("نام خانوادگی") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("نام") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = studentNumber,
                        onValueChange = { studentNumber = it },
                        label = { Text("شماره دانش‌آموزی") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("نام پدر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (studentError != null) {
                        Text(
                            studentError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ln = lastName.trim()
                        val fn = firstName.trim()
                        val sn = studentNumber.trim()
                        val father = fatherName.trim()
                        val classId = studentClassId

                        if (
                            ln.isEmpty() ||
                            fn.isEmpty() ||
                            sn.isEmpty() ||
                            father.isEmpty()
                        ) {
                            studentError = "لطفاً تمام فیلدها را تکمیل کن."
                        } else if (classId == null) {
                            studentError = "ابتدا یک کلاس انتخاب کن."
                        } else if (
                            students.any {
                                it.studentNumber == sn &&
                                    it.id != editingStudent?.id
                            }
                        ) {
                            studentError =
                                "این شماره دانش‌آموزی قبلاً در این کلاس ثبت شده است."
                        } else {
                            scope.launch {
                                try {
                                    if (editingStudent != null) {
                                        db.studentDao().update(
                                            editingStudent!!.copy(
                                                lastName = ln,
                                                firstName = fn,
                                                studentNumber = sn,
                                                fatherName = father
                                            )
                                        )
                                    } else {
                                        val result =
                                            db.studentDao().insert(
                                                Student(
                                                    classId = classId,
                                                    lastName = ln,
                                                    firstName = fn,
                                                    studentNumber = sn,
                                                    fatherName = father
                                                )
                                            )

                                        if (result == -1L) {
                                            studentError =
                                                "این شماره دانش‌آموزی قبلاً ثبت شده است."
                                            return@launch
                                        }
                                    }

                                    showStudentDialog = false
                                } catch (e: Exception) {
                                    studentError =
                                        "ذخیره اطلاعات انجام نشد. دوباره تلاش کن."
                                }
                            }
                        }
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStudentDialog = false
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }

    // Delete confirmation

    if (studentToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                studentToDelete = null
            },
            title = {
                Text("حذف دانش‌آموز")
            },
            text = {
                Text(
                    "آیا از حذف ${studentToDelete!!.firstName} ${studentToDelete!!.lastName} مطمئن هستی؟"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = studentToDelete
                        studentToDelete = null

                        if (item != null) {
                            scope.launch {
                                db.studentDao().delete(item)
                            }
                        }
                    }
                ) {
                    Text(
                        "حذف",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        studentToDelete = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun SelectionDropdown(
    label: String,
    selected: String,
    options: List<Pair<Long, String>>,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label)

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = options.isNotEmpty()
            ) {
                Text(selected)
                Spacer(Modifier.weight(1f))
                Text("⌄")
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

@Composable
private fun EmptyMessage(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
