package com.gradecalculator.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
}
