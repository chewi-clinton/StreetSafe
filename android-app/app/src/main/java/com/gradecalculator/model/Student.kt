package com.gradecalculator.model

import java.io.Serializable

data class Student(
    val name: String,
    val scores: MutableList<SubjectScore> = mutableListOf()
) : Serializable {

    val average: Double
        get() = if (scores.isEmpty()) 0.0 else scores.map { it.score.toDouble() }.average()

    val grade: String
        get() = calculateGrade(average)

    val hasScores: Boolean
        get() = scores.isNotEmpty()

    val isPassing: Boolean
        get() = average >= 60.0

    companion object {
        fun calculateGrade(average: Double): String = when {
            average >= 90 -> "A"
            average >= 80 -> "B"
            average >= 70 -> "C"
            average >= 60 -> "D"
            else -> "F"
        }
    }
}

data class SubjectScore(
    val subject: String,
    val score: Int
) : Serializable
