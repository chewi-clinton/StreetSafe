// ==========================================
// Unit Tests for Grade Calculator
// Tests cover: grade calculation, student model,
// filtering, sorting, statistics, and OOP hierarchy
// ==========================================

// --- Test Data ---
data class TestStudent(
    val name: String,
    val scores: List<Int>?
) {
    val average: Double get() = scores?.average() ?: 0.0
    val hasScores: Boolean get() = scores != null && scores.isNotEmpty()
}

// --- Grade Calculation (mirrors app logic) ---
fun calculateGrade(average: Double): String = when {
    average >= 90 -> "A"
    average >= 80 -> "B"
    average >= 70 -> "C"
    average >= 60 -> "D"
    else -> "F"
}

// --- Test Framework (lightweight, no external dependencies) ---
var totalTests = 0
var passedTests = 0
var failedTests = 0

fun assert(condition: Boolean, testName: String) {
    totalTests++
    if (condition) {
        passedTests++
        println("  PASS: $testName")
    } else {
        failedTests++
        println("  FAIL: $testName")
    }
}

fun <T> assertEquals(expected: T, actual: T, testName: String) {
    totalTests++
    if (expected == actual) {
        passedTests++
        println("  PASS: $testName")
    } else {
        failedTests++
        println("  FAIL: $testName (expected: $expected, got: $actual)")
    }
}

fun assertDoubleEquals(expected: Double, actual: Double, testName: String, tolerance: Double = 0.01) {
    totalTests++
    if (Math.abs(expected - actual) < tolerance) {
        passedTests++
        println("  PASS: $testName")
    } else {
        failedTests++
        println("  FAIL: $testName (expected: $expected, got: $actual)")
    }
}

// ==========================================
// Test Suites
// ==========================================

fun testGradeCalculation() {
    println("\n=== Grade Calculation Tests ===")

    assertEquals("A", calculateGrade(95.0), "95 should be A")
    assertEquals("A", calculateGrade(90.0), "90 should be A (boundary)")
    assertEquals("B", calculateGrade(85.0), "85 should be B")
    assertEquals("B", calculateGrade(80.0), "80 should be B (boundary)")
    assertEquals("C", calculateGrade(75.0), "75 should be C")
    assertEquals("C", calculateGrade(70.0), "70 should be C (boundary)")
    assertEquals("D", calculateGrade(65.0), "65 should be D")
    assertEquals("D", calculateGrade(60.0), "60 should be D (boundary)")
    assertEquals("F", calculateGrade(59.0), "59 should be F")
    assertEquals("F", calculateGrade(0.0), "0 should be F")
    assertEquals("A", calculateGrade(100.0), "100 should be A")
    assertEquals("F", calculateGrade(30.0), "30 should be F")
}

fun testStudentModel() {
    println("\n=== Student Model Tests ===")

    val studentWithScores = TestStudent("Alice", listOf(85, 92, 78, 90))
    assert(studentWithScores.hasScores, "Student with scores should have hasScores = true")
    assertDoubleEquals(86.25, studentWithScores.average, "Alice average should be 86.25")
    assertEquals("Alice", studentWithScores.name, "Student name should be Alice")

    val studentNoScores = TestStudent("Bob", null)
    assert(!studentNoScores.hasScores, "Student with null scores should have hasScores = false")
    assertDoubleEquals(0.0, studentNoScores.average, "Null scores average should be 0.0")

    val studentEmptyScores = TestStudent("Charlie", emptyList())
    assert(!studentEmptyScores.hasScores, "Student with empty scores should have hasScores = false")

    val singleScore = TestStudent("Diana", listOf(75))
    assertDoubleEquals(75.0, singleScore.average, "Single score average should equal the score")
}

fun testDataClassFeatures() {
    println("\n=== Data Class Features Tests ===")

    val s1 = TestStudent("Alice", listOf(85, 90))
    val s2 = TestStudent("Alice", listOf(85, 90))
    val s3 = TestStudent("Bob", listOf(85, 90))

    assert(s1 == s2, "Equal students should be equal (data class equals)")
    assert(s1 != s3, "Different name students should not be equal")
    assert(s1.hashCode() == s2.hashCode(), "Equal students should have same hashCode")

    val copied = s1.copy(name = "Copied")
    assertEquals("Copied", copied.name, "Copied student should have new name")
    assertEquals(s1.scores, copied.scores, "Copied student should retain original scores")

    // Destructuring
    val (name, scores) = s1
    assertEquals("Alice", name, "Destructured name should match")
    assertEquals(listOf(85, 90), scores, "Destructured scores should match")

    // toString
    val str = s1.toString()
    assert(str.contains("Alice"), "toString should contain student name")
}

