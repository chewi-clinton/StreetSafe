#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LIB_DIR="lib"
CLASSPATH=$(echo "$LIB_DIR"/*.jar | tr ' ' ':')

echo "Compiling Grade Calculator GUI..."
kotlinc GradeCalculator.kt -cp "$CLASSPATH" -include-runtime -d GradeCalculator.jar 2>/dev/null

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Launching Grade Calculator..."
    java -cp "GradeCalculator.jar:$CLASSPATH" GradeCalculatorKt
else
    echo "Compilation failed!"
    exit 1
fi
