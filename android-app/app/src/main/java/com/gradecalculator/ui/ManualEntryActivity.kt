package com.gradecalculator.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gradecalculator.R
import com.gradecalculator.databinding.ActivityManualEntryBinding
import com.gradecalculator.model.Student
import com.gradecalculator.model.SubjectScore

class ManualEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManualEntryBinding
    private val subjectViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Add initial subject row
        addSubjectRow()

        binding.btnAddSubject.setOnClickListener { addSubjectRow() }
        binding.btnCalculate.setOnClickListener { calculateResults() }
    }

    private fun addSubjectRow() {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_subject_row, binding.subjectsContainer, false)

        val btnDelete = view.findViewById<TextView>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            binding.subjectsContainer.removeView(view)
            subjectViews.remove(view)
        }

        binding.subjectsContainer.addView(view)
        subjectViews.add(view)
    }

    private fun calculateResults() {
        binding.tvError.visibility = View.GONE

        val studentName = binding.etStudentName.text.toString().trim()
        if (studentName.isEmpty()) {
            showError(getString(R.string.error_empty_name))
            return
        }

        val scores = mutableListOf<SubjectScore>()

        for (view in subjectViews) {
            val subjectName = view.findViewById<EditText>(R.id.etSubjectName)
                .text.toString().trim()
            val scoreText = view.findViewById<EditText>(R.id.etScore)
                .text.toString().trim()

            if (subjectName.isEmpty() || scoreText.isEmpty()) continue

            val score = scoreText.toIntOrNull()
            if (score == null || score < 0 || score > 100) {
                showError(getString(R.string.error_invalid_score))
                return
            }

            scores.add(SubjectScore(subjectName, score))
        }

        if (scores.isEmpty()) {
            showError(getString(R.string.error_no_subjects))
            return
        }

        val student = Student(studentName, scores)

        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("student", student)
        startActivity(intent)
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}
