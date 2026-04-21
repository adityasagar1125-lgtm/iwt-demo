package com.silicon.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.silicon.model.Club;
import com.silicon.util.DBConnection;

public class ClubDAO {

    public List<Club> getAllClubs() {
        List<Club> clubs = new ArrayList<>();
        String sql = "SELECT * FROM clubs ORDER BY id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clubs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clubs;
    }

    public List<Club> getPendingClubs() {
        List<Club> clubs = new ArrayList<>();
        String sql = "SELECT * FROM clubs WHERE status = 'pending' ORDER BY id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clubs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clubs;
    }

    public Club getClubById(int id) {
        String sql = "SELECT * FROM clubs WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Club createClub(Club club) {
        String sql = "INSERT INTO clubs (name, short_name, description, category, icon, color_accent, founded_year, member_count, status) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, club.getName());
            ps.setString(2, club.getShortName());
            ps.setString(3, club.getDescription());
            ps.setString(4, club.getCategory());
            ps.setString(5, club.getIcon());
            ps.setString(6, club.getColorAccent());
            ps.setInt(7, club.getFoundedYear());
            ps.setInt(8, club.getMemberCount());
            ps.setString(9, club.getStatus() == null || club.getStatus().isBlank() ? "pending" : club.getStatus());

            int updated = ps.executeUpdate();
            if (updated == 0) return null;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    club.setId(keys.getInt(1));
                }
            }
            return club;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Club updateClub(Club club) {
        String sql = "UPDATE clubs SET name = ?, short_name = ?, description = ?, category = ?, icon = ?, " +
                     "color_accent = ?, founded_year = ?, member_count = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, club.getName());
            ps.setString(2, club.getShortName());
            ps.setString(3, club.getDescription());
            ps.setString(4, club.getCategory());
            ps.setString(5, club.getIcon());
            ps.setString(6, club.getColorAccent());
            ps.setInt(7, club.getFoundedYear());
            ps.setInt(8, club.getMemberCount());
            ps.setInt(9, club.getId());

            if (ps.executeUpdate() == 0) return null;
            return getClubById(club.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteClub(int id) {
        String sql = "DELETE FROM clubs WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateClubStatus(int id, String status) {
        String sql = "UPDATE clubs SET status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getClubMembers(int clubId) {
        List<String> members = new ArrayList<>();
        String sql = "SELECT u.name FROM memberships m JOIN users u ON m.user_id = u.user_id WHERE m.club_id = ? ORDER BY u.name";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                members.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public int getMemberCountByClub(int clubId) {
        String sql = "SELECT COUNT(*) AS total FROM memberships WHERE club_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void syncMemberCount(int clubId) {
        String sql = "UPDATE clubs SET member_count = (SELECT COUNT(*) FROM memberships WHERE club_id = ?) WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            ps.setInt(2, clubId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Club mapRow(ResultSet rs) throws SQLException {
        Club c = new Club();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setShortName(rs.getString("short_name"));
        c.setDescription(rs.getString("description"));
        c.setCategory(rs.getString("category"));
        c.setIcon(rs.getString("icon"));
        c.setColorAccent(rs.getString("color_accent"));
        c.setStatus(rs.getString("status"));
        c.setFoundedYear(rs.getInt("founded_year"));
        c.setMemberCount(rs.getInt("member_count"));
        return c;
    }
}
