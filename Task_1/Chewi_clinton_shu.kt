// Student data class
data class Student(
    val name: String,
    val scores: List<Int>?
)

// Calculate grade based on average score
fun getGrade(average: Double): String {
    return when {
        average >= 90 -> "A"
        average >= 80 -> "B"
        average >= 70 -> "C"
        average >= 60 -> "D"
        else -> "F"
    }
}

fun main() {
    val student = Student("Alice", listOf(85, 92, 78, 90))
    val average = student.scores?.average() ?: 0.0
    val grade = getGrade(average)
    println("${student.name}: Average = ${"%.1f".format(average)}, Grade = $grade")
}
