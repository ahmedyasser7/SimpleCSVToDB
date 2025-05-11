package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CsvToDatabase {
    private Connection conn;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/users_db?useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "123456789*";
    private static final String CSV_FILE = "data_extended.csv";
    private static final String TIME_FILE = "execution_time.txt";

    public static void main(String[] args) {
        CsvToDatabase csvToDb = new CsvToDatabase();
        long startTime = System.nanoTime();

        try {
            csvToDb.connect();
            csvToDb.createTable();
            csvToDb.importCsv();
            csvToDb.selectSample();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            csvToDb.disconnect();
        }

        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;
        System.out.printf("Process took: %.3f ms%n", executionTime);

        // Write execution time to file
        try (FileWriter writer = new FileWriter(TIME_FILE)) {
            writer.write(String.format("Execution Time: %.3f ms%n", executionTime));
            System.out.println("Execution time written to " + TIME_FILE);
        } catch (IOException e) {
            System.err.println("Error writing to time file: " + e.getMessage());
        }
    }

    private void connect() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
    }

    private void disconnect() {
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT PRIMARY KEY, " +
                "firstName VARCHAR(50), " +
                "lastName VARCHAR(50), " +
                "email VARCHAR(100))";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private List<String[]> readCsv() throws Exception {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length == 4) {
                    records.add(values);
                }
            }
        }
        return records;
    }

    private void importCsv() throws Exception {
        String sql = "INSERT INTO users (id, firstName, lastName, email) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (String[] record : readCsv()) {
                try {
                    pstmt.setInt(1, Integer.parseInt(record[0]));
                    pstmt.setString(2, record[1]);
                    pstmt.setString(3, record[2]);
                    pstmt.setString(4, record[3]);
                    pstmt.executeUpdate();
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid row: " + String.join(",", record));
                }
            }
            conn.commit();
        }
    }

    private void selectSample() throws SQLException {
        String sql = "SELECT * FROM users LIMIT 5";
        try (Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(sql);
            while (rs.next()) {
                System.out.printf("ID: %d, Name: %s %s, Email: %s%n",
                        rs.getInt("id"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("email"));
            }
        }
    }
}