package com.silicon.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.silicon.model.Event;
import com.silicon.util.DBConnection;

public class EventDAO {

    private static final Logger LOGGER = Logger.getLogger(EventDAO.class.getName());

    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.*, c.name AS club_name, c.icon AS club_icon " +
                     "FROM events e JOIN clubs c ON e.club_id = c.id " +
                     "ORDER BY e.event_date ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) events.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch all events", e);
        }
        return events;
    }

    public List<Event> getEventsByClub(int clubId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.*, c.name AS club_name, c.icon AS club_icon " +
                     "FROM events e JOIN clubs c ON e.club_id = c.id " +
                     "WHERE e.club_id = ? ORDER BY e.event_date ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) events.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch events by club: " + clubId, e);
        }
        return events;
    }

    public List<Event> getUpcomingEvents(int limit) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.*, c.name AS club_name, c.icon AS club_icon " +
                     "FROM events e JOIN clubs c ON e.club_id = c.id " +
                     "WHERE e.event_date >= CURDATE() " +
                     "ORDER BY e.event_date ASC LIMIT ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) events.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch upcoming events", e);
        }
        return events;
    }

    public Event createEvent(Event event) {
        String insertSql = "INSERT INTO events (club_id, title, description, event_date, event_time, venue, image_url, max_seats, status) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, event.getClubId());
            ps.setString(2, event.getTitle());
            ps.setString(3, event.getDescription());
            ps.setString(4, event.getEventDate());
            ps.setString(5, event.getEventTime());
            ps.setString(6, event.getVenue());
            ps.setString(7, event.getImageUrl());
            ps.setInt(8, event.getMaxSeats() > 0 ? event.getMaxSeats() : 100);
            ps.setString(9, event.getStatus());

            int updated = ps.executeUpdate();
            if (updated == 0) return null;

            int createdId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    createdId = keys.getInt(1);
                }
            }

            if (createdId == -1) return null;
            return getEventById(createdId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to create event", e);
            return null;
        }
    }

    public Event getEventById(int id) {
        String sql = "SELECT e.*, c.name AS club_name, c.icon AS club_icon " +
                     "FROM events e JOIN clubs c ON e.club_id = c.id WHERE e.id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch event by id: " + id, e);
        }
        return null;
    }

    public Event updateEvent(Event event) {
        String sql = "UPDATE events SET club_id = ?, title = ?, description = ?, event_date = ?, event_time = ?, " +
                     "venue = ?, image_url = ?, max_seats = ?, status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, event.getClubId());
            ps.setString(2, event.getTitle());
            ps.setString(3, event.getDescription());
            ps.setString(4, event.getEventDate());
            ps.setString(5, event.getEventTime());
            ps.setString(6, event.getVenue());
            ps.setString(7, event.getImageUrl());
            ps.setInt(8, event.getMaxSeats() > 0 ? event.getMaxSeats() : 100);
            ps.setString(9, event.getStatus());
            ps.setInt(10, event.getId());

            if (ps.executeUpdate() == 0) return null;
            return getEventById(event.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to update event id: " + event.getId(), e);
            return null;
        }
    }

    public boolean deleteEvent(int id) {
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to delete event id: " + id, e);
            return false;
        }
    }

    public boolean registerUserForEvent(int userId, int eventId) {
        String sql = "INSERT INTO event_registrations(user_id, event_id) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public int getRegistrationCount(int eventId) {
        String sql = "SELECT COUNT(*) AS total FROM event_registrations WHERE event_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch registration count for event id: " + eventId, e);
        }
        return 0;
    }

    private Event mapRow(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setId(rs.getInt("id"));
        e.setClubId(rs.getInt("club_id"));
        e.setClubName(rs.getString("club_name"));
        e.setClubIcon(rs.getString("club_icon"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setEventDate(rs.getString("event_date"));
        e.setEventTime(rs.getString("event_time"));
        e.setVenue(rs.getString("venue"));
        e.setImageUrl(rs.getString("image_url"));
        try {
            e.setMaxSeats(rs.getInt("max_seats"));
        } catch (SQLException ignore) {
            e.setMaxSeats(100);
        }
        e.setStatus(rs.getString("status"));
        return e;
    }
}
