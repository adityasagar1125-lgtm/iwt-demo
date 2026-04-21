package com.silicon.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static final String MYSQL_URL = readOrDefault(
            "CLUBHUB_DB_URL",
            "jdbc:mysql://localhost:3306/silicon_clubhub?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    );
    private static final String MYSQL_USER = readOrDefault("CLUBHUB_DB_USER", "root");
    private static final String MYSQL_PASS = readOrDefault("CLUBHUB_DB_PASS", "password");

    private static final String H2_URL = "jdbc:h2:file:./.local/clubhubdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";
    private static final String H2_USER = "sa";
    private static final String H2_PASS = "";

    private static volatile boolean useH2Fallback = false;
    private static volatile boolean h2Initialized = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Required JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!useH2Fallback) {
            try {
                return DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
            } catch (SQLException ex) {
                useH2Fallback = true;
                System.err.println("[ClubHub] MySQL unavailable, switching to embedded H2 fallback: " + ex.getMessage());
            }
        }

        Connection con = DriverManager.getConnection(H2_URL, H2_USER, H2_PASS);
        initializeH2IfNeeded(con);
        return con;
    }

    private static synchronized void initializeH2IfNeeded(Connection con) throws SQLException {
        if (h2Initialized) {
            return;
        }

        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(150) UNIQUE," +
                    "roll_number VARCHAR(20) UNIQUE," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "role VARCHAR(20) DEFAULT 'student'," +
                    "bio VARCHAR(300)," +
                    "profile_picture VARCHAR(255)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            st.execute("CREATE TABLE IF NOT EXISTS clubs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100) NOT NULL," +
                    "short_name VARCHAR(30)," +
                    "description TEXT," +
                    "category VARCHAR(50)," +
                    "icon VARCHAR(30)," +
                    "color_accent VARCHAR(20)," +
                    "founded_year INT," +
                    "member_count INT DEFAULT 0," +
                    "admin_id INT," +
                    "status VARCHAR(20) DEFAULT 'active'" +
                    ")");

                    // Backward-compatible migrations for older local DB files.
                    st.execute("ALTER TABLE clubs ADD COLUMN IF NOT EXISTS admin_id INT");
                    st.execute("ALTER TABLE clubs ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'active'");

            st.execute("CREATE TABLE IF NOT EXISTS events (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "club_id INT NOT NULL," +
                    "title VARCHAR(150) NOT NULL," +
                    "description TEXT," +
                    "event_date DATE NOT NULL," +
                    "event_time VARCHAR(20)," +
                    "venue VARCHAR(150)," +
                    "image_url VARCHAR(255)," +
                    "max_seats INT DEFAULT 100," +
                    "status VARCHAR(20) DEFAULT 'upcoming'," +
                    "CONSTRAINT fk_events_club FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE" +
                    ")");

                    // Backward-compatible migrations for older local DB files.
                    st.execute("ALTER TABLE events ADD COLUMN IF NOT EXISTS image_url VARCHAR(255)");
                    st.execute("ALTER TABLE events ADD COLUMN IF NOT EXISTS max_seats INT DEFAULT 100");
                    st.execute("ALTER TABLE events ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'upcoming'");

            st.execute("CREATE TABLE IF NOT EXISTS memberships (" +
                    "membership_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "club_id INT NOT NULL," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "CONSTRAINT uq_membership UNIQUE(user_id, club_id)," +
                    "CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                    "CONSTRAINT fk_membership_club FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE" +
                    ")");

            st.execute("CREATE TABLE IF NOT EXISTS announcements (" +
                    "announcement_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "club_id INT NOT NULL," +
                    "title VARCHAR(150) NOT NULL," +
                    "body VARCHAR(500) NOT NULL," +
                    "priority VARCHAR(20) DEFAULT 'normal'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "CONSTRAINT fk_announcement_club FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE" +
                    ")");

            st.execute("CREATE TABLE IF NOT EXISTS event_registrations (" +
                    "registration_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "event_id INT NOT NULL," +
                    "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "CONSTRAINT uq_event_registration UNIQUE(user_id, event_id)," +
                    "CONSTRAINT fk_event_reg_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                    "CONSTRAINT fk_event_reg_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE" +
                    ")");
        }

        int clubCount = 0;
        int userCount = 0;
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM clubs")) {
            if (rs.next()) {
                clubCount = rs.getInt(1);
            }
        }
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next()) {
                userCount = rs.getInt(1);
            }
        }

        if (userCount == 0) {
            try (Statement st = con.createStatement()) {
                st.execute("INSERT INTO users(name,email,roll_number,password_hash,role,bio) VALUES " +
                        "('Aarav Student','student@ccms.local','CS2025001','student123','student','Computer science student.')," +
                        "('Nisha ClubAdmin','admin@ccms.local','CS2025002','admin123','club_admin','Club coordinator.')," +
                        "('Super Admin','super@ccms.local','ADM2025001','super123','super_admin','System administrator.')");
            }
        }

        if (clubCount == 0) {
            try (Statement st = con.createStatement()) {
                st.execute("INSERT INTO clubs(name, short_name, description, category, icon, color_accent, founded_year, member_count) VALUES " +
                        "('CodeCraft Club', 'CODE', 'Programming, software design and hackathon preparation.', 'Technology', 'CODE', '#2c6bff', 2018, 45)," +
                        "('Cultural Society', 'CULT', 'Music, theatre, public speaking and creative arts.', 'Cultural', 'CULT', '#17a36b', 2015, 38)," +
                        "('Sports Forum', 'SPORT', 'College-level indoor and outdoor sports activities.', 'Sports', 'SPORT', '#e67e22', 2014, 52)");

                st.execute("INSERT INTO events(club_id, title, description, event_date, event_time, venue, image_url, status) VALUES " +
                        "(1, 'Web Development Bootcamp', 'Frontend and backend workshop series.', CURRENT_DATE + 5, '10:00 AM', 'Lab 3', '', 'upcoming')," +
                        "(2, 'Open Mic Evening', 'Club talent showcase and performances.', CURRENT_DATE + 10, '4:00 PM', 'Auditorium', '', 'upcoming')," +
                        "(3, 'Inter-Department Football', 'Knockout tournament between departments.', CURRENT_DATE + 15, '7:00 AM', 'Main Ground', '', 'upcoming')");

                st.execute("INSERT INTO memberships(user_id, club_id) VALUES (1,1),(1,2),(2,1)");

                st.execute("INSERT INTO announcements(club_id, title, body, priority) VALUES " +
                        "(1,'Workshop Registration Open','Registration is open for the web development bootcamp.','high')," +
                        "(2,'Audition Notice','Open mic shortlist auditions are this Friday.','normal')," +
                        "(3,'Practice Schedule','Football practice starts every morning at 6:30 AM.','low')");
            }
        }

        h2Initialized = true;
    }

    private static String readOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
