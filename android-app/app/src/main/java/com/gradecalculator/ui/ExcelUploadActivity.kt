package com.gradecalculator.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import java.io.File
import java.io.FileOutputStream

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

        binding.btnGenerateSample.setOnClickListener { generateSampleExcel() }
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

            // Find the name column (first column or one labeled name/student)
            var nameCol = -1
            for (i in 0 until headerRow.lastCellNum) {
                val cell = headerRow.getCell(i) ?: continue
                val header = cell.toString().trim().lowercase()
                if (header == "name" || header == "student name" || header == "student") {
                    nameCol = i
                    break
                }
            }
            if (nameCol == -1) nameCol = 0

            // All other numeric columns are scores
            val scoreCols = mutableListOf<Int>()
            for (i in 0 until headerRow.lastCellNum) {
                if (i != nameCol) scoreCols.add(i)
            }

            processedStudents.clear()
            val sb = StringBuilder()

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val studentName = row.getCell(nameCol)?.toString()?.trim() ?: "Unknown"

                val scores = mutableListOf<SubjectScore>()
                for ((scoreIndex, col) in scoreCols.withIndex()) {
                    val cell = row.getCell(col)
                    if (cell != null) {
                        val value = when (cell.cellType) {
                            CellType.NUMERIC -> cell.numericCellValue.toInt()
                            CellType.STRING -> cell.toString().trim().toIntOrNull() ?: 0
                            else -> 0
                        }
                        scores.add(SubjectScore("Score ${scoreIndex + 1}", value))
                    }
                }

                val student = Student(studentName, scores)
                processedStudents.add(student)

                if (student.hasScores) {
                    val scoresStr = student.scores.joinToString(", ") { "${it.score}" }
                    sb.appendLine("${student.name}: [$scoresStr] Avg = ${"%.1f".format(student.average)}, Grade = ${student.grade}")
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

    private fun generateSampleExcel() {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Students")

            // Header row: Name, Score 1, Score 2, Score 3, Score 4, Score 5
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Name")
            for (i in 1..5) {
                headerRow.createCell(i).setCellValue("Score $i")
            }

            // Sample student data
            val sampleData = listOf(
                arrayOf("Alice Johnson", 92, 88, 95, 78, 90),
                arrayOf("Bob Smith", 75, 82, 68, 71, 80),
                arrayOf("Carol White", 65, 58, 72, 60, 55),
                arrayOf("David Brown", 88, 91, 85, 93, 87),
                arrayOf("Eva Martinez", 45, 52, 38, 60, 48)
            )

            for ((index, data) in sampleData.withIndex()) {
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(data[0] as String)
                for (i in 1..5) {
                    row.createCell(i).setCellValue((data[i] as Int).toDouble())
                }
            }

            // Save to Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "sample_students.xlsx")
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            outputStream.close()
            workbook.close()

            Toast.makeText(this, "Sample saved to Downloads/sample_students.xlsx", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error creating sample: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
