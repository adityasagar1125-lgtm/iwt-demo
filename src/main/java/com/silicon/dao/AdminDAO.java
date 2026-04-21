package com.silicon.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.silicon.util.DBConnection;

public class AdminDAO {

    public int getTotalClubs() {
        return getCount("SELECT COUNT(*) FROM clubs");
    }

    public int getTotalUsers() {
        return getCount("SELECT COUNT(*) FROM users");
    }

    public int getTotalEvents() {
        return getCount("SELECT COUNT(*) FROM events");
    }

    public int getTotalMemberships() {
        return getCount("SELECT COUNT(*) FROM memberships");
    }

    private int getCount(String sql) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