fun testCollectionOperations() {
    println("\n=== Collection Operations Tests ===")

    val students = listOf(
        TestStudent("Alice", listOf(85, 92, 78, 90)),   // avg 86.25 -> B
        TestStudent("Bob", listOf(55, 63, 48, 70)),     // avg 59.0 -> F
        TestStudent("Charlie", null),                    // no scores
        TestStudent("Diana", listOf(95, 98, 100, 92)),  // avg 96.25 -> A
        TestStudent("Eve", listOf(72, 68, 74, 65))      // avg 69.75 -> D
    )

    // filter: get students with scores
    val validStudents = students.filter { it.hasScores }
    assertEquals(4, validStudents.size, "Should have 4 students with scores")

    // filter: passing students (avg >= 60)
    val passing = validStudents.filter { it.average >= 60.0 }
    assertEquals(3, passing.size, "Should have 3 passing students")

    // map: get averages
    val averages = validStudents.map { it.average }
    assertEquals(4, averages.size, "Should have 4 averages")

    // sortedBy: sort by average
    val sorted = validStudents.sortedBy { it.average }
    assertEquals("Bob", sorted.first().name, "Lowest average should be Bob")
    assertEquals("Diana", sorted.last().name, "Highest average should be Diana")

    // sortedByDescending
    val sortedDesc = validStudents.sortedByDescending { it.average }
    assertEquals("Diana", sortedDesc.first().name, "Highest should be first in desc sort")

    // maxByOrNull / minByOrNull
    val top = validStudents.maxByOrNull { it.average }
    assertEquals("Diana", top?.name, "Top student should be Diana")
    val bottom = validStudents.minByOrNull { it.average }
    assertEquals("Bob", bottom?.name, "Bottom student should be Bob")

    // count
    val aCount = validStudents.count { calculateGrade(it.average) == "A" }
    assertEquals(1, aCount, "Should have 1 student with grade A")

    // any / all
    val anyA = validStudents.any { calculateGrade(it.average) == "A" }
    assert(anyA, "Should have at least one A student")
    val allPassing = validStudents.all { it.average >= 60.0 }
    assert(!allPassing, "Not all students should be passing")

    // fold: calculate class total
    val classTotal = validStudents.fold(0.0) { acc, s -> acc + s.average }
    assertDoubleEquals(311.25, classTotal, "Class total should be 311.25")

    // groupBy: group by grade
    val grouped = validStudents.groupBy { calculateGrade(it.average) }
    assertEquals(1, grouped["A"]?.size ?: 0, "Should have 1 A student")
    assertEquals(1, grouped["B"]?.size ?: 0, "Should have 1 B student")
    assertEquals(1, grouped["F"]?.size ?: 0, "Should have 1 F student")

    // flatMap: get all individual scores
    val allScores = validStudents.flatMap { it.scores!! }
    assertEquals(16, allScores.size, "Should have 16 total individual scores")

    // take: top 2 students
    val top2 = sortedDesc.take(2)
    assertEquals(2, top2.size, "Should get exactly 2 students")
    assertEquals("Diana", top2[0].name, "First top student should be Diana")
    assertEquals("Alice", top2[1].name, "Second top student should be Alice")

    // distinct: unique grades
    val grades = validStudents.map { calculateGrade(it.average) }.distinct().sorted()
    assertEquals(listOf("A", "B", "D", "F"), grades, "Should have grades A, B, D, F")
}

fun testHigherOrderFunctions() {
    println("\n=== Higher-Order Function Tests ===")

    // Lambda as variable
    val gradeFunction: (Double) -> String = { avg ->
        when {
            avg >= 90 -> "A"
            avg >= 80 -> "B"
            avg >= 70 -> "C"
            avg >= 60 -> "D"
            else -> "F"
        }
    }

    assertEquals("A", gradeFunction(95.0), "Lambda grade function should return A for 95")
    assertEquals("F", gradeFunction(50.0), "Lambda grade function should return F for 50")

    // Higher-order function accepting strategy
    fun evaluate(scores: List<Int>, strategy: (Double) -> String): String {
        return strategy(scores.average())
    }

    assertEquals("B", evaluate(listOf(85, 80, 82), gradeFunction), "Evaluate with strategy should return B")

    // Custom higher-order function with transformation
    fun <T> transformStudents(
        students: List<TestStudent>,
        transform: (TestStudent) -> T
    ): List<T> = students.map(transform)

    val students = listOf(
        TestStudent("Alice", listOf(90, 95)),
        TestStudent("Bob", listOf(60, 70))
    )

    val names = transformStudents(students) { it.name }
    assertEquals(listOf("Alice", "Bob"), names, "Transform should extract names")

    val grades = transformStudents(students) { calculateGrade(it.average) }
    assertEquals(listOf("A", "D"), grades, "Transform should calculate grades")
}

fun testEdgeCases() {
    println("\n=== Edge Case Tests ===")

    // Perfect score
    assertEquals("A", calculateGrade(100.0), "Perfect score should be A")

    // Zero score
    assertEquals("F", calculateGrade(0.0), "Zero score should be F")

    // Boundary values
    assertEquals("A", calculateGrade(90.0), "Exactly 90 should be A")
    assertEquals("B", calculateGrade(89.99), "89.99 should be B")
    assertEquals("D", calculateGrade(60.0), "Exactly 60 should be D")
    assertEquals("F", calculateGrade(59.99), "59.99 should be F")

    // Single score student
    val single = TestStudent("Solo", listOf(88))
    assertDoubleEquals(88.0, single.average, "Single score average should be the score itself")

    // Very large scores list
    val manyScores = (1..100).toList()
    val bigStudent = TestStudent("Many", manyScores)
    assertDoubleEquals(50.5, bigStudent.average, "Average of 1-100 should be 50.5")

    // All same scores
    val sameScores = TestStudent("Same", listOf(80, 80, 80, 80))
    assertDoubleEquals(80.0, sameScores.average, "All same scores should average to that score")
}

// ==========================================
// Main Test Runner
// ==========================================
fun main() {
    println("==========================================")
    println("  Grade Calculator Test Suite")
    println("==========================================")

    testGradeCalculation()
    testStudentModel()
    testDataClassFeatures()
    testCollectionOperations()
    testHigherOrderFunctions()
    testEdgeCases()

    println("\n==========================================")
    println("  Results: $passedTests/$totalTests passed, $failedTests failed")
    println("==========================================")

    if (failedTests > 0) {
        println("  Some tests FAILED!")
    } else {
        println("  All tests PASSED!")
    }
}
