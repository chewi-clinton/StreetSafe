package com.gradecalculator.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gradecalculator.R
import com.gradecalculator.databinding.ActivityResultsBinding
import com.gradecalculator.model.Student
import java.io.File
import java.io.FileOutputStream

class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding
    private var currentStudent: Student? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        @Suppress("DEPRECATION")
        val student = intent.getSerializableExtra("student") as? Student
        if (student == null) {
            Toast.makeText(this, "No student data received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentStudent = student
        displayResults(student)

        binding.btnDownload.setOnClickListener {
            currentStudent?.let { downloadPdf(it) }
        }
    }

    private fun displayResults(student: Student) {
        binding.tvStudentName.text = student.name
        binding.tvGrade.text = student.grade
        binding.tvAverage.text = "Average: ${"%.1f".format(student.average)}%"

        val gradeColor = getGradeColor(student.grade)
        binding.tvGrade.setTextColor(gradeColor)

        if (student.isPassing) {
            binding.tvPassFail.text = "PASSING"
            val bg = GradientDrawable()
            bg.cornerRadius = 24f * resources.displayMetrics.density
            bg.setColor(ContextCompat.getColor(this, R.color.success_green))
            binding.tvPassFail.background = bg
        } else {
            binding.tvPassFail.text = "FAILING"
            val bg = GradientDrawable()
            bg.cornerRadius = 24f * resources.displayMetrics.density
            bg.setColor(ContextCompat.getColor(this, R.color.error_red))
            binding.tvPassFail.background = bg
        }

        student.scores.forEachIndexed { index, subjectScore ->
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_subject_result, binding.subjectsBreakdown, false)

            view.findViewById<TextView>(R.id.tvSubjectName).text = "Score ${index + 1}"
            view.findViewById<TextView>(R.id.tvSubjectScore).text = "${subjectScore.score}/100"

            val gradeBadge = view.findViewById<TextView>(R.id.tvSubjectGrade)
            val scoreGrade = Student.calculateGrade(subjectScore.score.toDouble())
            gradeBadge.text = scoreGrade

            val badgeBg = GradientDrawable()
            badgeBg.cornerRadius = 12f * resources.displayMetrics.density
            badgeBg.setColor(getGradeColor(scoreGrade))
            gradeBadge.background = badgeBg

            binding.subjectsBreakdown.addView(view)
        }
    }

    private fun downloadPdf(student: Student) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#6A4DF6")
                textSize = 28f
                isFakeBoldText = true
            }

            val headerPaint = Paint().apply {
                color = Color.parseColor("#2D3142")
                textSize = 20f
                isFakeBoldText = true
            }

            val bodyPaint = Paint().apply {
                color = Color.parseColor("#2D3142")
                textSize = 16f
            }

            val subtextPaint = Paint().apply {
                color = Color.parseColor("#9094A6")
                textSize = 14f
            }

            var y = 60f

            canvas.drawText("Grade Calculator Report", 40f, y, titlePaint)
            y += 40f

            val linePaint = Paint().apply { color = Color.parseColor("#EBE7FF"); strokeWidth = 2f }
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 30f

            canvas.drawText("Student: ${student.name}", 40f, y, headerPaint)
            y += 30f
            canvas.drawText("Average: ${"%.1f".format(student.average)}%", 40f, y, bodyPaint)
            y += 25f
            canvas.drawText("Grade: ${student.grade}", 40f, y, bodyPaint)
            y += 25f
            canvas.drawText("Status: ${if (student.isPassing) "PASSING" else "FAILING"}", 40f, y, bodyPaint)
            y += 40f

            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 30f

            canvas.drawText("Score Breakdown", 40f, y, headerPaint)
            y += 30f

            student.scores.forEachIndexed { index, score ->
                val scoreGrade = Student.calculateGrade(score.score.toDouble())
                canvas.drawText("Score ${index + 1}:", 40f, y, bodyPaint)
                canvas.drawText("${score.score}/100", 200f, y, bodyPaint)
                canvas.drawText("(${scoreGrade})", 300f, y, subtextPaint)
                y += 25f
            }

            document.finishPage(page)

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "${student.name.replace(" ", "_")}_report.pdf"
            val file = File(downloadsDir, fileName)
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()

            Toast.makeText(this, "PDF saved to Downloads/$fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getGradeColor(grade: String): Int {
        val colorRes = when (grade) {
            "A" -> R.color.grade_a
            "B" -> R.color.grade_b
            "C" -> R.color.grade_c
            "D" -> R.color.grade_d
            else -> R.color.grade_f
        }
        return ContextCompat.getColor(this, colorRes)
    }
}
