package com.silicon.servlet;

import java.io.BufferedReader;
import java.io.IOException;

import org.json.JSONObject;

import com.silicon.dao.ClubDAO;
import com.silicon.dao.UserDAO;
import com.silicon.model.User;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/api/admin/clubs/pending", "/api/admin/clubs/status"})
public class ClubApprovalServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        int userId;
        try {
            userId = Integer.parseInt(req.getParameter("userId"));
        } catch (NumberFormatException ex) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"userId is required\"}");
            return;
        }

        User user = new UserDAO().getById(userId);
        if (user == null || !"super_admin".equals(user.getRole())) {
            res.setStatus(403);
            res.getWriter().write("{\"error\":\"Only super admin can view pending clubs\"}");
            return;
        }

        org.json.JSONArray items = new org.json.JSONArray();
        for (com.silicon.model.Club c : new ClubDAO().getPendingClubs()) {
            JSONObject row = new JSONObject();
            row.put("id", c.getId());
            row.put("name", c.getName());
            row.put("category", c.getCategory());
            row.put("memberCount", c.getMemberCount());
            row.put("status", c.getStatus());
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
        int userId = body.optInt("userId", -1);
        int clubId = body.optInt("clubId", -1);
        String action = body.optString("action", "").trim().toLowerCase();

        if (userId <= 0 || clubId <= 0 || (!"approve".equals(action) && !"reject".equals(action))) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"userId, clubId and action(approve|reject) are required\"}");
            return;
        }

        User user = new UserDAO().getById(userId);
        if (user == null || !"super_admin".equals(user.getRole())) {
            res.setStatus(403);
            res.getWriter().write("{\"error\":\"Only super admin can approve/reject clubs\"}");
            return;
        }

        String nextStatus = "approve".equals(action) ? "active" : "rejected";
        boolean ok = new ClubDAO().updateClubStatus(clubId, nextStatus);
        if (!ok) {
            res.setStatus(404);
            res.getWriter().write("{\"error\":\"Club not found\"}");
            return;
        }

        res.getWriter().write("{\"status\":\"success\",\"clubId\":" + clubId + ",\"nextStatus\":\"" + nextStatus + "\"}");
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
