package com.silicon.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.silicon.model.Announcement;
import com.silicon.util.DBConnection;

public class AnnouncementDAO {

    public List<Announcement> getAnnouncements(Integer clubId) {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT a.*, c.name AS club_name FROM announcements a JOIN clubs c ON a.club_id = c.id " +
                (clubId == null ? "ORDER BY a.created_at DESC" : "WHERE a.club_id = ? ORDER BY a.created_at DESC");
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (clubId != null) {
                ps.setInt(1, clubId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Announcement a = new Announcement();
                a.setAnnouncementId(rs.getInt("announcement_id"));
                a.setClubId(rs.getInt("club_id"));
                a.setClubName(rs.getString("club_name"));
                a.setTitle(rs.getString("title"));
                a.setBody(rs.getString("body"));
                a.setPriority(rs.getString("priority"));
                a.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
                list.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean createAnnouncement(Announcement a) {
        String sql = "INSERT INTO announcements(club_id, title, body, priority) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.getClubId());
            ps.setString(2, a.getTitle());
            ps.setString(3, a.getBody());
            ps.setString(4, a.getPriority());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
