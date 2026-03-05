import com.formdev.flatlaf.FlatDarkLaf
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.EmptyBorder

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

// ==================== MAIN APPLICATION ====================
class GradeCalculatorApp : JFrame("Grade Calculator") {
    private val students = mutableListOf(
        Student("Alice", listOf(85, 92, 78, 90)),
        Student("Bob", listOf(55, 63, 48, 70)),
        Student("Charlie", null),
        Student("Diana", listOf(95, 98, 100, 92)),
        Student("Eve", listOf(72, 68, 74, 65))
    )

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        size = Dimension(1100, 700)
        minimumSize = Dimension(900, 600)
        setLocationRelativeTo(null)
        contentPane.background = AppColors.background
        layout = BorderLayout()

        add(JLabel("Grade Calculator - Building UI...", SwingConstants.CENTER).apply {
            font = Font("SansSerif", Font.BOLD, 28)
            foreground = AppColors.textPrimary
        }, BorderLayout.CENTER)
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
