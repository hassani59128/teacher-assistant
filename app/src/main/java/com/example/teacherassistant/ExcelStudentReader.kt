
package com.example.teacherassistant

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

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

        val entries = mutableMapOf<String, ByteArray>()

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) {
                "امکان باز کردن فایل وجود ندارد."
            }

            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break

                    if (!entry.isDirectory) {
                        val output = ByteArrayOutputStream()
                        zip.copyTo(output)
                        entries[entry.name] = output.toByteArray()
                    }

                    zip.closeEntry()
                }
            }
        }

        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: throw IllegalArgumentException(
                "برگه اول فایل Excel پیدا نشد."
            )

        val sharedStrings = entries["xl/sharedStrings.xml"]
            ?.let { parseSharedStrings(it) }
            ?: emptyList()

        val rows = parseSheet(sheetBytes, sharedStrings)

        return rows
            .filter { it.key > 1 }
            .toSortedMap()
            .mapNotNull { (rowNumber, cells) ->

                val lastName = cells[0].orEmpty().trim()
                val firstName = cells[1].orEmpty().trim()
                val studentNumber = cells[2].orEmpty().trim()
                val fatherName = cells[3].orEmpty().trim()

                if (
                    lastName.isBlank() &&
                    firstName.isBlank() &&
                    studentNumber.isBlank() &&
                    fatherName.isBlank()
                ) {
                    return@mapNotNull null
                }

                val error = when {
                    lastName.isBlank() ->
                        "نام خانوادگی وارد نشده است."

                    firstName.isBlank() ->
                        "نام وارد نشده است."

                    studentNumber.isBlank() ->
                        "شماره دانش‌آموزی وارد نشده است."

                    fatherName.isBlank() ->
                        "نام پدر وارد نشده است."

                    else -> null
                }

                ExcelStudentRow(
                    lastName = lastName,
                    firstName = firstName,
                    studentNumber = studentNumber,
                    fatherName = fatherName,
                    rowNumber = rowNumber,
                    error = error
                )
            }
    }

    private fun parseSharedStrings(
        bytes: ByteArray
    ): List<String> {

        val result = mutableListOf<String>()
        val parser = newParser(bytes)

        var insideString = false
        var currentText = StringBuilder()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {

            when (parser.eventType) {

                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            insideString = true
                            currentText = StringBuilder()
                        }

                        "t" -> {
                            if (insideString) {
                                currentText.append(parser.nextText())
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        result.add(currentText.toString())
                        insideString = false
                    }
                }
            }

            parser.next()
        }

        return result
    }

    private fun parseSheet(
        bytes: ByteArray,
        sharedStrings: List<String>
    ): Map<Int, Map<Int, String>> {

        val result = mutableMapOf<Int, Map<Int, String>>()
        val parser = newParser(bytes)

        var currentRow = 0
        var currentCells = mutableMapOf<Int, String>()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {

            when (parser.eventType) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "row" -> {
                            currentRow =
                                parser.getAttributeValue(null, "r")
                                    ?.toIntOrNull() ?: (currentRow + 1)

                            currentCells = mutableMapOf()
                        }

                        "c" -> {
                            val reference =
                                parser.getAttributeValue(null, "r")
                                    ?: ""

                            val type =
                                parser.getAttributeValue(null, "t")
                                    ?: ""

                            val column = columnIndex(reference)

                            var value = ""

                            var insideCell = true

                            while (
                                insideCell &&
                                parser.next() != XmlPullParser.END_DOCUMENT
                            ) {

                                if (
                                    parser.eventType == XmlPullParser.START_TAG
                                ) {
                                    when (parser.name) {

                                        "v", "t" -> {
                                            value += parser.nextText()
                                        }
                                    }
                                }

                                if (
                                    parser.eventType == XmlPullParser.END_TAG &&
                                    parser.name == "c"
                                ) {
                                    insideCell = false
                                }
                            }

                            val finalValue = when (type) {

                                "s" -> {
                                    sharedStrings.getOrNull(
                                        value.toIntOrNull() ?: -1
                                    ).orEmpty()
                                }

                                else -> value
                            }

                            if (column >= 0) {
                                currentCells[column] = finalValue
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "row") {
                        result[currentRow] = currentCells.toMap()
                    }
                }
            }

            parser.next()
        }

        return result
    }

    private fun columnIndex(reference: String): Int {

        var result = 0

        for (char in reference) {
            if (!char.isLetter()) break

            result = result * 26 +
                (char.uppercaseChar() - 'A' + 1)
        }

        return result - 1
    }

    private fun newParser(bytes: ByteArray): XmlPullParser {

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()

        parser.setInput(
            ByteArrayInputStream(bytes),
            "UTF-8"
        )

        return parser
    }
}
