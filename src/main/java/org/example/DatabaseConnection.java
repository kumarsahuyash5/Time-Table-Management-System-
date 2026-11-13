package org.example;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/timetable_db";
    private static final String USER = "root";
    private static final String PASSWORD = "Student@123"; // 👈 Yahan apna MySQL password enter karein

    public static Connection getConnection() {
        Connection conn = null;
        try {
            // MySQL Driver register karein
            Class.forName("com.mysql.cj.jdbc.Driver");

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Database Connected Successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver Class Not Found! Check pom.xml dependency.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Database Connection Failed!");
            e.printStackTrace();
        }
        return conn;
    }

    public static void main(String[] args) {
        Connection conn = getConnection();
        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM schedule");

                System.out.println("\n--- Timetable Data from MySQL ---");
                while (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id") +
                            " | Subject: " + rs.getString("subject") +
                            " | Teacher: " + rs.getString("teacher") +
                            " | Day: " + rs.getString("day") +
                            " | Time: " + rs.getString("time_slot"));
                }
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}