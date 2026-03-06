import com.formdev.flatlaf.FlatDarkLaf
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import java.awt.*
import java.awt.event.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.JTableHeader

// ==================== COLORS & THEME ====================
object AppColors {
    val purplePrimary = Color(128, 0, 128)
    val purpleLight = Color(186, 85, 211)
    val purpleDark = Color(75, 0, 130)
    val purpleAccent = Color(155, 89, 182)
    val background = Color(25, 25, 35)
    val surface = Color(35, 35, 50)
    val surfaceLight = Color(45, 45, 65)
    val cardBg = Color(40, 40, 58)
    val textPrimary = Color(240, 240, 245)
    val textSecondary = Color(180, 180, 195)
    val success = Color(46, 204, 113)
    val warning = Color(241, 196, 15)
    val danger = Color(231, 76, 60)
    val gradeA = Color(46, 204, 113)
    val gradeB = Color(52, 152, 219)
    val gradeC = Color(241, 196, 15)
    val gradeD = Color(230, 126, 34)
    val gradeF = Color(231, 76, 60)
}

// ==================== DATA MODEL ====================
data class Student(val name: String, val scores: List<Int>?) {
    val average: Double get() = scores?.average() ?: 0.0
    val grade: String get() = getGrade(average)
    val hasScores: Boolean get() = scores != null && scores.isNotEmpty()
}

fun getGrade(average: Double): String = when {
    average >= 90 -> "A"
    average >= 80 -> "B"
    average >= 70 -> "C"
    average >= 60 -> "D"
    else -> "F"
}

fun getGradeColor(grade: String): Color = when (grade) {
    "A" -> AppColors.gradeA
    "B" -> AppColors.gradeB
    "C" -> AppColors.gradeC
    "D" -> AppColors.gradeD
    else -> AppColors.gradeF
}

// ==================== CUSTOM COMPONENTS ====================

class RoundedPanel(
    private val cornerRadius: Int = 20,
    private val bgColor: Color = AppColors.cardBg
) : JPanel() {
    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = bgColor
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius)
        g2.dispose()
        super.paintComponent(g)
    }
}

class PurpleButton(text: String, private val isPrimary: Boolean = true) : JButton(text) {
    init {
        font = Font("SansSerif", Font.BOLD, 14)
        foreground = Color.WHITE
        background = if (isPrimary) AppColors.purplePrimary else AppColors.surfaceLight
        isFocusPainted = false
        isBorderPainted = false
        isContentAreaFilled = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        preferredSize = Dimension(preferredSize.width.coerceAtLeast(140), 42)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.color = if (model.isRollover) {
            if (isPrimary) AppColors.purpleLight else AppColors.purpleAccent
        } else {
            background
        }
        g2.fillRoundRect(0, 0, width, height, 12, 12)
        g2.dispose()
        super.paintComponent(g)
    }
}

class StyledTextField(columns: Int = 20) : JTextField(columns) {
    init {
        font = Font("SansSerif", Font.PLAIN, 14)
        foreground = AppColors.textPrimary
        background = AppColors.surfaceLight
        caretColor = AppColors.purpleLight
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppColors.purpleAccent, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        )
        preferredSize = Dimension(preferredSize.width, 40)
    }
}

// ==================== SIDEBAR ====================
class Sidebar(private val onNavigate: (String) -> Unit) : JPanel() {
    private var activeItem = "dashboard"

    init {
        preferredSize = Dimension(220, 0)
        background = AppColors.surface
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = EmptyBorder(20, 0, 20, 0)

        // App title
        val titlePanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
            background = AppColors.surface
            maximumSize = Dimension(220, 80)
            val icon = JLabel("\u2b23").apply {
                font = Font("SansSerif", Font.BOLD, 28)
                foreground = AppColors.purpleLight
            }
            val title = JLabel("Grade Calc").apply {
                font = Font("SansSerif", Font.BOLD, 18)
                foreground = AppColors.textPrimary
            }
            add(icon)
            add(title)
        }
        add(titlePanel)
        add(Box.createVerticalStrut(30))

