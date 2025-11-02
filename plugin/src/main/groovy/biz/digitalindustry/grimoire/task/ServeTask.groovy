package biz.digitalindustry.grimoire.task

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

import java.net.InetSocketAddress
import java.nio.file.Files

abstract class ServeTask extends DefaultTask {

    @Inject
    abstract ProjectLayout getLayout()

    @InputFile
    abstract RegularFileProperty getConfigFile()

    @Internal
    abstract DirectoryProperty getWebRootDir()

    private HttpServer server

    @Internal
    HttpServer getServer() { server }

    ServeTask() {
        getConfigFile().convention(getLayout().projectDirectory.file("config.grim"))
        getWebRootDir().convention(getLayout().projectDirectory.dir("public"))
    }

    @TaskAction
    void serve() {
        def config = new ConfigSlurper().parse(getConfigFile().get().asFile.toURI().toURL())
        // This must be final so the closure can safely capture it.
        File webRootDir = config.outputDir ? getLayout().projectDirectory.dir(config.outputDir).asFile : getWebRootDir().get().asFile

        if (!webRootDir.exists() || !webRootDir.isDirectory())
            throw new GradleException("Web root directory to serve not found or not a directory: ${webRootDir.absolutePath}. Please run 'grim-gen' first.")

        def sendError = { HttpExchange exchange, int code, String message ->
            def response = message.bytes
            exchange.sendResponseHeaders(code, response.length)
            exchange.responseBody.withStream { it.write(response) }
        }

        // Normalize base path for the HTTP context ("/" or "/subpath")
        String basePath = normalizeBasePathForServer(config.baseUrl as String)

        // Define handler closure inside the method to correctly capture the web root directory
        // and the normalized base path.
        def handler = { HttpExchange exchange ->
            try {
                def requestPath = exchange.requestURI.path ?: "/"

                // If the request exactly matches the base path (e.g., "/arden"),
                // redirect to the trailing-slash form ("/arden/") so that relative
                // URLs in the HTML resolve under the subpath instead of root.
                if (basePath != "/" && requestPath == basePath) {
                    def redirectTo = basePath + "/"
                    exchange.responseHeaders.set("Location", redirectTo)
                    exchange.sendResponseHeaders(301, -1)
                    exchange.close()
                    return
                }

                // Strip the configured base path so we can map to files under webRootDir
                String localPath = requestPath
                if (basePath != "/" && requestPath.startsWith(basePath)) {
                    localPath = requestPath.substring(basePath.length())
                    if (localPath.isEmpty()) localPath = "/"
                }

                def effectivePath = (localPath == "/") ? 'index.html' : (localPath.startsWith('/') ? localPath.substring(1) : localPath)
                def requestedFile = new File(webRootDir, effectivePath)

                if (!requestedFile.canonicalPath.startsWith(webRootDir.canonicalPath)) {
                    sendError(exchange, 403, "Forbidden")
                    return
                }

                if (requestedFile.isFile()) {
                    def fileBytes = requestedFile.bytes
                    def contentType = Files.probeContentType(requestedFile.toPath()) ?: 'application/octet-stream'
                    exchange.responseHeaders.set("Content-Type", contentType)
                    exchange.sendResponseHeaders(200, fileBytes.length)
                    exchange.responseBody.withStream { it.write(fileBytes) }
                } else {
                    sendError(exchange, 404, "Not Found: ${requestPath}")
                }
            } catch (Exception e) {
                logger.error("Error handling request", e)
                sendError(exchange, 500, "Internal Server Error")
            } finally {
                exchange.close()
            }
        }

        def baseUrl = basePath
        def port = config.server?.port ?: 8080
        this.server = HttpServer.create(new InetSocketAddress(port), 0)
        server.createContext(baseUrl, handler as com.sun.net.httpserver.HttpHandler)
        server.executor = null
        server.start()

        def displayUrl = "http://localhost:${port}" + (baseUrl == "/" ? "" : baseUrl)
        logger.lifecycle("Grimoire server started on ${displayUrl}")
        logger.lifecycle("Serving files from: ${webRootDir.absolutePath}")
        logger.lifecycle("Press Ctrl+C to stop the server.")

        // Block until interrupted (e.g., Ctrl+C) and then stop the server.
        // Using sleep in a loop allows Gradle to interrupt the task thread
        // on cancellation, ensuring we can cleanly close the socket.
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000)
            }
        } catch (InterruptedException ignored) {
            // Gradle cancels tasks by interrupting their threads
            Thread.currentThread().interrupt()
        } finally {
            stopServer()
        }
    }

    private static String normalizeBasePathForServer(String raw) {
        if (!raw) return "/"
        String base = raw.trim()
        if (!base.startsWith("/")) base = "/" + base
        // Remove trailing slash except for root
        if (base.length() > 1 && base.endsWith("/")) base = base.substring(0, base.length() - 1)
        return base
    }

    void stopServer() {
        if (server != null) {
            // FIX: Change the delay from 0 to 1.
            // This gives the server up to 1 second to finish any active
            // requests gracefully, which prevents the race condition.
            server.stop(1)
            server = null
            logger.info("Server stopped.")
        }
    }
}
