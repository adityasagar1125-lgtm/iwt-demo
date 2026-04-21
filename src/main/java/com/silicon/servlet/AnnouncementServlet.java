package com.silicon.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.silicon.dao.AnnouncementDAO;
import com.silicon.model.Announcement;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/announcements")
public class AnnouncementServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        Integer clubId = null;
        try {
            String clubIdParam = req.getParameter("clubId");
            if (clubIdParam != null && !clubIdParam.isBlank()) {
                clubId = Integer.parseInt(clubIdParam);
            }
        } catch (NumberFormatException ignore) {
        }

        List<Announcement> list = new AnnouncementDAO().getAnnouncements(clubId);
        JSONArray items = new JSONArray();
        for (Announcement a : list) {
            JSONObject row = new JSONObject();
            row.put("announcementId", a.getAnnouncementId());
            row.put("clubId", a.getClubId());
            row.put("clubName", a.getClubName());
            row.put("title", a.getTitle());
            row.put("body", a.getBody());
            row.put("priority", a.getPriority());
            row.put("createdAt", a.getCreatedAt());
            items.put(row);
        }

        JSONObject out = new JSONObject();
        out.put("items", items);
        res.getWriter().write(out.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        JSONObject body = readJson(req);
        Announcement a = new Announcement();
        a.setClubId(body.optInt("clubId", -1));
        a.setTitle(body.optString("title", "").trim());
        a.setBody(body.optString("body", "").trim());
        a.setPriority(body.optString("priority", "normal").trim());

        if (a.getClubId() <= 0 || a.getTitle().isEmpty() || a.getBody().isEmpty()) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"clubId, title and body are required\"}");
            return;
        }

        boolean ok = new AnnouncementDAO().createAnnouncement(a);
        if (!ok) {
            res.setStatus(500);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to post announcement\"}");
            return;
        }

        res.getWriter().write("{\"status\":\"success\"}");
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
}