        addNavItem("dashboard", "\u25a3  Dashboard")
        addNavItem("students", "\u263a  Students")
        addNavItem("add", "\u002b  Add Student")
        addNavItem("excel", "\u2b06  Excel Import")

        add(Box.createVerticalGlue())
    }

    private fun addNavItem(id: String, label: String) {
        val item = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                if (activeItem == id) {
                    g2.color = AppColors.purpleDark
                    g2.fillRoundRect(8, 2, width - 16, height - 4, 10, 10)
                }
                g2.dispose()
                super.paintComponent(g)
            }
        }.apply {
            isOpaque = false
            layout = FlowLayout(FlowLayout.LEFT, 20, 10)
            maximumSize = Dimension(220, 48)
            preferredSize = Dimension(220, 48)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            val lbl = JLabel(label).apply {
                font = Font("SansSerif", if (activeItem == id) Font.BOLD else Font.PLAIN, 14)
                foreground = if (activeItem == id) AppColors.purpleLight else AppColors.textSecondary
            }
            add(lbl)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    activeItem = id
                    onNavigate(id)
                    // Refresh sidebar
                    this@Sidebar.removeAll()
                    this@Sidebar.revalidate()
                    this@Sidebar.repaint()
                    // Rebuild sidebar
                    rebuildSidebar()
                }
            })
        }
        add(item)
    }

    private fun rebuildSidebar() {
        removeAll()
        border = EmptyBorder(20, 0, 20, 0)

        val titlePanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
            background = AppColors.surface
            maximumSize = Dimension(220, 80)
            add(JLabel("\u2b23").apply {
                font = Font("SansSerif", Font.BOLD, 28)
                foreground = AppColors.purpleLight
            })
            add(JLabel("Grade Calc").apply {
                font = Font("SansSerif", Font.BOLD, 18)
                foreground = AppColors.textPrimary
            })
        }
        add(titlePanel)
        add(Box.createVerticalStrut(30))

        addNavItem("dashboard", "\u25a3  Dashboard")
        addNavItem("students", "\u263a  Students")
        addNavItem("add", "\u002b  Add Student")
        addNavItem("excel", "\u2b06  Excel Import")

        add(Box.createVerticalGlue())
        revalidate()
        repaint()
    }
}

// ==================== DASHBOARD PANEL ====================
class DashboardPanel(private val students: List<Student>) : JPanel() {
    init {
        background = AppColors.background
        layout = BorderLayout(20, 20)
        border = EmptyBorder(30, 30, 30, 30)

        // Header
        val header = JLabel("Dashboard").apply {
            font = Font("SansSerif", Font.BOLD, 28)
            foreground = AppColors.textPrimary
        }
        add(header, BorderLayout.NORTH)

        // Stats cards
        val statsPanel = JPanel(GridLayout(1, 4, 15, 0)).apply {
            isOpaque = false
        }

        val totalStudents = students.size
        val avgScore = if (students.any { it.hasScores }) {
            students.filter { it.hasScores }.map { it.average }.average()
        } else 0.0
        val passingCount = students.count { it.hasScores && it.average >= 60 }
        val topGradeCount = students.count { it.hasScores && it.grade == "A" }

        statsPanel.add(createStatCard("Total Students", "$totalStudents", AppColors.purplePrimary))
        statsPanel.add(createStatCard("Class Average", "${"%.1f".format(avgScore)}%", AppColors.gradeB))
        statsPanel.add(createStatCard("Passing", "$passingCount / $totalStudents", AppColors.success))
        statsPanel.add(createStatCard("Top Grade (A)", "$topGradeCount", AppColors.gradeA))

        // Grade distribution
        val centerPanel = JPanel(BorderLayout(0, 20)).apply { isOpaque = false }
        centerPanel.add(statsPanel, BorderLayout.NORTH)

        val distPanel = createGradeDistribution()
        centerPanel.add(distPanel, BorderLayout.CENTER)

        add(centerPanel, BorderLayout.CENTER)
    }

