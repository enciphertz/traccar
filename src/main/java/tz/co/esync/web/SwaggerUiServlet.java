/*
 * Copyright 2025 Encipher Company Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tz.co.esync.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

public class SwaggerUiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html; charset=UTF-8");
        
        String baseUrl = getBaseUrl(req);
        // Get request URI and remove /v1/docs if present
        String requestUri = req.getRequestURI();
        if (requestUri != null && requestUri.contains("/v1/docs")) {
            // If accessed via /v1/docs/*, we need the base URL without that path
            baseUrl = baseUrl.replace("/v1/docs", "").replaceAll("/+$", "");
        }
        String openApiUrl = baseUrl + "/v1/openapi/yaml";
        
        try (PrintWriter writer = resp.getWriter()) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"en\">");
            writer.println("<head>");
            writer.println("  <meta charset=\"UTF-8\">");
            writer.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.println("  <title>E-Sync Telemetries Engine API Documentation</title>");
            writer.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui.css\" />");
            writer.println("  <style>");
            writer.println("    html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }");
            writer.println("    *, *:before, *:after { box-sizing: inherit; }");
            writer.println("    body { margin:0; background: #fafafa; }");
            writer.println("  </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("  <div id=\"swagger-ui\"></div>");
            writer.println("  <script src=\"https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-bundle.js\"></script>");
            writer.println("  <script src=\"https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-standalone-preset.js\"></script>");
            writer.println("  <script>");
            writer.println("    window.onload = function() {");
            writer.println("      const ui = SwaggerUIBundle({");
            writer.println("        url: \"" + openApiUrl + "\",");
            writer.println("        dom_id: '#swagger-ui',");
            writer.println("        deepLinking: true,");
            writer.println("        presets: [");
            writer.println("          SwaggerUIBundle.presets.apis,");
            writer.println("          SwaggerUIStandalonePreset");
            writer.println("        ],");
            writer.println("        plugins: [");
            writer.println("          SwaggerUIBundle.plugins.DownloadUrl");
            writer.println("        ],");
            writer.println("        layout: \"StandaloneLayout\",");
            writer.println("        tryItOutEnabled: true");
            writer.println("      });");
            writer.println("    };");
            writer.println("  </script>");
            writer.println("</body>");
            writer.println("</html>");
        }
    }

    private String getBaseUrl(HttpServletRequest req) {
        String scheme = req.getScheme();
        String serverName = req.getServerName();
        int serverPort = req.getServerPort();
        String contextPath = req.getContextPath();
        
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        
        url.append(contextPath);
        
        return url.toString();
    }
}
