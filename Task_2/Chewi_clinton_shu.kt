import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// Student data class
data class Student(
    val name: String,
    val scores: List<Int>?
)

// Calculate grade based on average score using a lambda
val getGrade: (Double) -> String = { average ->
    when {
        average >= 90 -> "A"
        average >= 80 -> "B"
        average >= 70 -> "C"
        average >= 60 -> "D"
        else -> "F"
    }
}

// Higher-order function: applies a grading strategy to a student's average
fun evaluateStudent(student: Student, gradingStrategy: (Double) -> String): String {
    val scores = student.scores
    if (scores == null || scores.isEmpty()) return "No scores available"
    val average = scores.average()
    return "${student.name}: Average = ${"%.1f".format(average)}, Grade = ${gradingStrategy(average)}"
}

// Print grades using forEach lambda
fun printGrades(students: List<Student>) {
    students.forEach { student ->
        println(evaluateStudent(student, getGrade))
    }
}

// Filter students by grade using filter and map lambdas
fun filterStudentsByGrade(students: List<Student>, targetGrade: String): List<Student> {
    return students.filter { student ->
        val scores = student.scores
        if (scores == null || scores.isEmpty()) false
        else getGrade(scores.average()) == targetGrade
    }
}

// Get students with valid scores using filter
fun getValidStudents(students: List<Student>): List<Student> {
    return students.filter { it.scores != null && it.scores.isNotEmpty() }
}

// Map students to their averages
fun getStudentAverages(students: List<Student>): List<Pair<String, Double>> {
    return getValidStudents(students).map { student ->
        Pair(student.name, student.scores!!.average())
    }
}

// Calculate class total using fold
fun calculateClassTotal(students: List<Student>): Double {
    return getValidStudents(students).fold(0.0) { acc, student ->
        acc + student.scores!!.average()
    }
}

// Calculate class average using fold and count
fun calculateClassAverage(students: List<Student>): Double {
    val valid = getValidStudents(students)
    if (valid.isEmpty()) return 0.0
    val total = valid.fold(0.0) { acc, student -> acc + student.scores!!.average() }
    return total / valid.size
}

// Sort students by average score using sortedBy/sortedByDescending
fun getStudentsSortedByAverage(students: List<Student>, ascending: Boolean = true): List<Student> {
    val valid = getValidStudents(students)
    return if (ascending) {
        valid.sortedBy { it.scores!!.average() }
    } else {
        valid.sortedByDescending { it.scores!!.average() }
    }
}

// Find top N students using sortedByDescending and take
fun getTopStudents(students: List<Student>, n: Int): List<Student> {
    return getValidStudents(students)
        .sortedByDescending { it.scores!!.average() }
        .take(n)
}

// Find students at risk (below passing average of 60) using filter
fun getAtRiskStudents(students: List<Student>): List<Student> {
    return getValidStudents(students).filter { it.scores!!.average() < 60.0 }
}

// Group students by grade using groupBy
fun groupStudentsByGrade(students: List<Student>): Map<String, List<Student>> {
    return getValidStudents(students).groupBy { student ->
        getGrade(student.scores!!.average())
    }
}

// Print grade distribution using groupBy and mapValues
fun printGradeDistribution(students: List<Student>) {
    val grouped = groupStudentsByGrade(students)
    val gradeOrder = listOf("A", "B", "C", "D", "F")

    println("Grade Distribution:")
    gradeOrder.forEach { grade ->
        val count = grouped[grade]?.size ?: 0
        val bar = "*".repeat(count)
        val names = grouped[grade]?.joinToString(", ") { it.name } ?: "None"
        println("  $grade: $bar ($count) - $names")
    }
}

// Check if all students passed using all
fun allStudentsPassed(students: List<Student>): Boolean {
    return getValidStudents(students).all { it.scores!!.average() >= 60.0 }
}