    private fun createStatCard(title: String, value: String, accentColor: Color): RoundedPanel {
        return RoundedPanel().apply {
            layout = BorderLayout(0, 8)
            border = EmptyBorder(20, 20, 20, 20)
            preferredSize = Dimension(0, 120)

            add(JLabel(title).apply {
                font = Font("SansSerif", Font.PLAIN, 13)
                foreground = AppColors.textSecondary
            }, BorderLayout.NORTH)

            add(JLabel(value).apply {
                font = Font("SansSerif", Font.BOLD, 32)
                foreground = accentColor
            }, BorderLayout.CENTER)
        }
    }

    private fun createGradeDistribution(): RoundedPanel {
        val grades = listOf("A", "B", "C", "D", "F")
        val counts = grades.map { g -> students.count { it.hasScores && it.grade == g } }
        val maxCount = (counts.maxOrNull() ?: 1).coerceAtLeast(1)

        return RoundedPanel().apply {
            layout = BorderLayout(0, 15)
            border = EmptyBorder(20, 20, 20, 20)

            add(JLabel("Grade Distribution").apply {
                font = Font("SansSerif", Font.BOLD, 18)
                foreground = AppColors.textPrimary
            }, BorderLayout.NORTH)

            val barsPanel = JPanel(GridLayout(1, 5, 20, 0)).apply { isOpaque = false }
            for ((i, grade) in grades.withIndex()) {
                val barPanel = JPanel(BorderLayout(0, 8)).apply { isOpaque = false }
                val barHeight = if (maxCount > 0) (counts[i] * 150) / maxCount else 0

                val bar = object : JPanel() {
                    override fun paintComponent(g: Graphics) {
                        val g2 = g.create() as Graphics2D
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                        g2.color = getGradeColor(grade)
                        val h = (height * barHeight) / 150
                        g2.fillRoundRect((width - 40) / 2, height - h, 40, h, 8, 8)
                        g2.dispose()
                    }
                }.apply {
                    isOpaque = false
                    preferredSize = Dimension(60, 150)
                }

                val label = JLabel(grade, SwingConstants.CENTER).apply {
                    font = Font("SansSerif", Font.BOLD, 16)
                    foreground = getGradeColor(grade)
                }
                val countLabel = JLabel("${counts[i]}", SwingConstants.CENTER).apply {
                    font = Font("SansSerif", Font.PLAIN, 12)
                    foreground = AppColors.textSecondary
                }

                barPanel.add(bar, BorderLayout.CENTER)
                barPanel.add(label, BorderLayout.SOUTH)
                barPanel.add(countLabel, BorderLayout.NORTH)
                barsPanel.add(barPanel)
            }
            add(barsPanel, BorderLayout.CENTER)
        }
    }
}

