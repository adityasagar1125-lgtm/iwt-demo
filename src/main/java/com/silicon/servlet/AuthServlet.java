package com.silicon.servlet;

import java.io.BufferedReader;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import com.silicon.dao.UserDAO;
import com.silicon.model.User;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/api/auth/register", "/api/auth/login", "/api/auth/profile", "/api/auth/change-password"})
public class AuthServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        JSONObject body;
        try {
            body = readJson(req);
        } catch (JSONException ex) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid JSON\"}");
            return;
        }

        String path = req.getServletPath();
        if ("/api/auth/register".equals(path)) {
            handleRegister(body, res);
            return;
        }
        if ("/api/auth/login".equals(path)) {
            handleLogin(body, res);
            return;
        }
        if ("/api/auth/change-password".equals(path)) {
            handleChangePassword(body, res);
            return;
        }
        res.setStatus(404);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        if (!"/api/auth/profile".equals(req.getServletPath())) {
            res.setStatus(404);
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(req.getParameter("userId"));
        } catch (NumberFormatException ex) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"userId is required\"}");
            return;
        }

        User user = new UserDAO().getById(userId);
        if (user == null) {
            res.setStatus(404);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"User not found\"}");
            return;
        }

        res.getWriter().write(userToJson(user).toString());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        if (!"/api/auth/profile".equals(req.getServletPath())) {
            res.setStatus(404);
            return;
        }

        JSONObject body;
        try {
            body = readJson(req);
        } catch (JSONException ex) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid JSON\"}");
            return;
        }

        int userId = body.optInt("userId", -1);
        if (userId <= 0) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"userId is required\"}");
            return;
        }

        UserDAO dao = new UserDAO();
        User user = dao.getById(userId);
        if (user == null) {
            res.setStatus(404);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"User not found\"}");
            return;
        }

        user.setName(body.optString("name", user.getName()));
        user.setBio(body.optString("bio", user.getBio()));
        user.setProfilePicture(body.optString("profilePicture", user.getProfilePicture()));
        boolean ok = dao.updateProfile(user);
        if (!ok) {
            res.setStatus(500);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to update profile\"}");
            return;
        }

        res.getWriter().write(userToJson(user).put("status", "success").toString());
    }

    private void handleRegister(JSONObject body, HttpServletResponse res) throws IOException {
        String name = body.optString("name", "").trim();
        String email = body.optString("email", "").trim();
        String roll = body.optString("rollNumber", "").trim();
        String password = body.optString("password", "").trim();

        if (name.isEmpty() || email.isEmpty() || roll.isEmpty() || password.isEmpty()) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"All fields are required\"}");
            return;
        }

        UserDAO dao = new UserDAO();
        if (dao.emailExists(email)) {
            res.setStatus(409);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Email already registered\"}");
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRollNumber(roll);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(12)));
        user.setRole("student");
        user.setBio("");

        User created = dao.createUser(user);
        if (created == null) {
            res.setStatus(500);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Could not create user\"}");
            return;
        }

        JSONObject out = new JSONObject();
        out.put("status", "success");
        out.put("userId", created.getUserId());
        out.put("role", created.getRole());
        res.getWriter().write(out.toString());
    }

    private void handleLogin(JSONObject body, HttpServletResponse res) throws IOException {
        String email = body.optString("email", "").trim();
        String password = body.optString("password", "").trim();

        if (email.isEmpty() || password.isEmpty()) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Email and password are required\"}");
            return;
        }

        UserDAO dao = new UserDAO();
        User user = dao.login(email, password);
        if (user == null) {
            res.setStatus(401);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid credentials\"}");
            return;
        }

        String stored = user.getPasswordHash() == null ? "" : user.getPasswordHash();
        boolean valid;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            valid = BCrypt.checkpw(password, stored);
        } else {
            valid = stored.equals(password);
            if (valid) {
                dao.updatePassword(user.getUserId(), BCrypt.hashpw(password, BCrypt.gensalt(12)));
            }
        }

        if (!valid) {
            res.setStatus(401);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid credentials\"}");
            return;
        }

        JSONObject out = userToJson(user);
        out.put("status", "success");
        res.getWriter().write(out.toString());
    }

    private void handleChangePassword(JSONObject body, HttpServletResponse res) throws IOException {
        int userId = body.optInt("userId", -1);
        String currentPassword = body.optString("currentPassword", "").trim();
        String newPassword = body.optString("newPassword", "").trim();

        if (userId <= 0 || currentPassword.isEmpty() || newPassword.length() < 6) {
            res.setStatus(400);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"userId, currentPassword and a 6+ char newPassword are required\"}");
            return;
        }

        UserDAO dao = new UserDAO();
        User user = dao.getById(userId);
        if (user == null) {
            res.setStatus(404);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"User not found\"}");
            return;
        }

        String stored = user.getPasswordHash() == null ? "" : user.getPasswordHash();
        boolean validCurrent;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            validCurrent = BCrypt.checkpw(currentPassword, stored);
        } else {
            validCurrent = stored.equals(currentPassword);
        }

        if (!validCurrent) {
            res.setStatus(401);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Current password is incorrect\"}");
            return;
        }

        boolean updated = dao.updatePassword(userId, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
        if (!updated) {
            res.setStatus(500);
            res.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to update password\"}");
            return;
        }

        res.getWriter().write("{\"status\":\"success\",\"message\":\"Password updated\"}");
    }

    private JSONObject userToJson(User user) {
        JSONObject obj = new JSONObject();
        obj.put("userId", user.getUserId());
        obj.put("name", user.getName());
        obj.put("email", user.getEmail());
        obj.put("rollNumber", user.getRollNumber());
        obj.put("role", user.getRole());
        obj.put("bio", user.getBio() == null ? "" : user.getBio());
        obj.put("profilePicture", user.getProfilePicture() == null ? "" : user.getProfilePicture());
        return obj;
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
