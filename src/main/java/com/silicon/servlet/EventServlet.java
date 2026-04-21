package com.silicon.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.silicon.dao.EventDAO;
import com.silicon.model.Event;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/events")
public class EventServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.setHeader("Access-Control-Allow-Origin", "*");

        EventDAO dao = new EventDAO();
        String idParam = req.getParameter("id");
        String clubIdParam = req.getParameter("clubId");
        String upcomingParam = req.getParameter("upcoming");

        List<Event> events;

        if (idParam != null) {
            int id;
            try {
                id = Integer.parseInt(idParam);
            } catch (NumberFormatException ex) {
                res.setStatus(400);
                res.getWriter().write("{\"error\":\"Invalid event id\"}");
                return;
            }

            Event event = dao.getEventById(id);
            if (event == null) {
                res.setStatus(404);
                res.getWriter().write("{\"error\":\"Event not found\"}");
                return;
            }
            int registrations = dao.getRegistrationCount(event.getId());
            event.setRegisteredCount(registrations);
            JSONObject payload = new JSONObject();
            payload.put("event", eventToJson(event));
            res.getWriter().write(payload.toString());
            return;
        }

        if (clubIdParam != null) {
            int clubId;
            try {
                clubId = Integer.parseInt(clubIdParam);
            } catch (NumberFormatException ex) {
                res.setStatus(400);
                res.getWriter().write("{\"error\":\"Invalid club id\"}");
                return;
            }
            events = dao.getEventsByClub(clubId);
        } else if ("true".equals(upcomingParam)) {
            events = dao.getUpcomingEvents(6);
        } else {
            events = dao.getAllEvents();
        }

        JSONArray result = new JSONArray();
        for (Event e : events) {
            int registrations = dao.getRegistrationCount(e.getId());
            e.setRegisteredCount(registrations);
            result.put(eventToJson(e));
        }
        JSONObject payload = new JSONObject();
        payload.put("events", result);
        res.getWriter().write(payload.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        JSONObject body;
        try {
            body = readJson(req);
        } catch (JSONException ex) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"Invalid JSON request body\"}");
            return;
        }

        int clubId = body.optInt("clubId", -1);
        String title = body.optString("title", "").trim();
        String eventDate = body.optString("eventDate", "").trim();

        if (clubId <= 0 || title.isEmpty() || eventDate.isEmpty()) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"clubId, title and eventDate are required\"}");
            return;
        }

        Event event = new Event();
        event.setClubId(clubId);
        event.setTitle(title);
        event.setDescription(body.optString("description", "").trim());
        event.setEventDate(eventDate);
        event.setEventTime(body.optString("eventTime", "").trim());
        event.setVenue(body.optString("venue", "").trim());
        event.setImageUrl(body.optString("imageUrl", "").trim());
        event.setMaxSeats(body.optInt("maxSeats", 100));
        event.setStatus(body.optString("status", "upcoming").trim());

        Event created = new EventDAO().createEvent(event);
        if (created == null) {
            res.setStatus(500);
            res.getWriter().write("{\"error\":\"Failed to create event\"}");
            return;
        }

        res.setStatus(201);
        res.getWriter().write(eventToJson(created).toString());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        JSONObject body;
        try {
            body = readJson(req);
        } catch (JSONException ex) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"Invalid JSON request body\"}");
            return;
        }

        int id = body.optInt("id", -1);
        int clubId = body.optInt("clubId", -1);
        String title = body.optString("title", "").trim();
        String eventDate = body.optString("eventDate", "").trim();

        if (id <= 0 || clubId <= 0 || title.isEmpty() || eventDate.isEmpty()) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"id, clubId, title and eventDate are required\"}");
            return;
        }

        Event event = new Event();
        event.setId(id);
        event.setClubId(clubId);
        event.setTitle(title);
        event.setDescription(body.optString("description", "").trim());
        event.setEventDate(eventDate);
        event.setEventTime(body.optString("eventTime", "").trim());
        event.setVenue(body.optString("venue", "").trim());
        event.setImageUrl(body.optString("imageUrl", "").trim());
        event.setMaxSeats(body.optInt("maxSeats", 100));
        event.setStatus(body.optString("status", "upcoming").trim());

        Event updated = new EventDAO().updateEvent(event);
        if (updated == null) {
            res.setStatus(404);
            res.getWriter().write("{\"error\":\"Event not found or not updated\"}");
            return;
        }

        res.getWriter().write(eventToJson(updated).toString());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String idParam = req.getParameter("id");
        int id;
        try {
            id = Integer.parseInt(idParam);
        } catch (NumberFormatException ex) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"Valid id query parameter is required\"}");
            return;
        }

        boolean deleted = new EventDAO().deleteEvent(id);
        if (!deleted) {
            res.setStatus(404);
            res.getWriter().write("{\"error\":\"Event not found\"}");
            return;
        }

        res.getWriter().write("{\"message\":\"Event deleted\"}");
    }

    private JSONObject readJson(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return new JSONObject(sb.toString());
    }

    private JSONObject eventToJson(Event e) {
        JSONObject obj = new JSONObject();
        obj.put("id", e.getId());
        obj.put("clubId", e.getClubId());
        obj.put("clubName", e.getClubName());
        obj.put("clubIcon", e.getClubIcon());
        obj.put("title", e.getTitle());
        obj.put("description", e.getDescription());
        obj.put("eventDate", e.getEventDate() != null ? e.getEventDate() : "");
        obj.put("eventTime", e.getEventTime() != null ? e.getEventTime() : "");
        obj.put("venue", e.getVenue() != null ? e.getVenue() : "");
        obj.put("imageUrl", e.getImageUrl() != null ? e.getImageUrl() : "");
        obj.put("maxSeats", e.getMaxSeats());
        obj.put("registeredCount", e.getRegisteredCount());
        obj.put("remainingSeats", Math.max(0, e.getMaxSeats() - e.getRegisteredCount()));
        obj.put("status", e.getStatus());
        return obj;
    }
}