// ==================== STUDENT LIST PANEL ====================
class StudentListPanel(private val students: List<Student>) : JPanel() {
    init {
        background = AppColors.background
        layout = BorderLayout(0, 20)
        border = EmptyBorder(30, 30, 30, 30)

        add(JLabel("All Students").apply {
            font = Font("SansSerif", Font.BOLD, 28)
            foreground = AppColors.textPrimary
        }, BorderLayout.NORTH)

        val columns = arrayOf("Name", "Scores", "Average", "Grade")
        val data = students.map { s ->
            arrayOf<Any>(
                s.name,
                s.scores?.joinToString(", ") ?: "N/A",
                if (s.hasScores) "${"%.1f".format(s.average)}" else "N/A",
                if (s.hasScores) s.grade else "N/A"
            )
        }.toTypedArray()

        val model = object : DefaultTableModel(data, columns) {
            override fun isCellEditable(row: Int, column: Int) = false
        }

        val table = JTable(model).apply {
            font = Font("SansSerif", Font.PLAIN, 14)
            foreground = AppColors.textPrimary
            background = AppColors.cardBg
            selectionBackground = AppColors.purpleDark
            selectionForeground = Color.WHITE
            gridColor = AppColors.surfaceLight
            rowHeight = 45
            setShowGrid(true)
            setShowHorizontalLines(true)
            setShowVerticalLines(false)
            intercellSpacing = Dimension(0, 1)

            // Center alignment for Average and Grade columns
            val centerRenderer = DefaultTableCellRenderer().apply {
                horizontalAlignment = SwingConstants.CENTER
                background = AppColors.cardBg
                foreground = AppColors.textPrimary
            }
            columnModel.getColumn(2).cellRenderer = centerRenderer

            // Grade column with color
            columnModel.getColumn(3).cellRenderer = object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable, value: Any?, isSelected: Boolean,
                    hasFocus: Boolean, row: Int, column: Int
                ): Component {
                    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    horizontalAlignment = SwingConstants.CENTER
                    font = Font("SansSerif", Font.BOLD, 14)
                    if (!isSelected) {
                        foreground = if (value != "N/A") getGradeColor(value.toString()) else AppColors.textSecondary
                        background = AppColors.cardBg
                    }
                    return comp
                }
            }
        }

        // Style header
        table.tableHeader.apply {
            font = Font("SansSerif", Font.BOLD, 14)
            background = AppColors.purpleDark
            foreground = Color.WHITE
            preferredSize = Dimension(preferredSize.width, 45)
            (this as JTableHeader).defaultRenderer = object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable, value: Any?, isSelected: Boolean,
                    hasFocus: Boolean, row: Int, column: Int
                ): Component {
                    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    background = AppColors.purpleDark
                    foreground = Color.WHITE
                    font = Font("SansSerif", Font.BOLD, 14)
                    horizontalAlignment = SwingConstants.CENTER
                    border = EmptyBorder(10, 10, 10, 10)
                    return comp
                }
            }
        }

        val scrollPane = JScrollPane(table).apply {
            border = BorderFactory.createEmptyBorder()
            background = AppColors.cardBg
            viewport.background = AppColors.cardBg
        }

        val tableWrapper = RoundedPanel().apply {
            layout = BorderLayout()
            border = EmptyBorder(5, 5, 5, 5)
            add(scrollPane)
        }

        add(tableWrapper, BorderLayout.CENTER)
    }
}

// ==================== ADD STUDENT PANEL ====================
class AddStudentPanel(private val onAdd: (Student) -> Unit) : JPanel() {
    init {
        background = AppColors.background
        layout = BorderLayout(0, 20)
        border = EmptyBorder(30, 30, 30, 30)

        add(JLabel("Add New Student").apply {
            font = Font("SansSerif", Font.BOLD, 28)
            foreground = AppColors.textPrimary
        }, BorderLayout.NORTH)

        val formCard = RoundedPanel().apply {
            layout = GridBagLayout()
            border = EmptyBorder(30, 40, 30, 40)
        }

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(8, 5, 8, 5)
        }

        val nameField = StyledTextField(25)
        val scoresField = StyledTextField(25)
        val statusLabel = JLabel(" ").apply {
            font = Font("SansSerif", Font.PLAIN, 14)
            foreground = AppColors.success
        }

        // Name label
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        formCard.add(JLabel("Student Name").apply {
            font = Font("SansSerif", Font.BOLD, 14)
            foreground = AppColors.textSecondary
        }, gbc)

        // Name field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1.0
        formCard.add(nameField, gbc)

        // Scores label
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0
        formCard.add(JLabel("Scores (comma-separated, e.g. 85,92,78)").apply {
            font = Font("SansSerif", Font.BOLD, 14)
            foreground = AppColors.textSecondary
        }, gbc)

        // Scores field
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1.0
        formCard.add(scoresField, gbc)

        // Button
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0
        gbc.insets = Insets(20, 5, 8, 5)
        val addBtn = PurpleButton("Add Student").apply {
            preferredSize = Dimension(200, 45)
        }
        formCard.add(addBtn, gbc)

        // Status
        gbc.gridx = 0; gbc.gridy = 5
        gbc.insets = Insets(8, 5, 8, 5)
        formCard.add(statusLabel, gbc)