// Check if any student got an A using any
fun anyStudentGotA(students: List<Student>): Boolean {
    return getValidStudents(students).any { it.scores!!.average() >= 90.0 }
}

// Count students per grade using count
fun countStudentsWithGrade(students: List<Student>, targetGrade: String): Int {
    return getValidStudents(students).count { getGrade(it.scores!!.average()) == targetGrade }
}

// Generate a summary report using multiple collection operations
fun generateSummaryReport(students: List<Student>) {
    val valid = getValidStudents(students)
    if (valid.isEmpty()) {
        println("No valid student data available.")
        return
    }

    println("=== Summary Report ===")
    println("All students passed: ${if (allStudentsPassed(students)) "Yes" else "No"}")
    println("Any student got A: ${if (anyStudentGotA(students)) "Yes" else "No"}")
    println()

    // Use flatMap to get all individual scores across all students
    val allScores = valid.flatMap { it.scores!! }
    println("Total individual scores recorded: ${allScores.size}")
    println("Overall score average: ${"%.1f".format(allScores.average())}")
    println("Highest individual score: ${allScores.max()}")
    println("Lowest individual score: ${allScores.min()}")
    println()

    // Use distinct to find unique grades present
    val uniqueGrades = valid.map { getGrade(it.scores!!.average()) }.distinct().sorted()
    println("Grades present: ${uniqueGrades.joinToString(", ")}")
    println()

    printGradeDistribution(students)
}

// Print class statistics
fun printClassStatistics(students: List<Student>) {
    val valid = getValidStudents(students)
    if (valid.isEmpty()) {
        println("No valid student data available.")
        return
    }

    val classAvg = calculateClassAverage(students)
    val highest = valid.maxByOrNull { it.scores!!.average() }
    val lowest = valid.minByOrNull { it.scores!!.average() }

    println("Class Average: ${"%.1f".format(classAvg)}")
    println("Total Students: ${students.size} (${valid.size} with scores)")
    highest?.let { println("Highest: ${it.name} (${"%.1f".format(it.scores!!.average())})") }
    lowest?.let { println("Lowest: ${it.name} (${"%.1f".format(it.scores!!.average())})") }

    val atRisk = getAtRiskStudents(students)
    if (atRisk.isNotEmpty()) {
        println("At-risk students (avg < 60):")
        atRisk.forEach { println("  - ${it.name}: ${"%.1f".format(it.scores!!.average())}") }
    }
}

