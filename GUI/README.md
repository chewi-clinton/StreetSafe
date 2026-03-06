# Grade Calculator - Desktop GUI Application

A modern desktop GUI application built with Kotlin and Swing (FlatLaf) for calculating and managing student grades with Excel import/export support.

## Features

- **Dashboard** - Overview with stats cards showing total students, class average, passing rate, and top grades. Includes a visual grade distribution chart.
- **Student List** - View all students in a styled table with names, scores, averages, and color-coded letter grades.
- **Add Student** - Form to add new students with their name and comma-separated scores.
- **Excel Import/Export** - Upload `.xlsx` files with student data, auto-calculate grades, and export graded results. Includes a sample file generator.

## Design

- Dark theme with purple accent colors
- Rounded cards and modern UI components
- Sidebar navigation
- Color-coded grades (A=Green, B=Blue, C=Yellow, D=Orange, F=Red)

## Prerequisites

- Kotlin compiler (`kotlinc`)
- Java Runtime Environment (JRE 17+)
- Required libraries in `lib/` folder:
  - flatlaf-3.4.jar
  - poi-5.2.5.jar
  - poi-ooxml-5.2.5.jar
  - poi-ooxml-lite-5.2.5.jar
  - commons-compress-1.25.0.jar
  - commons-io-2.15.1.jar
  - xmlbeans-5.2.0.jar
  - commons-collections4-4.4.jar
  - commons-codec-1.16.0.jar
  - log4j-api-2.22.1.jar

## Build and Run

```bash
chmod +x build_and_run.sh
./build_and_run.sh
```

Or manually:
```bash
kotlinc GradeCalculator.kt -cp "lib/*" -include-runtime -d GradeCalculator.jar
java -cp "GradeCalculator.jar:lib/*" GradeCalculatorKt
```

## Author

Chewi Clinton Shu
