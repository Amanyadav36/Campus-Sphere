package com.example.campus_sphere;

import android.net.Uri;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class CsvUtils {

    private CsvUtils() {}

    public static String escape(String value) {
        if (value == null) return "";
        boolean mustQuote = value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"");
        if (!mustQuote) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public static String buildStudentCsv(java.util.List<StudentRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("name,email,mobile,branch,enrollment,section,year\n");
        for (StudentRow r : rows) {
            sb.append(escape(r.name)).append(',')
                    .append(escape(r.email)).append(',')
                    .append(escape(r.mobile)).append(',')
                    .append(escape(r.branch)).append(',')
                    .append(escape(r.enrollment)).append(',')
                    .append(escape(r.section)).append(',')
                    .append(escape(r.year)).append('\n');
        }
        return sb.toString();
    }

    public static void writeUtf8(OutputStream os, String content) throws Exception {
        os.write(content.getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    public static final class StudentRow {
        public final String name;
        public final String email;
        public final String mobile;
        public final String branch;
        public final String enrollment;
        public final String section;
        public final String year;

        public StudentRow(String name, String email, String mobile, String branch, String enrollment, String section, String year) {
            this.name = name;
            this.email = email;
            this.mobile = mobile;
            this.branch = branch;
            this.enrollment = enrollment;
            this.section = section;
            this.year = year;
        }
    }
}
