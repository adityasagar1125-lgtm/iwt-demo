CREATE DATABASE IF NOT EXISTS silicon_clubhub;
USE silicon_clubhub;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE,
    roll_number VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('student','club_admin','super_admin') DEFAULT 'student',
    bio VARCHAR(300),
    profile_picture VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clubs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    short_name VARCHAR(30),
    description TEXT,
    category VARCHAR(50),
    icon VARCHAR(30),
    color_accent VARCHAR(20),
    founded_year INT,
    member_count INT DEFAULT 0,
    admin_id INT,
    status ENUM('pending','active','inactive') DEFAULT 'active',
    FOREIGN KEY (admin_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS memberships (
    membership_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    club_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_membership (user_id, club_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    club_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    event_date DATE NOT NULL,
    event_time VARCHAR(20),
    venue VARCHAR(150),
    image_url VARCHAR(255),
    max_seats INT DEFAULT 100,
    status ENUM('upcoming','ongoing','past') DEFAULT 'upcoming',
    FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS event_registrations (
    registration_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_event_registration (user_id, event_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS announcements (
    announcement_id INT AUTO_INCREMENT PRIMARY KEY,
    club_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    body VARCHAR(500) NOT NULL,
    priority ENUM('high','normal','low') DEFAULT 'normal',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE
);

INSERT INTO users(name,email,roll_number,password_hash,role,bio) VALUES
('Aarav Student','student@ccms.local','CS2025001','student123','student','Computer science student.'),
('Nisha ClubAdmin','admin@ccms.local','CS2025002','admin123','club_admin','Club coordinator.'),
('Super Admin','super@ccms.local','ADM2025001','super123','super_admin','System administrator.')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO clubs(name,short_name,description,category,icon,color_accent,founded_year,member_count,admin_id,status) VALUES
('CodeCraft Club','CODE','Programming, software design and hackathon preparation.','Technology','CODE','#2c6bff',2018,45,2,'active'),
('Cultural Society','CULT','Music, theatre, public speaking and creative arts.','Cultural','CULT','#17a36b',2015,38,2,'active'),
('Sports Forum','SPORT','College-level indoor and outdoor sports activities.','Sports','SPORT','#e67e22',2014,52,2,'active')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO memberships(user_id,club_id) VALUES (1,1),(1,2),(2,1)
ON DUPLICATE KEY UPDATE joined_at = joined_at;

INSERT INTO events(club_id,title,description,event_date,event_time,venue,max_seats,status) VALUES
(1,'Web Development Bootcamp','Frontend and backend workshop series.','2026-06-10','10:00 AM','Lab 3',120,'upcoming'),
(2,'Open Mic Evening','Club talent showcase and performances.','2026-06-15','4:00 PM','Auditorium',200,'upcoming'),
(3,'Inter-Department Football','Knockout tournament between departments.','2026-06-20','7:00 AM','Main Ground',100,'upcoming')
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO announcements(club_id,title,body,priority) VALUES
(1,'Workshop Registration Open','Registration is open for the web development bootcamp.','high'),
(2,'Audition Notice','Open mic shortlist auditions are this Friday.','normal'),
(3,'Practice Schedule','Football practice starts every morning at 6:30 AM.','low');
