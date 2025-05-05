package com.example.languagelistenings;
import android.os.StrictMode;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.io.File;

public class ConnectionClass {

    static Connection conn;
    static ResultSet resultSetLong;
    static ResultSet resultSetTotalAmount;
    static Boolean updatedTimeListened = true;
    static Boolean updatedDataTable = true;

    private static LanguageDict languageDict = new LanguageDict();

    public Connection CONN() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                try {
                    Class.forName("org.postgresql.Driver");
                    String conUrl = languageDict.getCurrentLanguageInfo().getUrl();
                    conn = DriverManager.getConnection(conUrl);
                } catch (ClassNotFoundException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn;
    }

    public ResultSet dbGetLong() {
        if (resultSetLong==null || updatedDataTable) {
            try {
                dbUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return resultSetLong;
    }

    public ResultSet dbGetTotalAmount() {
        if (resultSetTotalAmount==null || updatedTimeListened) {
            try {
                dbUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return resultSetTotalAmount;
    }

    public void dbUpdate() {
        String dbName = languageDict.getCurrentLanguageInfo().getDbName();
        try {
            Statement statementLong =
                    CONN().createStatement(
                            ResultSet.TYPE_SCROLL_INSENSITIVE,
                            ResultSet.CONCUR_READ_ONLY);
            Statement statementTotalAmount =
                    CONN().createStatement(
                            ResultSet.TYPE_SCROLL_INSENSITIVE,
                            ResultSet.CONCUR_READ_ONLY);
            resultSetLong = statementLong.executeQuery("WITH Sums AS (" +
                    "SELECT dt, SUM(amount) AS sumAmount, 'NO' As com FROM " + dbName + " WHERE dt NOT IN (SELECT dt FROM " + dbName + " WHERE comment <> '') GROUP BY dt " +
                    "UNION " +
                    "SELECT dt, SUM(amount) AS sumAmount, 'YES' As com FROM " + dbName + " WHERE dt IN (SELECT dt FROM " + dbName + " WHERE comment <> '') GROUP BY dt " +
                    ")" +
                    "SELECT dt, sumAmount, com FROM Sums ORDER BY dt DESC;");
            resultSetTotalAmount = statementTotalAmount.executeQuery("SELECT SUM(amount) AS total_time FROM " + dbName + ";");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setDirty() { updatedDataTable=true; updatedTimeListened=true; }
    public void setCleanTimeListened() { updatedTimeListened=false; }
    public void setCleanDataTable() { updatedDataTable=false; }
    public Boolean isDirtyTimeListened() { return updatedTimeListened; }
    public Boolean isDirtyDataTable() { return updatedDataTable; }
}