        addBtn.addActionListener {
            val name = nameField.text.trim()
            if (name.isEmpty()) {
                statusLabel.foreground = AppColors.danger
                statusLabel.text = "Please enter a student name."
                return@addActionListener
            }

            val scoresText = scoresField.text.trim()
            val scores = if (scoresText.isEmpty()) null
            else scoresText.split(",").mapNotNull { it.trim().toIntOrNull() }

            if (scoresText.isNotEmpty() && (scores == null || scores.isEmpty())) {
                statusLabel.foreground = AppColors.danger
                statusLabel.text = "Invalid scores format. Use numbers separated by commas."
                return@addActionListener
            }

            onAdd(Student(name, scores))
            nameField.text = ""
            scoresField.text = ""
            statusLabel.foreground = AppColors.success
            statusLabel.text = "$name added successfully!"
        }

        val wrapper = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            isOpaque = false
            add(formCard)
        }
        add(wrapper, BorderLayout.CENTER)
    }
}

// ==================== EXCEL PANEL ====================
class ExcelPanel(private val onImport: (List<Student>) -> Unit) : JPanel() {
    private val resultArea = JTextArea().apply {
        font = Font("Monospaced", Font.PLAIN, 13)
        foreground = AppColors.textPrimary
        background = AppColors.surfaceLight
        caretColor = AppColors.purpleLight
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        border = EmptyBorder(15, 15, 15, 15)
    }

    init {
        background = AppColors.background
        layout = BorderLayout(0, 20)
        border = EmptyBorder(30, 30, 30, 30)

        add(JLabel("Excel Import / Export").apply {
            font = Font("SansSerif", Font.BOLD, 28)
            foreground = AppColors.textPrimary
        }, BorderLayout.NORTH)

        val actionsCard = RoundedPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT, 15, 15)
            border = EmptyBorder(15, 20, 15, 20)
        }

        val uploadBtn = PurpleButton("Upload Excel File")
        val sampleBtn = PurpleButton("Generate Sample", false)

        actionsCard.add(uploadBtn)
        actionsCard.add(sampleBtn)
        actionsCard.add(JLabel("Upload a .xlsx file with student names and marks").apply {
            font = Font("SansSerif", Font.ITALIC, 13)
            foreground = AppColors.textSecondary
        })

        val resultScroll = JScrollPane(resultArea).apply {
            border = BorderFactory.createLineBorder(AppColors.purpleAccent, 1, true)
            preferredSize = Dimension(0, 300)
        }

        val resultCard = RoundedPanel().apply {
            layout = BorderLayout(0, 10)
            border = EmptyBorder(15, 15, 15, 15)
            add(JLabel("Results").apply {
                font = Font("SansSerif", Font.BOLD, 16)
                foreground = AppColors.textPrimary
            }, BorderLayout.NORTH)
            add(resultScroll, BorderLayout.CENTER)
        }

        val centerPanel = JPanel(BorderLayout(0, 15)).apply {
            isOpaque = false
            add(actionsCard, BorderLayout.NORTH)
            add(resultCard, BorderLayout.CENTER)
        }
        add(centerPanel, BorderLayout.CENTER)

        uploadBtn.addActionListener { uploadExcel() }
        sampleBtn.addActionListener { generateSample() }
    }

    private fun uploadExcel() {
        val chooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx")
            dialogTitle = "Select Excel File"
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            processExcelFile(file)
        }
    }

    private fun processExcelFile(file: File) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0) ?: run {
                resultArea.text = "Error: Excel file is empty."
                workbook.close()
                return
            }

            var nameCol = -1
            var marksStartCol = -1
            var marksEndCol = -1

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

            if (nameCol == -1) nameCol = 0
            if (marksStartCol == -1) {
                marksStartCol = 1
                marksEndCol = headerRow.lastCellNum.toInt() - 1
            }

            val avgCol = marksEndCol + 1
            val gradeCol = marksEndCol + 2
            headerRow.createCell(avgCol).setCellValue("Average")
            headerRow.createCell(gradeCol).setCellValue("Grade")

            val headerStyle = workbook.createCellStyle()
            val headerFont = workbook.createFont()
            headerFont.bold = true
            headerStyle.setFont(headerFont)
            headerRow.getCell(avgCol).cellStyle = headerStyle
            headerRow.getCell(gradeCol).cellStyle = headerStyle

            val importedStudents = mutableListOf<Student>()
            val sb = StringBuilder("=== Excel Processing Results ===\n\n")

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val studentName = row.getCell(nameCol)?.toString()?.trim() ?: "Unknown"

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
                    sb.appendLine("$studentName: Average = ${"%.1f".format(average)}, Grade = $grade")
                    importedStudents.add(Student(studentName, marks))
                } else {
                    row.createCell(avgCol).setCellValue("N/A")
                    row.createCell(gradeCol).setCellValue("N/A")
                    sb.appendLine("$studentName: No scores available")
                }
            }

            for (col in 0..gradeCol) {
                sheet.autoSizeColumn(col)
            }

            val outputFile = File(file.parent, file.nameWithoutExtension + "_graded.xlsx")
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()

            sb.appendLine("\nGraded file saved to: ${outputFile.absolutePath}")
            sb.appendLine("\n${importedStudents.size} students imported into the app.")
            resultArea.text = sb.toString()

            if (importedStudents.isNotEmpty()) {
                onImport(importedStudents)
            }

        } catch (e: Exception) {
            resultArea.text = "Error processing file: ${e.message}"
        }
    }

    private fun generateSample() {
        val chooser = JFileChooser().apply {
            selectedFile = File("sample_students.xlsx")
            dialogTitle = "Save Sample Excel File"
        }

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var outFile = chooser.selectedFile
            if (!outFile.name.endsWith(".xlsx")) {
                outFile = File(outFile.absolutePath + ".xlsx")
            }

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Students")

            val headerStyle = workbook.createCellStyle()
            val headerFont = workbook.createFont()
            headerFont.bold = true
            headerStyle.setFont(headerFont)

            val headerRow = sheet.createRow(0)
            listOf("Name", "Math", "Science", "English").forEachIndexed { i, h ->
                headerRow.createCell(i).apply {
                    setCellValue(h)
                    cellStyle = headerStyle
                }
            }

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

            for (i in 0..3) sheet.autoSizeColumn(i)

            FileOutputStream(outFile).use { workbook.write(it) }
            workbook.close()

            resultArea.text = "Sample Excel file created at:\n${outFile.absolutePath}\n\nYou can now upload it using the 'Upload Excel File' button."
        }
    }
}

