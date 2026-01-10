/*
 * Copyright 2012 - 2025 Encipher Company Limited
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

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.ee10.proxy.AsyncProxyServlet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ResourceServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.session.DatabaseAdaptor;
import org.eclipse.jetty.session.DefaultSessionCache;
import org.eclipse.jetty.session.JDBCSessionDataStoreFactory;
import org.eclipse.jetty.session.SessionCache;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.esync.LifecycleObject;
import tz.co.esync.api.CorsResponseFilter;
import tz.co.esync.api.DateParameterConverterProvider;
import tz.co.esync.api.ResourceErrorHandler;
import tz.co.esync.api.StreamWriter;
import tz.co.esync.api.resource.ServerResource;
import tz.co.esync.api.security.LoginService;
import tz.co.esync.api.security.SecurityRequestFilter;
import tz.co.esync.api.security.TenantContextFilter;
import tz.co.esync.config.Config;
import tz.co.esync.config.Keys;
import tz.co.esync.helper.ObjectMapperContextResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

public class WebServer implements LifecycleObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private final Injector injector;
    private final Config config;

    private final Server server;
    private McpServerHolder mcpServerHolder;

    public WebServer(Injector injector, Config config) throws IOException {
        this.injector = injector;
        this.config = config;
        String address = config.getString(Keys.WEB_ADDRESS);
        int port = config.getInteger(Keys.WEB_PORT);
        if (address == null) {
            server = new Server(port);
        } else {
            server = new Server(new InetSocketAddress(address, port));
        }

        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        JettyWebSocketServletContainerInitializer.configure(servletHandler, null);
        servletHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

        // Register API servlets FIRST to ensure they match before the default ResourceServlet
        // This ensures /v1/* and /api/* paths are handled by their respective servlets
        initApi(servletHandler);
        initSessionConfig(servletHandler);

        if (config.getBoolean(Keys.WEB_CONSOLE)) {
            servletHandler.addServlet(new ServletHolder(new ConsoleServlet(config)), "/console/*");
        }

        // Register web app (ResourceServlet) AFTER API servlets
        // More specific paths registered above will match before the default servlet at /
        initWebApp(servletHandler);

        Handler.Sequence handlers = new Handler.Sequence();
        initClientProxy(servletHandler);
        handlers.addHandler(new CompressionHandler());
        handlers.addHandler(servletHandler);
        server.setHandler(handlers);

        if (config.hasKey(Keys.WEB_REQUEST_LOG_PATH)) {
            RequestLogWriter logWriter = new RequestLogWriter(config.getString(Keys.WEB_REQUEST_LOG_PATH));
            logWriter.setAppend(true);
            logWriter.setRetainDays(config.getInteger(Keys.WEB_REQUEST_LOG_RETAIN_DAYS));
            server.setRequestLog(new WebRequestLog(logWriter));
        }
    }

    private void initClientProxy(ServletContextHandler servletHandler) {
        int port = config.getInteger(Keys.PROTOCOL_PORT.withPrefix("osmand"));
        if (port > 0) {
            ServletHolder proxy = new ServletHolder(AsyncProxyServlet.Transparent.class);
            proxy.setInitParameter("proxyTo", "http://localhost:" + port);
            servletHandler.addServlet(proxy, "/client-proxy/*");
            servletHandler.addFilter((request, response, chain) -> {
                HttpServletRequest r = (HttpServletRequest) request;
                if ("POST".equals(r.getMethod()) && "/".equals(r.getRequestURI())) {
                    request.getRequestDispatcher("/client-proxy/").forward(request, response);
                    return;
                }
                chain.doFilter(request, response);
            }, "/*", EnumSet.allOf(DispatcherType.class));
        }
    }

    private void initWebApp(ServletContextHandler servletHandler) throws IOException {
        String cache = config.getString(Keys.WEB_CACHE_CONTROL);

        Path basePath = Paths.get(config.getString(Keys.WEB_PATH));
        Files.createDirectories(basePath);
        Path baseReal = basePath.toRealPath(LinkOption.NOFOLLOW_LINKS);
        servletHandler.setBaseResource(ResourceFactory.of(servletHandler).newResource(baseReal));

        // ResourceServlet for static web files only
        // CRITICAL: This servlet is registered AFTER API servlets in initApi() to ensure
        // path-specific servlets (/v1/*, /api/*) match before this default servlet (/)
        // In Jetty, servlet matching by path specificity happens automatically, but
        // registration order matters when paths have equal specificity
        ServletHolder baseHolder = new ServletHolder(ResourceServlet.class);
        baseHolder.setInitParameter("dirAllowed", "false");
        baseHolder.setInitParameter("cacheControl", cache);
        baseHolder.setInitParameter("welcomeServlets", "false");
        baseHolder.setInitParameter("etags", "true");
        baseHolder.setInitParameter("redirectWelcome", "false");
        baseHolder.setInitParameter("precompressed", "false");
        // Configure ResourceServlet to only serve files that exist
        // For non-existent files, it will return 404, allowing other servlets to handle API paths
        servletHandler.addServlet(baseHolder, "/");

        Path override = Paths.get(config.getString(Keys.WEB_OVERRIDE));
        Files.createDirectories(override);
        Path overrideReal = override.toRealPath(LinkOption.NOFOLLOW_LINKS);

        ServletHolder overrideHolder = new ServletHolder(ResourceServlet.class);
        overrideHolder.setInitParameter("baseResource", overrideReal.toString());
        overrideHolder.setInitParameter("pathInfoOnly", "true");
        overrideHolder.setInitParameter("dirAllowed", "false");
        overrideHolder.setInitParameter("cacheControl", cache);
        servletHandler.addServlet(overrideHolder, "/override/*");

        FilterHolder filterHolder = new FilterHolder(new OverrideFileFilter());
        filterHolder.setInitParameter("overridePath", overrideReal.toString());
        servletHandler.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

        if (config.getBoolean(Keys.WEB_DEBUG)) {
            servletHandler.setWelcomeFiles(new String[] {"debug.html", "index.html"});
        } else {
            servletHandler.setWelcomeFiles(new String[] {"release.html", "index.html"});
        }
    }

    private void initApi(ServletContextHandler servletHandler) {
        String mediaPath = config.getString(Keys.MEDIA_PATH);
        if (mediaPath != null) {
            ServletHolder servletHolder = new ServletHolder(ResourceServlet.class);
            servletHolder.setInitParameter("baseResource", Path.of(mediaPath).toUri().toString());
            servletHolder.setInitParameter("dirAllowed", "false");
            servletHolder.setInitParameter("pathInfoOnly", "true");
            servletHandler.addServlet(servletHolder, "/api/media/*");
        }

        if (config.getBoolean(Keys.WEB_MCP_ENABLE)) {
            mcpServerHolder = injector.getInstance(McpServerHolder.class);
            var mcpServletHolder = new ServletHolder(mcpServerHolder.getServlet());
            mcpServletHolder.setAsyncSupported(true);
            servletHandler.addServlet(mcpServletHolder, McpServerHolder.PATH);
            servletHandler.addFilter(
                    new FilterHolder(new McpAuthFilter(injector.getInstance(LoginService.class))),
                    McpServerHolder.PATH + "/*", EnumSet.of(DispatcherType.REQUEST));
        }

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property("jersey.config.server.wadl.disableWadl", true);
        resourceConfig.registerClasses(
                JacksonFeature.class,
                ObjectMapperContextResolver.class,
                DateParameterConverterProvider.class,
                SecurityRequestFilter.class,
                TenantContextFilter.class,
                CorsResponseFilter.class,
                ResourceErrorHandler.class,
                StreamWriter.class);
        resourceConfig.packages(ServerResource.class.getPackage().getName());
        if (resourceConfig.getClasses().stream().filter(ServerResource.class::equals).findAny().isEmpty()) {
            LOGGER.warn("Failed to load API resources");
        }
        servletHandler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/api/*");

        // Add Swagger UI - must be registered BEFORE v1/* to take precedence
        ServletHolder swaggerHolder = new ServletHolder(new SwaggerUiServlet());
        swaggerHolder.setName("swagger-ui");
        servletHandler.addServlet(swaggerHolder, "/v1/docs");
        servletHandler.addServlet(new ServletHolder(new SwaggerUiServlet()), "/v1/docs/*");

        // Register v1 API endpoints under /v1/* (without /api prefix)
        ResourceConfig v1ResourceConfig = new ResourceConfig();
        v1ResourceConfig.property("jersey.config.server.wadl.disableWadl", true);
        v1ResourceConfig.registerClasses(
                JacksonFeature.class,
                ObjectMapperContextResolver.class,
                DateParameterConverterProvider.class,
                SecurityRequestFilter.class,
                TenantContextFilter.class,
                CorsResponseFilter.class,
                ResourceErrorHandler.class,
                StreamWriter.class);
        // Register all resources using package scanning (same as /api/*)
        v1ResourceConfig.packages(ServerResource.class.getPackage().getName());
        
        // Verify resources are registered
        if (v1ResourceConfig.getClasses().isEmpty()) {
            LOGGER.warn("No v1 resources registered");
        } else {
            long resourceCount = v1ResourceConfig.getClasses().stream()
                    .filter(c -> c.getPackageName().contains("resource"))
                    .count();
            LOGGER.info("Registered {} v1 resources via package scanning", resourceCount);
        }
        
        // Register v1 servlet container - must be registered BEFORE default servlet (/)
        // to ensure /v1/* paths match before the ResourceServlet at /
        ServletHolder v1ServletHolder = new ServletHolder(new ServletContainer(v1ResourceConfig));
        v1ServletHolder.setName("v1-api");
        servletHandler.addServlet(v1ServletHolder, "/v1/*");
        LOGGER.info("Registered v1 API servlet at /v1/*");
    }

    private void initSessionConfig(ServletContextHandler servletHandler) {
        SessionHandler sessionHandler = servletHandler.getSessionHandler();

        if (config.getBoolean(Keys.WEB_PERSIST_SESSION)) {
            DatabaseAdaptor databaseAdaptor = new DatabaseAdaptor();
            databaseAdaptor.setDatasource(injector.getInstance(DataSource.class));
            JDBCSessionDataStoreFactory jdbcSessionDataStoreFactory = new JDBCSessionDataStoreFactory();
            jdbcSessionDataStoreFactory.setDatabaseAdaptor(databaseAdaptor);
            SessionCache sessionCache = new DefaultSessionCache(sessionHandler);
            sessionCache.setSessionDataStore(jdbcSessionDataStoreFactory.getSessionDataStore(sessionHandler));
            sessionHandler.setSessionCache(sessionCache);
        }

        SessionCookieConfig sessionCookieConfig = servletHandler.getServletContext().getSessionCookieConfig();

        int sessionTimeout = config.getInteger(Keys.WEB_SESSION_TIMEOUT);
        if (sessionTimeout > 0) {
            servletHandler.getSessionHandler().setMaxInactiveInterval(sessionTimeout);
            sessionCookieConfig.setMaxAge(sessionTimeout);
        }

        String sameSiteCookie = config.getString(Keys.WEB_SAME_SITE_COOKIE);
        if (sameSiteCookie != null) {
            switch (sameSiteCookie.toLowerCase()) {
                case "lax":
                    sessionHandler.setSameSite(HttpCookie.SameSite.LAX);
                    break;
                case "strict":
                    sessionHandler.setSameSite(HttpCookie.SameSite.STRICT);
                    break;
                case "none":
                    sessionCookieConfig.setSecure(true);
                    sessionHandler.setSameSite(HttpCookie.SameSite.NONE);
                    break;
                default:
                    break;
            }
        }

        sessionCookieConfig.setHttpOnly(true);
    }

    @Override
    public void start() throws Exception {
        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
        if (mcpServerHolder != null) {
            mcpServerHolder.close();
        }
    }

}
