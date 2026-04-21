package com.silicon.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.silicon.dao.AnnouncementDAO;
import com.silicon.dao.ClubDAO;
import com.silicon.dao.MembershipDAO;
import com.silicon.dao.UserDAO;
import com.silicon.model.Announcement;
import com.silicon.model.Club;
import com.silicon.model.User;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/clubs")
public class ClubServlet extends HttpServlet {

    private static final String DEFAULT_ICON = "CLUB";
    private static final String DEFAULT_COLOR = "#1a6fff";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.setHeader("Access-Control-Allow-Origin", "*");

        ClubDAO dao = new ClubDAO();
        String idParam = req.getParameter("id");

        if (idParam != null) {
            int id;
            try {
                id = Integer.parseInt(idParam);
            } catch (NumberFormatException ex) {
                res.setStatus(400);
                res.getWriter().write("{\"error\":\"Invalid club id\"}");
                return;
            }

            Club c = dao.getClubById(id);
            if (c != null) {
                JSONObject payload = new JSONObject();
                payload.put("club", clubToJson(c));

                JSONArray members = new JSONArray();
                List<String> memberNames = dao.getClubMembers(id);
                for (String name : memberNames) {
                    members.put(name);
                }
                payload.put("members", members);

                JSONArray announcements = new JSONArray();
                List<Announcement> list = new AnnouncementDAO().getAnnouncements(id);
                for (Announcement a : list) {
                    JSONObject row = new JSONObject();
                    row.put("announcementId", a.getAnnouncementId());
                    row.put("clubId", a.getClubId());
                    row.put("title", a.getTitle());
                    row.put("body", a.getBody());
                    row.put("priority", a.getPriority());
                    row.put("createdAt", a.getCreatedAt());
                    announcements.put(row);
                }
                payload.put("announcements", announcements);

                String userIdParam = req.getParameter("userId");
                if (userIdParam != null) {
                    try {
                        int userId = Integer.parseInt(userIdParam);
                        boolean member = new MembershipDAO().isMember(userId, id);
                        payload.put("isMember", member);
                    } catch (NumberFormatException ignore) {
                    }
                }

                res.getWriter().write(payload.toString());
                return;
            } else {
                res.setStatus(404);
                res.getWriter().write("{\"error\":\"Club not found\"}");
                return;
            }
        }

        JSONArray result = new JSONArray();
        List<Club> clubs = dao.getAllClubs();
        for (Club c : clubs) {
            result.put(clubToJson(c));
        }
        JSONObject payload = new JSONObject();
        payload.put("clubs", result);
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

        String name = body.optString("name", "").trim();
        if (name.isEmpty()) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"Club name is required\"}");
            return;
        }

        Club club = new Club();
        club.setName(name);
        club.setShortName(body.optString("shortName", "").trim());
        club.setDescription(body.optString("description", "").trim());
        club.setCategory(body.optString("category", "General").trim());
        club.setIcon(body.optString("icon", DEFAULT_ICON).trim());
        club.setColorAccent(body.optString("colorAccent", DEFAULT_COLOR).trim());
        club.setFoundedYear(body.optInt("foundedYear", 2020));
        club.setMemberCount(body.optInt("memberCount", 0));

        int actorUserId = body.optInt("actorUserId", -1);
        if (actorUserId > 0) {
            User actor = new UserDAO().getById(actorUserId);
            if (actor == null) {
                res.setStatus(401);
                res.getWriter().write("{\"error\":\"Invalid actor user\"}");
                return;
            }
            String role = actor.getRole();
            if (!("super_admin".equals(role) || "club_admin".equals(role))) {
                res.setStatus(403);
                res.getWriter().write("{\"error\":\"Only club admins can create clubs\"}");
                return;
            }
            club.setStatus("super_admin".equals(role) ? "active" : "pending");
        } else {
            club.setStatus("pending");
        }

        Club created = new ClubDAO().createClub(club);
        if (created == null) {
            res.setStatus(500);
            res.getWriter().write("{\"error\":\"Failed to create club\"}");
            return;
        }

        res.setStatus(201);
        res.getWriter().write(clubToJson(created).toString());
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
        String name = body.optString("name", "").trim();
        if (id <= 0 || name.isEmpty()) {
            res.setStatus(400);
            res.getWriter().write("{\"error\":\"id and name are required\"}");
            return;
        }

        Club club = new Club();
        club.setId(id);
        club.setName(name);
        club.setShortName(body.optString("shortName", "").trim());
        club.setDescription(body.optString("description", "").trim());
        club.setCategory(body.optString("category", "General").trim());
        club.setIcon(body.optString("icon", DEFAULT_ICON).trim());
        club.setColorAccent(body.optString("colorAccent", DEFAULT_COLOR).trim());
        club.setFoundedYear(body.optInt("foundedYear", 2020));
        club.setMemberCount(body.optInt("memberCount", 0));

        Club updated = new ClubDAO().updateClub(club);
        if (updated == null) {
            res.setStatus(404);
            res.getWriter().write("{\"error\":\"Club not found or not updated\"}");
            return;
        }

        res.getWriter().write(clubToJson(updated).toString());
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

        boolean deleted = new ClubDAO().deleteClub(id);
        if (!deleted) {
            res.setStatus(404);
            res.getWriter().write("{\"error\":\"Club not found\"}");
            return;
        }

        res.getWriter().write("{\"message\":\"Club deleted\"}");
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

    private JSONObject clubToJson(Club c) {
        JSONObject obj = new JSONObject();
        obj.put("id", c.getId());
        obj.put("name", c.getName());
        obj.put("shortName", c.getShortName());
        obj.put("description", c.getDescription());
        obj.put("category", c.getCategory());
        obj.put("icon", c.getIcon());
        obj.put("colorAccent", c.getColorAccent());
        obj.put("status", c.getStatus() == null ? "pending" : c.getStatus());
        obj.put("foundedYear", c.getFoundedYear());
        obj.put("memberCount", c.getMemberCount());
        return obj;
    }
}
