package com.gradecalculator.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gradecalculator.R
import com.gradecalculator.databinding.ActivityExcelUploadBinding
import com.gradecalculator.model.Student
import com.gradecalculator.model.SubjectScore
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ExcelUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExcelUploadBinding
    private var selectedFileUri: Uri? = null
    private val processedStudents = mutableListOf<Student>()

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                showSelectedFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExcelUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.uploadZone.setOnClickListener { openFilePicker() }

        binding.btnCalculateGrades.setOnClickListener {
            selectedFileUri?.let { processExcelFile(it) }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }
        filePicker.launch(intent)
    }

    private fun showSelectedFile(uri: Uri) {
        val fileName = uri.lastPathSegment ?: "Selected file"
        binding.tvSelectedFile.text = fileName
        binding.selectedFileContainer.visibility = View.VISIBLE
        binding.btnCalculateGrades.isEnabled = true
        binding.btnCalculateGrades.alpha = 1.0f
    }

    private fun processExcelFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot open file")

            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0)
                ?: throw Exception("Excel file is empty")

            var nameCol = -1
            var marksStartCol = -1
            var marksEndCol = -1

            for (i in 0 until headerRow.lastCellNum) {
                val cell = headerRow.getCell(i) ?: continue
                val header = cell.toString().trim().lowercase()
                when {
                    header == "name" || header == "student name" || header == "student" -> nameCol = i
                    header.contains("mark") || header.contains("score") || header.contains("subject") ||
                    header.contains("math") || header.contains("science") || header.contains("english") -> {
                        if (marksStartCol == -1) marksStartCol = i
                        marksEndCol = i
                    }
                }
            }

            if (nameCol == -1) nameCol = 0
            if (marksStartCol == -1) {
                marksStartCol = 1
                marksEndCol = headerRow.lastCellNum.toInt() - 1
            }

            processedStudents.clear()
            val sb = StringBuilder()

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val studentName = row.getCell(nameCol)?.toString()?.trim() ?: "Unknown"

                val scores = mutableListOf<SubjectScore>()
                for (col in marksStartCol..marksEndCol) {
                    val cell = row.getCell(col)
                    val subjectName = headerRow.getCell(col)?.toString()?.trim() ?: "Subject $col"
                    if (cell != null) {
                        val value = when (cell.cellType) {
                            CellType.NUMERIC -> cell.numericCellValue.toInt()
                            CellType.STRING -> cell.toString().trim().toIntOrNull() ?: 0
                            else -> 0
                        }
                        scores.add(SubjectScore(subjectName, value))
                    }
                }

                val student = Student(studentName, scores)
                processedStudents.add(student)

                if (student.hasScores) {
                    sb.appendLine("${student.name}: Avg = ${"%.1f".format(student.average)}, Grade = ${student.grade}")
                } else {
                    sb.appendLine("${student.name}: No scores")
                }
            }

            workbook.close()
            inputStream.close()

            sb.appendLine("\n${processedStudents.size} students processed.")

            binding.tvResults.text = sb.toString()
            binding.resultsCard.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
