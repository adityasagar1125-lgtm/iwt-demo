package com.silicon.servlet;

import java.io.BufferedReader;
import java.io.IOException;

import org.json.JSONObject;

import com.silicon.dao.ClubDAO;
import com.silicon.dao.MembershipDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/clubs/join")
public class ClubMembershipServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        JSONObject body = readJson(req);
        int userId = body.optInt("userId", -1);
        int clubId = body.optInt("clubId", -1);
        String action = body.optString("action", "join");

        if (userId <= 0 || clubId <= 0) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"userId and clubId are required\"}");
            return;
        }

        MembershipDAO dao = new MembershipDAO();
        ClubDAO clubDAO = new ClubDAO();

        boolean ok;
        String status;
        if ("leave".equalsIgnoreCase(action)) {
            ok = dao.leaveClub(userId, clubId);
            status = "left";
        } else {
            ok = dao.joinClub(userId, clubId);
            status = "joined";
        }

        if (!ok) {
            res.setStatus(409);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Membership action failed\"}");
            return;
        }

        clubDAO.syncMemberCount(clubId);
        int count = clubDAO.getMemberCountByClub(clubId);
        JSONObject out = new JSONObject();
        out.put("status", status);
        out.put("memberCount", count);
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
