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
