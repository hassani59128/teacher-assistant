
package com.example.teacherassistant

import android.content.Context
import android.net.Uri
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelTemplateWriter {

    fun write(context: Context, uri: Uri) {

        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("امکان ذخیره فایل وجود ندارد.")

        ZipOutputStream(output).use { zip ->

            addFile(zip, "[Content_Types].xml", contentTypes)
            addFile(zip, "_rels/.rels", rootRelationships)
            addFile(zip, "xl/workbook.xml", workbook)
            addFile(zip, "xl/_rels/workbook.xml.rels", workbookRelationships)
            addFile(zip, "xl/styles.xml", styles)
            addFile(zip, "xl/worksheets/sheet1.xml", studentsSheet)
            addFile(zip, "xl/worksheets/sheet2.xml", guideSheet)
        }
    }

    private fun addFile(
        zip: ZipOutputStream,
        name: String,
        content: String
    ) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private val contentTypes = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
        <Default Extension="xml" ContentType="application/xml"/>
        <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
        <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        </Types>
    """.trimIndent()

    private val rootRelationships = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private val workbook = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
        <sheets>
        <sheet name="دانش‌آموزان" sheetId="1" r:id="rId1"/>
        <sheet name="راهنما" sheetId="2" r:id="rId2"/>
        </sheets>
        </workbook>
    """.trimIndent()

    private val workbookRelationships = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
        <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
        <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private val styles = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
        <fonts count="1"><font><sz val="11"/><name val="Arial"/></font></fonts>
        <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
        <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
        <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
        <cellXfs count="2">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="49" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
        </cellXfs>
        <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
    """.trimIndent()

    private val studentsSheet = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
        <sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
        <sheetFormatPr defaultRowHeight="22"/>
        <cols>
        <col min="1" max="1" width="25" customWidth="1"/>
        <col min="2" max="2" width="20" customWidth="1"/>
        <col min="3" max="3" width="24" customWidth="1" style="1"/>
        <col min="4" max="4" width="22" customWidth="1"/>
        </cols>
        <sheetData>
        <row r="1">
        <c r="A1" t="inlineStr"><is><t>نام خانوادگی</t></is></c>
        <c r="B1" t="inlineStr"><is><t>نام</t></is></c>
        <c r="C1" t="inlineStr" s="1"><is><t>شماره دانش‌آموزی</t></is></c>
        <c r="D1" t="inlineStr"><is><t>نام پدر</t></is></c>
        </row>
        </sheetData>
        </worksheet>
    """.trimIndent()

    private val guideSheet = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
        <sheetFormatPr defaultRowHeight="25"/>
        <cols><col min="1" max="1" width="90" customWidth="1"/></cols>
        <sheetData>
        <row r="1"><c r="A1" t="inlineStr"><is><t>راهنمای تکمیل فایل دانش‌آموزان</t></is></c></row>
        <row r="2"><c r="A2" t="inlineStr"><is><t>۱. اطلاعات دانش‌آموزان را در برگه دانش‌آموزان وارد کنید.</t></is></c></row>
        <row r="3"><c r="A3" t="inlineStr"><is><t>۲. ترتیب ستون‌ها را تغییر ندهید.</t></is></c></row>
        <row r="4"><c r="A4" t="inlineStr"><is><t>۳. نام خانوادگی، نام، شماره دانش‌آموزی و نام پدر الزامی هستند.</t></is></c></row>
        <row r="5"><c r="A5" t="inlineStr"><is><t>۴. شماره دانش‌آموزی را به‌صورت متن وارد کنید تا صفرهای ابتدایی حفظ شوند.</t></is></c></row>
        <row r="6"><c r="A6" t="inlineStr"><is><t>۵. شماره دانش‌آموزی نباید در یک کلاس تکراری باشد.</t></is></c></row>
        <row r="7"><c r="A7" t="inlineStr"><is><t>۶. ردیف‌های خالی نادیده گرفته می‌شوند.</t></is></c></row>
        <row r="8"><c r="A8" t="inlineStr"><is><t>۷. بعد از تکمیل فایل، آن را با فرمت XLSX ذخیره کنید.</t></is></c></row>
        </sheetData>
        </worksheet>
    """.trimIndent()
}
