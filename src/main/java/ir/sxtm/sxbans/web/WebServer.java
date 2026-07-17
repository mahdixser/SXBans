package ir.sxtm.sxbans.web;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.web.controllers.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer {
    private final SXBans plugin;
    private Server server;
    private boolean running;
    private final ConcurrentHashMap<String, String> sessionCache;

    public WebServer(SXBans plugin) {
        this.plugin = plugin;
        this.running = false;
        this.sessionCache = new ConcurrentHashMap<>();
    }

    public void start() {
        if (running) {
            plugin.getSXBansLogger().warning("Web server is already running");
            return;
        }

        try {
            int port = plugin.getConfigManager().getWebPort();
            String host = plugin.getConfigManager().getWebHost();

            server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setHost(host);
            connector.setPort(port);
            server.addConnector(connector);

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            // ===== WebSocket REMOVED =====

            // ===== STATIC RESOURCES =====
            context.addServlet(new ServletHolder(new StaticResourceServlet(plugin)), "/css/*");
            context.addServlet(new ServletHolder(new StaticResourceServlet(plugin)), "/js/*");
            context.addServlet(new ServletHolder(new StaticResourceServlet(plugin)), "/fonts/*");
            context.addServlet(new ServletHolder(new StaticResourceServlet(plugin)), "/images/*");

            // ===== API ENDPOINTS =====
            context.addServlet(new ServletHolder(new AuthController(plugin)), "/api/auth/*");
            context.addServlet(new ServletHolder(new DashboardController(plugin)), "/api/dashboard/*");
            context.addServlet(new ServletHolder(new PlayerController(plugin)), "/api/players/*");
            context.addServlet(new ServletHolder(new PunishmentController(plugin)), "/api/punishments/*");
            context.addServlet(new ServletHolder(new SettingsController(plugin)), "/api/settings/*");

            // ===== HTML PAGES =====
            context.addServlet(new ServletHolder(new PageServlet(plugin, "login")), "/login");
            context.addServlet(new ServletHolder(new PageServlet(plugin, "dashboard")), "/dashboard");
            context.addServlet(new ServletHolder(new PageServlet(plugin, "players")), "/players");
            context.addServlet(new ServletHolder(new PageServlet(plugin, "history")), "/history");
            context.addServlet(new ServletHolder(new PageServlet(plugin, "settings")), "/settings");
            context.addServlet(new ServletHolder(new PageServlet(plugin, "player-card")), "/player/*");
            context.addServlet(new ServletHolder(new PageServlet(plugin, "player-card-embed")), "/embed/*");

            // ===== ERROR PAGES =====
            context.addServlet(new ServletHolder(new ErrorPageServlet(plugin, "404")), "/404");
            context.addServlet(new ServletHolder(new ErrorPageServlet(plugin, "500")), "/500");
            context.addServlet(new ServletHolder(new ErrorPageServlet(plugin, "error")), "/error");

            // ===== ROOT =====
            context.addServlet(new ServletHolder(new RootServlet()), "/");

            server.start();
            running = true;
            plugin.getSXBansLogger().info("Web server started on " + host + ":" + port);

        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (!running || server == null) return;
        try {
            server.stop();
            running = false;
            plugin.getSXBansLogger().info("Web server stopped");
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to stop web server: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void invalidateSession(String token) {
        if (token != null) sessionCache.remove(token);
    }

    public void createSession(String token, String username) {
        if (token != null && username != null) sessionCache.put(token, username);
    }

    public boolean isValidSession(String token) {
        if (token == null) return false;
        return sessionCache.containsKey(token);
    }

    public String getUsernameFromSession(String token) {
        if (token == null) return null;
        return sessionCache.get(token);
    }

    // ======================================================================
    // SERVLETS
    // ======================================================================

    private static class RootServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.sendRedirect("/login");
        }
    }

    private static class StaticResourceServlet extends HttpServlet {
        private final SXBans plugin;

        public StaticResourceServlet(SXBans plugin) {
            this.plugin = plugin;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String resourcePath = "/web" + path;
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);

            if (inputStream == null) {
                File externalFile = new File(plugin.getDataFolder(), "web" + path);
                if (externalFile.exists()) {
                    inputStream = new FileInputStream(externalFile);
                }
            }

            if (inputStream == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String contentType = getContentType(path);
            resp.setContentType(contentType);
            resp.setStatus(HttpServletResponse.SC_OK);

            try (OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".gif")) return "image/gif";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".woff")) return "font/woff";
            if (path.endsWith(".woff2")) return "font/woff2";
            if (path.endsWith(".ttf")) return "font/ttf";
            return "application/octet-stream";
        }
    }

    private static class PageServlet extends HttpServlet {
        private final SXBans plugin;
        private final String page;

        public PageServlet(SXBans plugin, String page) {
            this.plugin = plugin;
            this.page = page;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!page.equals("login")) {
                String sessionToken = getSessionToken(req);
                if (sessionToken == null || !plugin.getWebServer().isValidSession(sessionToken)) {
                    resp.sendRedirect("/login");
                    return;
                }
            }

            String resourcePath = "/web/html/" + page + ".html";
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);

            if (inputStream == null) {
                File externalFile = new File(plugin.getDataFolder(), "web/html/" + page + ".html");
                if (externalFile.exists()) {
                    inputStream = new FileInputStream(externalFile);
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }

            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            try { inputStream.close(); } catch (IOException ignored) {}

            html = processHtml(html, req);

            resp.setContentType("text/html");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(html);
        }

        private String processHtml(String html, HttpServletRequest req) {
            String sessionToken = getSessionToken(req);

            if (sessionToken != null) {
                String username = plugin.getWebServer().getUsernameFromSession(sessionToken);
                if (username != null) {
                    html = html.replace("{{username}}", username);
                    var user = plugin.getWebUsersManager().getUser(username);
                    html = html.replace("{{user_level}}", user != null ? String.valueOf(user.getLevel()) : "0");
                }
            }

            html = html.replace("{{version}}", plugin.getDescription().getVersion());
            String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
            html = html.replace("{{base_url}}", baseUrl);

            return html;
        }

        private String getSessionToken(HttpServletRequest req) {
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }

            String cookieHeader = req.getHeader("Cookie");
            if (cookieHeader != null) {
                for (String cookie : cookieHeader.split(";")) {
                    String trimmed = cookie.trim();
                    if (trimmed.startsWith("session=")) {
                        return trimmed.substring(8);
                    }
                }
            }

            return null;
        }
    }

    // ======================================================================
    // ERROR PAGE SERVLET
    // ======================================================================
    private static class ErrorPageServlet extends HttpServlet {
        private final SXBans plugin;
        private final String errorPage;

        public ErrorPageServlet(SXBans plugin, String errorPage) {
            this.plugin = plugin;
            this.errorPage = errorPage;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String resourcePath = "/web/html/" + errorPage + ".html";
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);

            if (inputStream == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            try { inputStream.close(); } catch (IOException ignored) {}

            html = html.replace("{{version}}", plugin.getDescription().getVersion());

            resp.setContentType("text/html");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(html);
        }
    }
}