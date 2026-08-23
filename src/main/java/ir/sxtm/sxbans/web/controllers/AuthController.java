package ir.sxtm.sxbans.web.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.WebUser;
import ir.sxtm.sxbans.web.models.WebResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthController extends HttpServlet {
    private final SXBans plugin;
    private final Gson gson = new Gson();

    public AuthController(SXBans plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");

        try {
            if (path == null || path.equals("/login")) {
                handleLogin(req, resp);
            } else if (path.equals("/logout")) {
                handleLogout(req, resp);
            } else if (path.equals("/validate")) {
                handleValidate(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        JsonObject body = parseBody(req);
        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();

        if (plugin.getWebUsersManager().authenticate(username, password)) {
            String token = plugin.getWebUsersManager().createSession(username);
            if (token != null) {
                plugin.getWebServer().createSession(token, username);

                WebUser user = plugin.getWebUsersManager().getUser(username);
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("token", token);
                response.addProperty("username", username);
                response.addProperty("level", user.getLevel());
                response.add("permissions", gson.toJsonTree(user.getPermissions()));

                resp.getWriter().write(gson.toJson(response));
                return;
            }
        }

        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.getWriter().write(gson.toJson(new WebResponse(false, "Invalid credentials")));
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = getAuthToken(req);
        if (token != null) {
            plugin.getWebUsersManager().invalidateSession(token);
            plugin.getWebServer().invalidateSession(token);
        }
        resp.getWriter().write(gson.toJson(new WebResponse(true, "Logged out successfully")));
    }

    private void handleValidate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = getAuthToken(req);
        boolean valid = token != null && plugin.getWebServer().isValidSession(token);

        JsonObject response = new JsonObject();
        response.addProperty("valid", valid);
        if (valid) {
            String username = plugin.getWebServer().getUsernameFromSession(token);
            WebUser user = plugin.getWebUsersManager().getUser(username);
            response.addProperty("username", username);
            response.addProperty("level", user.getLevel());
            response.add("permissions", gson.toJsonTree(user.getPermissions()));
        }
        resp.getWriter().write(gson.toJson(response));
    }

    private JsonObject parseBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return gson.fromJson(sb.toString(), JsonObject.class);
    }

    private String getAuthToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        String cookieHeader = req.getHeader("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && parts[0].equals("session")) {
                    return parts[1];
                }
            }
        }

        return null;
    }
}