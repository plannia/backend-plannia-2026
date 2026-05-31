package upc.com.pe.backendplannia.iam.infrastructure.authorization.sfs.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletInputStream;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For POST /api/v1/authentication/sign-in, ensures the request is treated as application/json
 * so Spring uses RequestResponseBodyMethodProcessor and reads the body as SignInResource.
 * Some clients (e.g. Swagger UI) may send the body without Content-Type: application/json,
 * which causes ModelAttributeMethodProcessor to run and bind from (empty) form params.
 * Registered in the Security filter chain so it runs before any other filter consumes the body.
 */
public class SignInJsonBodyFilter extends OncePerRequestFilter {

    private static final String SIGN_IN_PATH = "/api/v1/authentication/sign-in";

    private static final Pattern FORM_EMAIL = Pattern.compile("(?:^|&)email=([^&]*)");
    private static final Pattern FORM_USERNAME = Pattern.compile("(?:^|&)username=([^&]*)");
    private static final Pattern FORM_PASSWORD = Pattern.compile("(?:^|&)password=([^&]*)");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!"POST".equalsIgnoreCase(request.getMethod()) || uri == null || !uri.endsWith(SIGN_IN_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }
        byte[] rawBody = request.getInputStream().readAllBytes();
        byte[] jsonBody = toJsonBody(rawBody, request.getContentType());
        HttpServletRequest wrapped = new JsonContentTypeRequestWrapper(request, jsonBody);
        filterChain.doFilter(wrapped, response);
    }

    /**
     * If body is form-urlencoded, convert to JSON so the controller can bind to SignInResource.
     * Otherwise return the raw body as-is (already JSON or empty).
     */
    private static byte[] toJsonBody(byte[] raw, String contentType) {
        if (raw == null || raw.length == 0) {
            return raw;
        }
        String str = new String(raw, StandardCharsets.UTF_8).trim();
        if (str.isEmpty()) {
            return raw;
        }
        boolean formEncoded = contentType != null && contentType.toLowerCase()
                .contains("application/x-www-form-urlencoded");
        if (formEncoded || (str.contains("=") && !str.trim().startsWith("{"))) {
            String email = extractFormParam(str, FORM_EMAIL);
            if (email.isEmpty()) {
                email = extractFormParam(str, FORM_USERNAME);
            }
            String password = extractFormParam(str, FORM_PASSWORD);
            String json = "{\"email\":\"" + escapeJson(email) + "\",\"password\":\"" + escapeJson(password) + "\"}";
            return json.getBytes(StandardCharsets.UTF_8);
        }
        return raw;
    }

    private static String extractFormParam(String form, Pattern p) {
        Matcher m = p.matcher(form);
        return m.find() ? java.net.URLDecoder.decode(m.group(1).trim(), StandardCharsets.UTF_8) : "";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static final class JsonContentTypeRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        JsonContentTypeRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public String getContentType() {
            return MediaType.APPLICATION_JSON_VALUE;
        }

        @Override
        public String getHeader(String name) {
            if ("Content-Type".equalsIgnoreCase(name)) {
                return MediaType.APPLICATION_JSON_VALUE;
            }
            return super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            if ("Content-Type".equalsIgnoreCase(name)) {
                return Collections.enumeration(java.util.List.of(MediaType.APPLICATION_JSON_VALUE));
            }
            return super.getHeaders(name);
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                private final ByteArrayInputStream stream = new ByteArrayInputStream(body);

                @Override
                public boolean isFinished() {
                    return stream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                }

                @Override
                public int read() {
                    return stream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }
    }
}

