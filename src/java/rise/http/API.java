package rise.http;

import rise.Constants;
import rise.Rise;
import rise.util.Logger;
import rise.util.ThreadPool;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class API {

    private static final int TESTNET_API_PORT = 6876;

    static final Set<String> allowedBotHosts;
    static final boolean enableDebugAPI = Rise.getBooleanProperty("rise.enableDebugAPI");

    private static final Server apiServer;

    static {
        List<String> allowedBotHostsList = Rise.getStringListProperty("rise.allowedBotHosts");
        if (! allowedBotHostsList.contains("*")) {
            allowedBotHosts = Collections.unmodifiableSet(new HashSet<>(allowedBotHostsList));
        } else {
            allowedBotHosts = null;
        }

        boolean enableAPIServer = Rise.getBooleanProperty("rise.enableAPIServer");
        if (enableAPIServer) {
            final int port = Constants.isTestnet ? TESTNET_API_PORT : Rise.getIntProperty("rise.apiServerPort");
            final String host = Rise.getStringProperty("rise.apiServerHost");
            apiServer = new Server();
            ServerConnector connector;

            boolean enableSSL = Rise.getBooleanProperty("rise.apiSSL");
            if (enableSSL) {
                Logger.logMessage("Using SSL (https) for the API server");
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.setSecureScheme("https");
                https_config.setSecurePort(port);
                https_config.addCustomizer(new SecureRequestCustomizer());
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(Rise.getStringProperty("rise.keyStorePath"));
                sslContextFactory.setKeyStorePassword(Rise.getStringProperty("rise.keyStorePassword"));
                sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                        "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
                sslContextFactory.setExcludeProtocols("SSLv3");
                connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                        new HttpConnectionFactory(https_config));
            } else {
                connector = new ServerConnector(apiServer);
            }

            connector.setPort(port);
            connector.setHost(host);
            connector.setIdleTimeout(Rise.getIntProperty("rise.apiServerIdleTimeout"));
            connector.setReuseAddress(true);
            apiServer.addConnector(connector);

            HandlerList apiHandlers = new HandlerList();

            ServletContextHandler apiHandler = new ServletContextHandler();
            String apiResourceBase = Rise.getStringProperty("rise.apiResourceBase");
            if (apiResourceBase != null) {
                ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
                defaultServletHolder.setInitParameter("dirAllowed", "false");
                defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
                defaultServletHolder.setInitParameter("welcomeServlets", "true");
                defaultServletHolder.setInitParameter("redirectWelcome", "true");
                defaultServletHolder.setInitParameter("gzip", "true");
                apiHandler.addServlet(defaultServletHolder, "/*");
                apiHandler.setWelcomeFiles(new String[]{"index.html"});
            }

            String javadocResourceBase = Rise.getStringProperty("rise.javadocResourceBase");
            if (javadocResourceBase != null) {
                ContextHandler contextHandler = new ContextHandler("/doc");
                ResourceHandler docFileHandler = new ResourceHandler();
                docFileHandler.setDirectoriesListed(false);
                docFileHandler.setWelcomeFiles(new String[]{"index.html"});
                docFileHandler.setResourceBase(javadocResourceBase);
                contextHandler.setHandler(docFileHandler);
                apiHandlers.addHandler(contextHandler);
            }

            apiHandler.addServlet(APIServlet.class, "/rise");
            if (Rise.getBooleanProperty("rise.enableAPIServerGZIPFilter")) {
                FilterHolder gzipFilterHolder = apiHandler.addFilter(GzipFilter.class, "/rise", null);
                gzipFilterHolder.setInitParameter("methods", "GET,POST");
                gzipFilterHolder.setAsyncSupported(true);
            }
            apiHandler.addServlet(APIServlet.class, "/burst");
            if (Rise.getBooleanProperty("rise.enableAPIServerGZIPFilter")) {
                FilterHolder gzipFilterHolder = apiHandler.addFilter(GzipFilter.class, "/burst", null);
                gzipFilterHolder.setInitParameter("methods", "GET,POST");
                gzipFilterHolder.setAsyncSupported(true);
            }

            apiHandler.addServlet(APITestServlet.class, "/test");
            if (enableDebugAPI) {
                apiHandler.addServlet(DbShellServlet.class, "/dbshell");
            }

            if (Rise.getBooleanProperty("rise.apiServerCORS")) {
                FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
                filterHolder.setInitParameter("allowedHeaders", "*");
                filterHolder.setAsyncSupported(true);
            }

            apiHandlers.addHandler(apiHandler);
            apiHandlers.addHandler(new DefaultHandler());

            apiServer.setHandler(apiHandlers);
            apiServer.setStopAtShutdown(true);

            ThreadPool.runBeforeStart(new Runnable() {
                @Override
                public void run() {
                    try {
                        apiServer.start();
                        Logger.logMessage("Started API server at " + host + ":" + port);
                    } catch (Exception e) {
                        Logger.logErrorMessage("Failed to start API server", e);
                        throw new RuntimeException(e.toString(), e);
                    }

                }
            }, true);

        } else {
            apiServer = null;
            Logger.logMessage("API server not enabled");
        }

    }

    public static void init() {}

    public static void shutdown() {
        if (apiServer != null) {
            try {
                apiServer.stop();
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to stop API server", e);
            }
        }
    }

    private API() {} // never

}
