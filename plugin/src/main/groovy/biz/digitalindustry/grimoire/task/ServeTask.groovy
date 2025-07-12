package biz.digitalindustry.grimoire.task

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import groovy.util.ConfigObject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

import java.net.InetSocketAddress
import java.nio.file.Files

abstract class ServeTask extends DefaultTask {

    @InputDirectory
    abstract DirectoryProperty getWebRootDir()

    @InputFile
    abstract RegularFileProperty getConfigFile()

    @Input
    abstract Property<Integer> getPort()

    private HttpServer server

    ServeTask() {
        port.convention(8080)
    }

    @TaskAction
    void serve() {
        def config = parseConfig()
        def serverPort = resolvePort(config)
        def rootDir = webRootDir.get().asFile

        this.server = createAndConfigureServer(serverPort, rootDir)
        server.start()

        logger.lifecycle("Grimoire server started on http://localhost:${serverPort}")
        logger.lifecycle("Serving files from: ${rootDir.absolutePath}")
        logger.lifecycle("Press Ctrl+C to stop the server.")

        // This will block until the thread is interrupted or server is stopped
        Thread.currentThread().join()
    }

    /**
     * Parses the configuration file. Visible for testing.
     * @return A ConfigObject representing the parsed file.
     */
    ConfigObject parseConfig() {
        def configFile = this.configFile.get().asFile
        if (!configFile.exists() || configFile.length() == 0) {
            return new ConfigObject() // Return empty config for non-existent/empty files
        }
        return new ConfigSlurper().parse(configFile.toURI().toURL())
    }

    /**
     * Resolves the port to use, prioritizing the config file over the default. Visible for testing.
     * @param config The parsed ConfigObject.
     * @return The resolved port number.
     */
    int resolvePort(ConfigObject config) {
        // Use the port from config.grim, or the task's default if not specified
        return config.server?.port ?: port.get()
    }

    private HttpServer createAndConfigureServer(int serverPort, File rootDir) {
        // --- FINAL FIX: Define sendError as a local closure to guarantee scope ---
        def sendError = { HttpExchange exchange, int code, String message ->
            // We can access the logger because it's in the outer scope of the method.
            logger.warn("Sending error response: {} {}", code, message)
            def response = message.bytes
            exchange.sendResponseHeaders(code, response.length)
            exchange.responseBody.withStream { it.write(response) }
        }

        def handler = { HttpExchange exchange ->
            try {
                def requestPath = exchange.requestURI.path
                def effectivePath = (!requestPath || requestPath == '/') ? 'index.html' : requestPath.substring(1)
                def requestedFile = new File(rootDir, effectivePath)

                if (!requestedFile.canonicalPath.startsWith(rootDir.canonicalPath)) {
                    sendError(exchange, 403, "Forbidden") // Call the local closure
                    return
                }

                if (requestedFile.isFile()) {
                    def fileBytes = requestedFile.bytes
                    def contentType = Files.probeContentType(requestedFile.toPath()) ?: 'application/octet-stream'
                    exchange.responseHeaders.set("Content-Type", contentType)
                    exchange.sendResponseHeaders(200, fileBytes.length)
                    exchange.responseBody.withStream { it.write(fileBytes) }
                } else {
                    sendError(exchange, 404, "Not Found: ${requestPath}") // Call the local closure
                }
            } catch (Exception e) {
                logger.error("Error handling request", e)
                sendError(exchange, 500, "Internal Server Error") // Call the local closure
            } finally {
                exchange.close()
            }
        }

        def server = HttpServer.create(new InetSocketAddress(serverPort), 0)
        server.createContext("/", handler as com.sun.net.httpserver.HttpHandler)
        server.executor = null
        return server
    }

    private void sendError(HttpExchange exchange, int code, String message) {
        def response = message.bytes
        exchange.sendResponseHeaders(code, response.length)
        exchange.responseBody.withStream { it.write(response) }
    }

    void stopServer() {
        if (server != null) {
            // FIX: Change the delay from 0 to 1.
            // This gives the server up to 1 second to finish any active
            // requests gracefully, which prevents the race condition.
            server.stop(1)
            logger.info("Server stopped by test.")
        }
    }
}