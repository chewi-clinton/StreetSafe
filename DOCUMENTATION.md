# Grade Calculator - Kotlin Branch

A comprehensive Grade Calculator project built in Kotlin, demonstrating progressive mastery of the language across multiple tasks: from basic functions, to lambdas and collections, to full object-oriented programming, and finally a modern Android mobile application.

---

## Project Structure

```
GradeCalculator/
├── Task_1/                  # Core grade calculator (functions, data class, Excel I/O)
├── Task_2/                  # Lambdas, higher-order functions, collection operations
├── Task_3/                  # OOP: inheritance, interfaces, sealed classes, companion objects
├── test/                    # Unit test suite for grade logic and collection operations
├── GUI/                     # Desktop GUI application (Java Swing + FlatLaf dark theme)
├── android-app/             # Android mobile application (Material Design, Kotlin)
└── README.md
```

---

## Task 1 - Core Grade Calculator

**File:** `Task_1/Chewi_clinton_shu.kt`

A terminal-based student grade calculator with Excel import/export support.

**Key Features:**
- `Student` data class with nullable scores
- Grade calculation using `when` expressions (A/B/C/D/F scale)
- Interactive menu with 5 options: view grades, add student, upload Excel, create sample, exit
- Excel processing with Apache POI: auto-detects name/score columns, writes averages and grades back
- Input parsing with `mapNotNull` and `toIntOrNull` for safe conversion

**Run:**
```bash
kotlinc Task_1/Chewi_clinton_shu.kt -include-runtime -d task1.jar -cp "GUI/lib/*"
java -cp "task1.jar:GUI/lib/*" Chewi_clinton_shuKt
```

---

## Task 2 - Lambdas and Collection Operations

**File:** `Task_2/Chewi_clinton_shu.kt`

Extends Task 1 by replacing loops with functional programming patterns.

**Key Changes from Task 1:**
- `getGrade` converted from a function to a **lambda variable** `(Double) -> String`
- `evaluateStudent` is a **higher-order function** accepting a grading strategy parameter
- `forEach` and `forEachIndexed` replace traditional `for` loops throughout
- **filter**: `filterStudentsByGrade()`, `getValidStudents()`, `getAtRiskStudents()`
- **map**: `getStudentAverages()` transforms students into `Pair<String, Double>` list
- **fold**: `calculateClassTotal()` and `calculateClassAverage()` accumulate scores
- **sortedBy / sortedByDescending**: `getStudentsSortedByAverage()` for ranking
- **take**: `getTopStudents()` gets the top N performers
- **groupBy**: `groupStudentsByGrade()` groups students by letter grade
- **flatMap**: aggregates all individual scores across students in `generateSummaryReport()`
- **any / all / count**: boolean checks like `allStudentsPassed()`, `anyStudentGotA()`
- **distinct / joinToString**: unique grade listing in summary reports
- **maxByOrNull / minByOrNull**: find highest and lowest performers

**Run:**
```bash
kotlinc Task_2/Chewi_clinton_shu.kt -include-runtime -d task2.jar -cp "GUI/lib/*"
java -cp "task2.jar:GUI/lib/*" Chewi_clinton_shuKt
```

---

## Task 3 - Object-Oriented Programming

**File:** `Task_3/Chewi_clinton_shu.kt`

Demonstrates all core OOP concepts covered in the course.

**Key Changes from Task 2:**
- **Abstract class** `Person` with `init` validation block, abstract `role()` method, and `open fun displayInfo()`
- **Inheritance**: `Student` extends `Person`, `GraduateStudent` extends `Student` (multi-level)
- **Teacher** class extends `Person` with department and course management
- **Data class** `Assessment` with `copy()`, destructuring declarations, and custom getter for `percentage`
- **Interfaces**: `Gradable` (with default method `isPassing()`) and `Exportable` (with `toCSV()`, `toSummaryString()`)
- **Multiple interface implementation**: `CourseResult` implements both `Gradable` and `Exportable`
- **Sealed class** `GradeResult` with `Passed`, `Failed`, `Incomplete`, and `NoData` subtypes
- **Exhaustive `when`** handling for sealed class without `else` branch
- **Companion object** `GradeCalculator` with `const` properties, factory method `fromScore()`, and `createReport()`
- **Visibility modifiers**: `private` for internal state, `protected` for age in Person
- **Polymorphism**: `List<Person>` storing Student, GraduateStudent, and Teacher instances

**Run:**
```bash
kotlinc Task_3/Chewi_clinton_shu.kt -include-runtime -d task3.jar
java -jar task3.jar
```

---

## Test Suite

**File:** `test/GradeCalculatorTest.kt`

Lightweight test framework (no external dependencies) covering all core logic.

**Test Suites:**
- Grade calculation boundary tests (A=90, B=80, C=70, D=60, F<60)
- Student model: average computation, null/empty score handling
- Data class features: equals, hashCode, copy, destructuring, toString
- Collection operations: filter, map, fold, groupBy, flatMap, sortedBy, take, any, all, count, distinct
- Higher-order functions: lambda variables, strategy pattern, generic transforms
- Edge cases: perfect scores, zero scores, boundary values, large lists

**Run:**
```bash
kotlinc test/GradeCalculatorTest.kt -include-runtime -d tests.jar
java -jar tests.jar
```

---

## Desktop GUI Application

**Directory:** `GUI/`

A full-featured desktop application built with Java Swing and FlatLaf dark theme.

**Screens:**
- **Dashboard**: stats cards (total students, class average, passing count, top grade) and grade distribution bar chart
- **Student List**: styled table with color-coded grades
- **Add Student**: form with validation and status feedback
- **Excel Import/Export**: file chooser, auto-processing, sample generation

**Run:**
```bash
cd GUI && chmod +x build_and_run.sh && ./build_and_run.sh
```

---

## Android Mobile Application

**Directory:** `android-app/`

A modern Android app following Material Design principles with a clean, card-based UI.

**Design System:**
- Primary Purple: `#6A4DF6` - buttons, top bars, active elements
- Light Purple Muted: `#EBE7FF` - secondary buttons, icon backgrounds
- App Background: `#F5F6FA` - soft off-white base
- Card Surface: `#FFFFFF` - white cards with 16dp rounded corners
- Grade colors: Green (A), Blue (B), Yellow (C), Orange (D), Red (F)

**Screens:**

| Screen | Description |
|--------|-------------|
| **Home/Dashboard** | Greeting, two large navigation cards (Upload Excel, Manual Entry), quick stats |
| **Manual Entry** | Purple top bar, student name input, dynamic subject rows with add/delete, sticky calculate button |
| **Results** | Hero card with large grade display, pass/fail badge, scrollable subject breakdown with grade badges |
| **Excel Upload** | Dashed upload zone, file picker integration, processing results display |

**Tech Stack:**
- Kotlin with View Binding
- Material Components for Android
- CardView and ConstraintLayout
- Apache POI for Excel processing
- ActivityResultContracts for file picking

**Build (Android Studio):**
1. Open the `android-app/` folder in Android Studio
2. Sync Gradle and let dependencies download
3. Connect your Android device via USB (enable Developer Options and USB Debugging)
4. Click Run or use `./gradlew installDebug`

---

## Grade Scale

All tasks use the same grading scale:

| Score Range | Grade |
|-------------|-------|
| 90 - 100    | A     |
| 80 - 89     | B     |
| 70 - 79     | C     |
| 60 - 69     | D     |
| 0 - 59      | F     |

---

## Author

**Chewi Clinton Shu**
SE 3242: Android Application Development
ICT University, Cameroon
