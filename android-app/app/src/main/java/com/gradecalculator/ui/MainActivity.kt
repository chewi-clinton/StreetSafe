package com.gradecalculator.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gradecalculator.R
import com.gradecalculator.databinding.ActivityMainBinding
import com.gradecalculator.model.Student
import com.gradecalculator.model.SubjectScore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sampleStudents = listOf(
        Student("Alice", mutableListOf(
            SubjectScore("Score 1", 85), SubjectScore("Score 2", 92),
            SubjectScore("Score 3", 78), SubjectScore("Score 4", 90)
        )),
        Student("Bob", mutableListOf(
            SubjectScore("Score 1", 55), SubjectScore("Score 2", 63),
            SubjectScore("Score 3", 48), SubjectScore("Score 4", 70)
        )),
        Student("Charlie"),
        Student("Diana", mutableListOf(
            SubjectScore("Score 1", 95), SubjectScore("Score 2", 98),
            SubjectScore("Score 3", 100), SubjectScore("Score 4", 92)
        )),
        Student("Eve", mutableListOf(
            SubjectScore("Score 1", 72), SubjectScore("Score 2", 68),
            SubjectScore("Score 3", 74), SubjectScore("Score 4", 65)
        ))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStats()
        setupNavigation()
        setupStudentList()
    }

    private fun setupStats() {
        val validStudents = sampleStudents.filter { it.hasScores }
        binding.tvTotalStudents.text = sampleStudents.size.toString()

        if (validStudents.isNotEmpty()) {
            val avg = validStudents.map { it.average }.average()
            binding.tvClassAverage.text = "${"%.1f".format(avg)}%"
        } else {
            binding.tvClassAverage.text = "N/A"
        }
    }

    private fun setupNavigation() {
        binding.cardManualEntry.setOnClickListener {
            startActivity(Intent(this, ManualEntryActivity::class.java))
        }

        binding.cardUploadExcel.setOnClickListener {
            startActivity(Intent(this, ExcelUploadActivity::class.java))
        }
    }

    private fun setupStudentList() {
        for (student in sampleStudents) {
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_student_row, binding.studentListContainer, false)

            view.findViewById<TextView>(R.id.tvStudentName).text = student.name

            val gradeBadge = view.findViewById<TextView>(R.id.tvGradeBadge)
            if (student.hasScores) {
                gradeBadge.text = student.grade
                val badgeBg = GradientDrawable()
                badgeBg.cornerRadius = 12f * resources.displayMetrics.density
                badgeBg.setColor(getGradeColor(student.grade))
                gradeBadge.background = badgeBg

                val scoresText = student.scores.joinToString(", ") { "${it.score}" }
                view.findViewById<TextView>(R.id.tvStudentScores).text = "Scores: $scoresText"
                view.findViewById<TextView>(R.id.tvStudentAvg).text = "${"%.1f".format(student.average)}%"
            } else {
                gradeBadge.text = "-"
                val badgeBg = GradientDrawable()
                badgeBg.cornerRadius = 12f * resources.displayMetrics.density
                badgeBg.setColor(ContextCompat.getColor(this, R.color.text_secondary))
                gradeBadge.background = badgeBg

                view.findViewById<TextView>(R.id.tvStudentScores).text = "No scores"
                view.findViewById<TextView>(R.id.tvStudentAvg).text = "N/A"
            }

            view.setOnClickListener {
                if (student.hasScores) {
                    val intent = Intent(this, ResultsActivity::class.java)
                    intent.putExtra("student", student)
                    startActivity(intent)
                }
            }

            binding.studentListContainer.addView(view)
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
