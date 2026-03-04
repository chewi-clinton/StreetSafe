// ==========================================
// Task 3: Object-Oriented Grade Calculator
// Demonstrates: Classes, Constructors, Inheritance,
// Abstract Classes, Data Classes, Visibility Modifiers
// ==========================================

// --- Data Class: holds assessment info ---
data class Assessment(
    val subject: String,
    val score: Int,
    val maxScore: Int = 100
) {
    val percentage: Double
        get() = (score.toDouble() / maxScore) * 100
}

// --- Abstract Class: base for all people in the system ---
abstract class Person(val name: String, protected var age: Int) {

    init {
        require(age >= 0) { "Age must be non-negative" }
        require(name.isNotBlank()) { "Name cannot be blank" }
    }

    abstract fun role(): String

    open fun displayInfo() {
        println("Name: $name | Age: $age | Role: ${role()}")
    }

    override fun toString(): String = "$name (${role()}, age $age)"
}

// --- Open class: Student extends Person ---
open class Student(
    name: String,
    age: Int,
    val studentId: String,
    private val assessments: MutableList<Assessment> = mutableListOf()
) : Person(name, age) {

    override fun role(): String = "Student"

    fun addAssessment(assessment: Assessment) {
        assessments.add(assessment)
    }

    fun getAssessments(): List<Assessment> = assessments.toList()

    fun calculateAverage(): Double {
        if (assessments.isEmpty()) return 0.0
        return assessments.map { it.percentage }.average()
    }

    fun getGrade(): String {
        val avg = calculateAverage()
        return when {
            avg >= 90 -> "A"
            avg >= 80 -> "B"
            avg >= 70 -> "C"
            avg >= 60 -> "D"
            else -> "F"
        }
    }

    override fun displayInfo() {
        super.displayInfo()
        println("  Student ID: $studentId")
        if (assessments.isNotEmpty()) {
            println("  Average: ${"%.1f".format(calculateAverage())}% | Grade: ${getGrade()}")
            println("  Assessments: ${assessments.size}")
        } else {
            println("  No assessments recorded")
        }
    }

    override fun toString(): String =
        "$name (Student #$studentId, avg: ${"%.1f".format(calculateAverage())}%)"
}

// --- GraduateStudent inherits from Student (multi-level inheritance) ---
class GraduateStudent(
    name: String,
    age: Int,
    studentId: String,
    val thesisTopic: String,
    private val advisor: String
) : Student(name, age, studentId) {

    override fun role(): String = "Graduate Student"

    override fun displayInfo() {
        super.displayInfo()
        println("  Thesis: $thesisTopic")
        println("  Advisor: $advisor")
    }
}

// --- Teacher extends Person ---
class Teacher(
    name: String,
    age: Int,
    val employeeId: String,
    private val department: String,
    private val courses: MutableList<String> = mutableListOf()
) : Person(name, age) {

    override fun role(): String = "Teacher"

    fun addCourse(course: String) {
        if (course !in courses) courses.add(course)
    }

    fun getCourses(): List<String> = courses.toList()

    override fun displayInfo() {
        super.displayInfo()
        println("  Employee ID: $employeeId")
        println("  Department: $department")
        if (courses.isNotEmpty()) {
            println("  Courses: ${courses.joinToString(", ")}")
        }
    }

    override fun toString(): String = "$name (Teacher, $department)"
}
