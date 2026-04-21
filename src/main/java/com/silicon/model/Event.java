package com.silicon.model;

public class Event {
    private int id;
    private int clubId;
    private String clubName;
    private String clubIcon;
    private String title;
    private String description;
    private String eventDate;
    private String eventTime;
    private String venue;
    private String imageUrl;
    private int maxSeats;
    private int registeredCount;
    private String status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClubId() { return clubId; }
    public void setClubId(int clubId) { this.clubId = clubId; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getClubIcon() { return clubIcon; }
    public void setClubIcon(String clubIcon) { this.clubIcon = clubIcon; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getMaxSeats() { return maxSeats; }
    public void setMaxSeats(int maxSeats) { this.maxSeats = maxSeats; }

    public int getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(int registeredCount) { this.registeredCount = registeredCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