// Input a student from the terminal
fun inputStudent(): Student {
    print("Enter student name: ")
    val name = readLine()?.trim() ?: ""

    print("Enter scores separated by commas (e.g. 85,92,78): ")
    val input = readLine()?.trim()

    val scores = if (input.isNullOrEmpty()) {
        null
    } else {
        input.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    return Student(name, scores)
}

// Process an Excel file: read students, calculate grades, write back
fun processExcelFile(inputPath: String): String {
    val file = File(inputPath)
    if (!file.exists() || !file.isFile) {
        println("Error: '$inputPath' is not a valid file.")
        return ""
    }
    if (!file.name.endsWith(".xlsx", ignoreCase = true)) {
        println("Error: File must be a .xlsx Excel file.")
        return ""
    }

    val workbook = try {
        XSSFWorkbook(FileInputStream(file))
    } catch (e: Exception) {
        println("Error: Could not read Excel file - ${e.message}")
        return ""
    }
    val sheet = workbook.getSheetAt(0)

    // Determine the structure: find Name and Marks columns
    val headerRow = sheet.getRow(0)
    if (headerRow == null) {
        println("Error: Excel file is empty.")
        workbook.close()
        return ""
    }

    var nameCol = -1
    var marksStartCol = -1
    var marksEndCol = -1

    // Find columns by header names
    for (i in 0 until headerRow.lastCellNum) {
        val cell = headerRow.getCell(i) ?: continue
        val header = cell.toString().trim().lowercase()
        when {
            header == "name" || header == "student name" || header == "student" -> nameCol = i
            header.contains("mark") || header.contains("score") || header.contains("subject") -> {
                if (marksStartCol == -1) marksStartCol = i
                marksEndCol = i
            }
        }
    }

    // If no marks columns found by header, assume column 0 = name, rest = marks
    if (nameCol == -1) nameCol = 0
    if (marksStartCol == -1) {
        marksStartCol = 1
        marksEndCol = headerRow.lastCellNum.toInt() - 1
    }

    // Add Average and Grade headers
    val avgCol = marksEndCol + 1
    val gradeCol = marksEndCol + 2
    headerRow.createCell(avgCol).setCellValue("Average")
    headerRow.createCell(gradeCol).setCellValue("Grade")

    // Style for headers
    val headerStyle = workbook.createCellStyle()
    val headerFont = workbook.createFont()
    headerFont.bold = true
    headerStyle.setFont(headerFont)
    headerRow.getCell(avgCol).cellStyle = headerStyle
    headerRow.getCell(gradeCol).cellStyle = headerStyle

    val studentsProcessed = mutableListOf<String>()

    // Process each student row
    for (i in 1..sheet.lastRowNum) {
        val row = sheet.getRow(i) ?: continue

        val nameCell = row.getCell(nameCol)
        val studentName = nameCell?.toString()?.trim() ?: "Unknown"

        // Collect marks
        val marks = mutableListOf<Int>()
        for (col in marksStartCol..marksEndCol) {
            val cell = row.getCell(col)
            if (cell != null) {
                val value = when (cell.cellType) {
                    CellType.NUMERIC -> cell.numericCellValue.toInt()
                    CellType.STRING -> cell.toString().trim().toIntOrNull() ?: 0
                    else -> 0
                }
                marks.add(value)
            }
        }

        if (marks.isNotEmpty()) {
            val average = marks.average()
            val grade = getGrade(average)

            row.createCell(avgCol).setCellValue(average)
            row.createCell(gradeCol).setCellValue(grade)

            studentsProcessed.add("$studentName: Average = ${"%.1f".format(average)}, Grade = $grade")
        } else {
            row.createCell(avgCol).setCellValue("N/A")
            row.createCell(gradeCol).setCellValue("N/A")
            studentsProcessed.add("$studentName: No scores available")
        }
    }

    // Auto-size columns
    for (col in 0..gradeCol) {
        sheet.autoSizeColumn(col)
    }

    // Save output file
    val outputFileName = file.nameWithoutExtension + "_graded.xlsx"
    val outputPath = File(file.parent, outputFileName).absolutePath
    val outputStream = FileOutputStream(outputPath)
    workbook.write(outputStream)
    outputStream.close()
    workbook.close()

    println("\n=== Excel Processing Results ===")
    studentsProcessed.forEach { println(it) }
    println("\nGraded file saved to: $outputPath")

    return outputPath
}

// Create a sample Excel file for testing
fun createSampleExcel(): String {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Students")

    // Header style
    val headerStyle = workbook.createCellStyle()
    val headerFont = workbook.createFont()
    headerFont.bold = true
    headerStyle.setFont(headerFont)

    // Create headers
    val headerRow = sheet.createRow(0)
    val headers = listOf("Name", "Math", "Science", "English")
    headers.forEachIndexed { i, header ->
        val cell = headerRow.createCell(i)
        cell.setCellValue(header)
        cell.cellStyle = headerStyle
    }

    // Sample data
    val sampleData = listOf(
        listOf("Alice", 85, 92, 78),
        listOf("Bob", 55, 63, 70),
        listOf("Charlie", 95, 98, 100),
        listOf("Diana", 72, 68, 74)
    )

    sampleData.forEachIndexed { rowIdx, data ->
        val row = sheet.createRow(rowIdx + 1)
        row.createCell(0).setCellValue(data[0] as String)
        for (j in 1 until data.size) {
            row.createCell(j).setCellValue((data[j] as Int).toDouble())
        }
    }

    // Auto-size columns
    headers.indices.forEach { sheet.autoSizeColumn(it) }

    val jarDir = File(object {}.javaClass.protectionDomain.codeSource.location.toURI()).parentFile
    val outputFile = File(jarDir, "sample_students.xlsx")
    val outputStream = FileOutputStream(outputFile)
    workbook.write(outputStream)
    outputStream.close()
    workbook.close()

    println("Sample Excel file created: ${outputFile.absolutePath}")
    return outputFile.absolutePath
}

// Main function with interactive menu
fun main() {
    val students = mutableListOf(
        Student("Alice", listOf(85, 92, 78, 90)),
        Student("Bob", listOf(55, 63, 48, 70)),
        Student("Charlie", null),
        Student("Diana", listOf(95, 98, 100, 92)),
        Student("Eve", listOf(72, 68, 74, 65))
    )

    while (true) {
        println("\n=== Student Grade Calculator ===")
        println("1. View all grades")
        println("2. Add a new student")
        println("3. Upload and process Excel file")
        println("4. Create sample Excel file")
        println("5. Filter students by grade")
        println("6. View student averages")
        println("7. View class statistics")
        println("8. View ranked students")
        println("9. View top students")
        println("10. View grade distribution")
        println("11. Generate summary report")
        println("12. Exit")
        print("Choose an option: ")

        when (readLine()?.trim()) {
            "1" -> {
                println("\n=== Student Grade Report ===")
                printGrades(students)
            }
            "2" -> {
                val student = inputStudent()
                students.add(student)
                println("${student.name} added successfully!")
            }
            "3" -> {
                println("Enter the full path to the Excel file (.xlsx)")
                print("Path: ")
                val path = readLine()?.trim() ?: ""
                if (path.isNotEmpty()) {
                    processExcelFile(path)
                } else {
                    println("No file path provided.")
                }
            }
            "4" -> {
                createSampleExcel()
            }
            "5" -> {
                print("Enter grade to filter by (A, B, C, D, F): ")
                val grade = readLine()?.trim()?.uppercase() ?: ""
                val filtered = filterStudentsByGrade(students, grade)
                if (filtered.isEmpty()) {
                    println("No students found with grade $grade")
                } else {
                    println("\n=== Students with grade $grade ===")
                    filtered.forEach { student ->
                        val avg = student.scores!!.average()
                        println("${student.name}: Average = ${"%.1f".format(avg)}")
                    }
                }
            }
            "6" -> {
                println("\n=== Student Averages ===")
                val averages = getStudentAverages(students)
                averages.forEach { (name, avg) ->
                    println("$name: ${"%.1f".format(avg)}")
                }
            }
            "7" -> {
                println("\n=== Class Statistics ===")
                printClassStatistics(students)
            }
            "8" -> {
                println("\n=== Students Ranked by Average (Highest to Lowest) ===")
                val ranked = getStudentsSortedByAverage(students, ascending = false)
                ranked.forEachIndexed { index, student ->
                    val avg = student.scores!!.average()
                    println("${index + 1}. ${student.name}: ${"%.1f".format(avg)} (${getGrade(avg)})")
                }
            }
            "9" -> {
                print("How many top students to show? ")
                val n = readLine()?.trim()?.toIntOrNull() ?: 3
                val top = getTopStudents(students, n)
                println("\n=== Top $n Students ===")
                top.forEachIndexed { index, student ->
                    val avg = student.scores!!.average()
                    println("${index + 1}. ${student.name}: ${"%.1f".format(avg)} (${getGrade(avg)})")
                }
            }
            "10" -> {
                println()
                printGradeDistribution(students)
            }
            "11" -> {
                println()
                generateSummaryReport(students)
            }
            "12" -> {
                println("Goodbye!")
                return
            }
            else -> println("Invalid option. Please try again.")
        }
    }
}
