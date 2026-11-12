
package com.example.teacherassistant

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory

data class ExcelStudentRow(
    val lastName: String,
    val firstName: String,
    val studentNumber: String,
    val fatherName: String,
    val rowNumber: Int,
    val error: String? = null
) {
    val isValid: Boolean
        get() = error == null &&
            lastName.isNotBlank() &&
            firstName.isNotBlank() &&
            studentNumber.isNotBlank() &&
            fatherName.isNotBlank()
}

object ExcelStudentReader {

    fun read(
        context: Context,
        uri: Uri
    ): List<ExcelStudentRow> {

        val result = mutableListOf<ExcelStudentRow>()

        context.contentResolver.openInputStream(uri).use { input ->

            requireNotNull(input) {
                "امکان باز کردن فایل وجود ندارد."
            }

            WorkbookFactory.create(input).use { workbook ->

                require(workbook.numberOfSheets > 0) {
                    "فایل هیچ برگه‌ای ندارد."
                }

                val sheet = workbook.getSheetAt(0)
                val formatter = DataFormatter()

                for (index in 1..sheet.lastRowNum) {

                    val row = sheet.getRow(index)

                    if (row == null) {
                        continue
                    }

                    fun cell(column: Int): String {
                        return formatter.formatCellValue(
                            row.getCell(column)
                        ).trim()
                    }

                    val lastName = cell(0)
                    val firstName = cell(1)
                    val studentNumber = cell(2)
                    val fatherName = cell(3)

                    if (
                        lastName.isBlank() &&
                        firstName.isBlank() &&
                        studentNumber.isBlank() &&
                        fatherName.isBlank()
                    ) {
                        continue
                    }

                    val error = when {
                        lastName.isBlank() -> "نام خانوادگی وارد نشده است."
                        firstName.isBlank() -> "نام وارد نشده است."
                        studentNumber.isBlank() -> "شماره دانش‌آموزی وارد نشده است."
                        fatherName.isBlank() -> "نام پدر وارد نشده است."
                        else -> null
                    }

                    result.add(
                        ExcelStudentRow(
                            lastName = lastName,
                            firstName = firstName,
                            studentNumber = studentNumber,
                            fatherName = fatherName,
                            rowNumber = index + 1,
                            error = error
                        )
                    )
                }
            }
        }

        return result
    }
}
