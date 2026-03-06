// ==========================================
// Task 3: Object-Oriented Grade Calculator
// Covers: Classes, Constructors, Inheritance,
// Abstract Classes, Data Classes, Interfaces,
// Sealed Classes, Companion Objects, Visibility Modifiers
// ==========================================

// --- Data Class: stores details about a single assessment ---
data class Assessment(
    val subject: String,
    val score: Int,
    val maxScore: Int = 100
) {
    val percentage: Double
        get() = (score.toDouble() / maxScore) * 100
}

// --- Abstract Class: foundation for all individuals in the system ---
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

// --- Open class: Student is a type of Person ---
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

// --- GraduateStudent builds on Student (multi-level inheritance) ---
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

// --- Teacher is also a type of Person ---
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

// --- Interface: Gradable - outlines grading contract ---
interface Gradable {
    fun calculateAverage(): Double
    fun getGrade(): String
    fun isPassing(): Boolean = calculateAverage() >= 60.0
}

// --- Interface: Exportable - outlines export contract ---
interface Exportable {
    fun toCSV(): String
    fun toSummaryString(): String
}

// --- Class that satisfies multiple interfaces ---
class CourseResult(
    val studentName: String,
    val courseName: String,
    private val scores: List<Int>
) : Gradable, Exportable {

    override fun calculateAverage(): Double {
        if (scores.isEmpty()) return 0.0
        return scores.average()
    }

    override fun getGrade(): String {
        val avg = calculateAverage()
        return when {
            avg >= 90 -> "A"
            avg >= 80 -> "B"
            avg >= 70 -> "C"
            avg >= 60 -> "D"
            else -> "F"
        }
    }

    override fun toCSV(): String =
        "$studentName,$courseName,${"%.1f".format(calculateAverage())},${getGrade()}"

    override fun toSummaryString(): String =
        "$studentName - $courseName: ${"%.1f".format(calculateAverage())}% (${getGrade()})"

    override fun toString(): String = toSummaryString()
}

// --- Sealed Class: captures all possible grading outcomes ---
sealed class GradeResult {
    data class Passed(val studentName: String, val grade: String, val average: Double) : GradeResult()
    data class Failed(val studentName: String, val average: Double, val deficit: Double) : GradeResult()
    data class Incomplete(val studentName: String, val reason: String) : GradeResult()
    object NoData : GradeResult()
}

// Handles each possible sealed class outcome exhaustively
fun handleGradeResult(result: GradeResult): String = when (result) {
    is GradeResult.Passed -> "${result.studentName} passed with ${result.grade} (${"%.1f".format(result.average)}%)"
    is GradeResult.Failed -> "${result.studentName} failed (${"%.1f".format(result.average)}%) - needs ${"%.1f".format(result.deficit)} more points"
    is GradeResult.Incomplete -> "${result.studentName}: Incomplete - ${result.reason}"
    GradeResult.NoData -> "No grade data available"
}

// Assesses a student and wraps the outcome in a sealed class
fun evaluateStudent(student: Student): GradeResult {
    if (student.getAssessments().isEmpty()) {
        return GradeResult.Incomplete(student.name, "No assessments submitted")
    }
    val avg = student.calculateAverage()
    return if (avg >= 60.0) {
        GradeResult.Passed(student.name, student.getGrade(), avg)
    } else {
        GradeResult.Failed(student.name, avg, 60.0 - avg)
    }
}

// --- Companion Object: GradeCalculator utility ---
class GradeCalculator private constructor() {
    companion object {
        const val PASSING_THRESHOLD = 60.0
        const val HONOR_ROLL_THRESHOLD = 85.0

        fun fromScore(score: Int, maxScore: Int = 100): String {
            val percentage = (score.toDouble() / maxScore) * 100
            return when {
                percentage >= 90 -> "A"
                percentage >= 80 -> "B"
                percentage >= 70 -> "C"
                percentage >= 60 -> "D"
                else -> "F"
            }
        }

        fun isHonorRoll(average: Double): Boolean = average >= HONOR_ROLL_THRESHOLD

        fun createReport(people: List<Person>): String {
            val sb = StringBuilder()
            sb.appendLine("=== Grade Calculator Report ===")
            sb.appendLine("Total people: ${people.size}")

            val students = people.filterIsInstance<Student>()
            val teachers = people.filterIsInstance<Teacher>()

            sb.appendLine("Students: ${students.size}")
            sb.appendLine("Teachers: ${teachers.size}")

            if (students.isNotEmpty()) {
                val avgOfAll = students.filter { it.getAssessments().isNotEmpty() }
                    .map { it.calculateAverage() }
                    .average()
                sb.appendLine("Class Average: ${"%.1f".format(avgOfAll)}%")
            }

            return sb.toString()
        }
    }
}

