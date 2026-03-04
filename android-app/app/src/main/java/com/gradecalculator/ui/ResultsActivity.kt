package com.gradecalculator.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gradecalculator.R
import com.gradecalculator.databinding.ActivityResultsBinding
import com.gradecalculator.model.Student

class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding

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

        displayResults(student)

        binding.btnDownload.setOnClickListener {
            Toast.makeText(this, "Report download coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayResults(student: Student) {
        binding.tvStudentName.text = student.name
        binding.tvGrade.text = student.grade
        binding.tvAverage.text = "Average: ${"%.1f".format(student.average)}%"

        // Grade color
        val gradeColor = getGradeColor(student.grade)
        binding.tvGrade.setTextColor(gradeColor)

        // Pass/Fail badge
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

        // Subject breakdown
        student.scores.forEach { subjectScore ->
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_subject_result, binding.subjectsBreakdown, false)

            view.findViewById<TextView>(R.id.tvSubjectName).text = subjectScore.subject
            view.findViewById<TextView>(R.id.tvSubjectScore).text = "Score: ${subjectScore.score}/100"

            val gradeBadge = view.findViewById<TextView>(R.id.tvSubjectGrade)
            val subjectGrade = Student.calculateGrade(subjectScore.score.toDouble())
            gradeBadge.text = subjectGrade

            val badgeBg = GradientDrawable()
            badgeBg.cornerRadius = 12f * resources.displayMetrics.density
            badgeBg.setColor(getGradeColor(subjectGrade))
            gradeBadge.background = badgeBg

            binding.subjectsBreakdown.addView(view)
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
