# Task 1 - Student Grade Calculator (Console Application)

A Kotlin console application that calculates student grades from manual input or Excel files.

## Features

- **View All Grades**: Display pre-loaded student data with calculated averages and letter grades
- **Add New Students**: Input student names and scores via the terminal
- **Excel File Processing**: Upload a `.xlsx` file with student names and marks, automatically calculate averages and grades, and export the results to a new graded Excel file
- **Sample Excel Generator**: Create a test Excel file to quickly try out the Excel processing feature

## Grade Scale

| Score Range | Grade |
|-------------|-------|
| 90 - 100    | A     |
| 80 - 89     | B     |
| 70 - 79     | C     |
| 60 - 69     | D     |
| Below 60    | F     |

## How to Build and Run

### Prerequisites
- Kotlin compiler (`kotlinc`)
- Java Runtime Environment (JRE)
- Apache POI library (for Excel support)

### Steps

1. Download the required Apache POI dependencies into a `lib/` folder:
   - poi-5.2.5.jar
   - poi-ooxml-5.2.5.jar
   - poi-ooxml-lite-5.2.5.jar
   - commons-compress-1.25.0.jar
   - commons-io-2.15.1.jar
   - xmlbeans-5.2.0.jar
   - commons-collections4-4.4.jar
   - commons-codec-1.16.0.jar
   - log4j-api-2.22.1.jar

2. Compile:
   ```bash
   kotlinc Chewi_clinton_shu.kt -cp "lib/*" -include-runtime -d Chewi_clinton_shu.jar
   ```

3. Run:
   ```bash
   java -cp "Chewi_clinton_shu.jar:lib/*" Chewi_clinton_shuKt
   ```

## Excel File Format

The application expects an Excel file (.xlsx) with the following structure:

| Name    | Math | Science | English |
|---------|------|---------|---------|
| Alice   | 85   | 92      | 78      |
| Bob     | 55   | 63      | 70      |

- First column should contain student names
- Remaining columns should contain numeric marks/scores
- Headers are auto-detected by keywords: "name", "mark", "score", "subject"

After processing, the application adds **Average** and **Grade** columns and saves the output as `<filename>_graded.xlsx` in the same directory.

## Author

Chewi Clinton Shu
