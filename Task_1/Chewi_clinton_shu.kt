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

// Print grades for a list of students
fun printGrades(students: List<Student>) {
    for (student in students) {
        val scores = student.scores
        if (scores == null || scores.isEmpty()) {
            println("${student.name}: No scores available")
        } else {
            val average = scores.average()
            val grade = getGrade(average)
            println("${student.name}: Average = ${"%.1f".format(average)}, Grade = $grade")
        }
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
    if (!file.exists()) {
        println("Error: File '$inputPath' not found.")
        return ""
    }

    val workbook = XSSFWorkbook(FileInputStream(file))
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
    for (result in studentsProcessed) {
        println(result)
    }
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
    for ((i, header) in headers.withIndex()) {
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

    for ((rowIdx, data) in sampleData.withIndex()) {
        val row = sheet.createRow(rowIdx + 1)
        row.createCell(0).setCellValue(data[0] as String)
        for (j in 1 until data.size) {
            row.createCell(j).setCellValue((data[j] as Int).toDouble())
        }
    }

    // Auto-size columns
    for (i in headers.indices) {
        sheet.autoSizeColumn(i)
    }

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
        println("5. Exit")
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
                println("Goodbye!")
                return
            }
            else -> println("Invalid option. Please try again.")
        }
    }
}