// ==========================================
// Main: walks through all OOP features
// ==========================================
fun main() {
    // --- 1. Instantiating objects via constructors ---
    println("=== Creating People ===")

    val student1 = Student("Alice", 20, "STU001")
    val student2 = Student("Bob", 22, "STU002")
    val student3 = Student("Charlie", 19, "STU003")

    val gradStudent = GraduateStudent(
        "Diana", 25, "GRD001",
        thesisTopic = "Machine Learning in Education",
        advisor = "Prof. Smith"
    )

    val teacher = Teacher("Prof. Smith", 45, "TCH001", "Computer Science")
    teacher.addCourse("SE 3242: Android Development")
    teacher.addCourse("CS 101: Intro to Programming")

    // --- 2. Assigning assessments to students ---
    println("\n=== Adding Assessments ===")

    student1.addAssessment(Assessment("Math", 92))
    student1.addAssessment(Assessment("Science", 88))
    student1.addAssessment(Assessment("English", 95))

    student2.addAssessment(Assessment("Math", 45))
    student2.addAssessment(Assessment("Science", 52))
    student2.addAssessment(Assessment("English", 58))

    gradStudent.addAssessment(Assessment("Research Methods", 91))
    gradStudent.addAssessment(Assessment("Advanced AI", 87))
    gradStudent.addAssessment(Assessment("Thesis Draft", 42, 50))

    println("Assessments added for ${student1.name}, ${student2.name}, and ${gradStudent.name}")
    println("${student3.name} has no assessments (will test Incomplete case)")

    // --- 3. Polymorphism: different subclasses grouped in one list ---
    println("\n=== Polymorphism: Displaying All People ===")

    val people: List<Person> = listOf(student1, student2, student3, gradStudent, teacher)

    people.forEach { person ->
        println("---")
        person.displayInfo()
    }

    // --- 4. Showcasing data class capabilities (toString, copy, destructuring) ---
    println("\n=== Data Class Features ===")

    val assessment = Assessment("Physics", 85, 100)
    println("toString: $assessment")

    val adjusted = assessment.copy(score = 90)
    println("copy with adjusted score: $adjusted")

    val (subject, score, maxScore) = assessment
    println("Destructuring: subject=$subject, score=$score, maxScore=$maxScore")

    // --- 5. Sealed class: determining student outcomes ---
    println("\n=== Sealed Class: Grade Evaluation ===")

    val students = listOf(student1, student2, student3, gradStudent)
    val results: List<GradeResult> = students.map { evaluateStudent(it) }

    results.forEach { result ->
        println(handleGradeResult(result))
    }

    // Test NoData case
    println(handleGradeResult(GradeResult.NoData))

    // --- 6. Interfaces: CourseResult satisfying multiple interfaces ---
    println("\n=== Interfaces: CourseResult ===")

    val courseResults = listOf(
        CourseResult("Alice", "Math", listOf(92, 88, 95)),
        CourseResult("Bob", "Math", listOf(45, 52, 58)),
        CourseResult("Diana", "Research", listOf(91, 87, 84))
    )

    println("Summary:")
    courseResults.forEach { println("  ${it.toSummaryString()}") }

    println("\nCSV Export:")
    println("  Name,Course,Average,Grade")
    courseResults.forEach { println("  ${it.toCSV()}") }

    println("\nPassing check (default interface method):")
    courseResults.forEach {
        println("  ${it.studentName}: ${if (it.isPassing()) "Passing" else "Not Passing"}")
    }

    // --- 7. Using the companion object ---
    println("\n=== Companion Object: GradeCalculator ===")

    println("Grade from score 85: ${GradeCalculator.fromScore(85)}")
    println("Grade from score 42/50: ${GradeCalculator.fromScore(42, 50)}")
    println("Passing threshold: ${GradeCalculator.PASSING_THRESHOLD}")
    println("Honor roll threshold: ${GradeCalculator.HONOR_ROLL_THRESHOLD}")

    println("\nHonor roll check:")
    students.filter { it.getAssessments().isNotEmpty() }.forEach { s ->
        val avg = s.calculateAverage()
        println("  ${s.name}: ${"%.1f".format(avg)}% - ${if (GradeCalculator.isHonorRoll(avg)) "Honor Roll" else "Regular"}")
    }

    // --- 8. Generating a report via companion object ---
    println("\n${GradeCalculator.createReport(people)}")

    // --- 9. Polymorphism through overridden toString ---
    println("=== Polymorphism: toString() ===")
    people.forEach { println(it) }
}