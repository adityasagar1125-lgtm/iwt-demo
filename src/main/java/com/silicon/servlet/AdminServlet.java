package com.silicon.servlet;

import java.io.IOException;

import org.json.JSONObject;

import com.silicon.dao.AdminDAO;
import com.silicon.dao.UserDAO;
import com.silicon.model.User;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/admin/dashboard")
public class AdminServlet extends HttpServlet {

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
        if (user == null) {
            res.setStatus(401);
            res.getWriter().write("{\"error\":\"Invalid user\"}");
            return;
        }

        String role = user.getRole();
        if (!("super_admin".equals(role) || "club_admin".equals(role))) {
            res.setStatus(403);
            res.getWriter().write("{\"error\":\"Access denied\"}");
            return;
        }

        AdminDAO dao = new AdminDAO();
        JSONObject out = new JSONObject();
        out.put("clubs", dao.getTotalClubs());
        out.put("members", dao.getTotalMemberships());
        out.put("events", dao.getTotalEvents());
        out.put("users", dao.getTotalUsers());
        res.getWriter().write(out.toString());
    }
}