// ==================== MAIN APPLICATION ====================
class GradeCalculatorApp : JFrame("Grade Calculator") {
    private val students = mutableListOf(
        Student("Alice", listOf(85, 92, 78, 90)),
        Student("Bob", listOf(55, 63, 48, 70)),
        Student("Charlie", null),
        Student("Diana", listOf(95, 98, 100, 92)),
        Student("Eve", listOf(72, 68, 74, 65))
    )

    private val contentPanel = JPanel(CardLayout()).apply {
        background = AppColors.background
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        size = Dimension(1100, 700)
        minimumSize = Dimension(900, 600)
        setLocationRelativeTo(null)
        contentPane.background = AppColors.background

        layout = BorderLayout()

        val sidebar = Sidebar { page -> navigateTo(page) }
        add(sidebar, BorderLayout.WEST)

        refreshPanels()
        add(contentPanel, BorderLayout.CENTER)

        navigateTo("dashboard")
    }

    private fun refreshPanels() {
        contentPanel.removeAll()
        contentPanel.add(DashboardPanel(students), "dashboard")
        contentPanel.add(StudentListPanel(students), "students")
        contentPanel.add(AddStudentPanel { student ->
            students.add(student)
            refreshPanels()
        }, "add")
        contentPanel.add(ExcelPanel { imported ->
            students.addAll(imported)
            refreshPanels()
        }, "excel")
    }

    private fun navigateTo(page: String) {
        refreshPanels()
        (contentPanel.layout as CardLayout).show(contentPanel, page)
    }
}

fun main() {
    FlatDarkLaf.setup()
    UIManager.put("ScrollBar.thumbArc", 999)
    UIManager.put("ScrollBar.thumbInsets", Insets(2, 2, 2, 2))

    SwingUtilities.invokeLater {
        GradeCalculatorApp().isVisible = true
    }
}
