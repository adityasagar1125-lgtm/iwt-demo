package com.silicon.servlet;

import java.io.BufferedReader;
import java.io.IOException;

import org.json.JSONObject;

import com.silicon.dao.EventDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/events/register")
public class EventRegistrationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        JSONObject body = readJson(req);
        int userId = body.optInt("userId", -1);
        int eventId = body.optInt("eventId", -1);

        if (userId <= 0 || eventId <= 0) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"userId and eventId are required\"}");
            return;
        }

        EventDAO dao = new EventDAO();
        boolean ok = dao.registerUserForEvent(userId, eventId);
        if (!ok) {
            res.setStatus(409);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Already registered or invalid event\"}");
            return;
        }

        int count = dao.getRegistrationCount(eventId);
        JSONObject out = new JSONObject();
        out.put("status", "registered");
        out.put("seats", count);
        res.getWriter().write(out.toString());
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
