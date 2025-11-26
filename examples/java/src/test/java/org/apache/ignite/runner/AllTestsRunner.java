package org.apache.ignite.runner;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class AllTestsRunner {
    private static final Path SOURCE_ROOT = Paths.get("C:/Users/ashis/git/gridgain-9/examples/java/src/main/java");
    private static final String EXCEL_PATH = "C:/Users/ashis/Documents/ZettaScape/GridGain/TestCaseLocal.xlsx";

    public static void main(String[] args) throws Exception {
        List<Path> javaFiles;

        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            javaFiles = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }

        System.out.println("Total .java files found: " + javaFiles.size());

        List<TestResult> results = new ArrayList<>();

        for (Path javaFile : javaFiles) {
            String fileName = javaFile.getFileName().toString();
            String relative = SOURCE_ROOT.relativize(javaFile).toString();
            String className = relative
                    .replace(File.separatorChar, '.')
                    .replace(".java", "");

            String source = Files.readString(javaFile);
            if (!source.contains("public static void main(")) {
                results.add(new TestResult(fileName, className, "NO main()", "SKIPPED"));
                continue;
            }

            try {
                // Compile
                Process compile = new ProcessBuilder("javac", javaFile.toString())
                        .redirectErrorStream(true)
                        .start();
                compile.waitFor();

                if (compile.exitValue() != 0) {
                    String err = new String(compile.getInputStream().readAllBytes());
                    results.add(new TestResult(fileName, className, err, "COMPILE FAILED"));
                    continue;
                }

                // Run
                Process run = new ProcessBuilder("java", "-cp",
                        SOURCE_ROOT.toString(), className)
                        .redirectErrorStream(true)
                        .start();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                run.getInputStream().transferTo(out);
                run.waitFor();

                if (run.exitValue() == 0)
                    results.add(new TestResult(fileName, className, "", "SUCCESS"));
                else
                    results.add(new TestResult(fileName, className, out.toString(), "FAILED"));

            } catch (Exception e) {
                results.add(new TestResult(fileName, className, e.getMessage(), "FAILED"));
            }
        }

        writeResultsToExcel(results);
        System.out.println("\nAll tests completed. Results written to: " + EXCEL_PATH);
    }

    private static void writeResultsToExcel(List<TestResult> results) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Results");

        Row header = sheet.createRow(0);
        String[] cols = {"Sr No", "Example / Testcase file", "Exception", "Status"};
        for (int i = 0; i < cols.length; i++)
            header.createCell(i).setCellValue(cols[i]);

        int rowNum = 1;
        for (int i = 0; i < results.size(); i++) {
            TestResult r = results.get(i);
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(r.className);
            row.createCell(2).setCellValue(r.exception);
            row.createCell(3).setCellValue(r.status);
        }

        for (int i = 0; i < cols.length; i++)
            sheet.autoSizeColumn(i);

        try (FileOutputStream out = new FileOutputStream(EXCEL_PATH)) {
            workbook.write(out);
        }
        workbook.close();
    }

    private static class TestResult {
        String fileName;
        String className;
        String exception;
        String status;

        TestResult(String fileName, String className, String exception, String status) {
            this.fileName = fileName;
            this.className = className;
            this.exception = exception;
            this.status = status;
        }
    }

}
