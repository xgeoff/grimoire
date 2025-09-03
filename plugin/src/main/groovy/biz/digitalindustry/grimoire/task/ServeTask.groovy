package biz.digitalindustry.grimoire.task

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
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

    @InputDirectory
    abstract DirectoryProperty getWebRootDir()

    @Internal
    private HttpServer server

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

        // Define handler closure inside the method to correctly capture the web root directory.
        def handler = { HttpExchange exchange ->
            try {

                def requestPath = exchange.requestURI.path
                def effectivePath = (!requestPath || requestPath == '/') ? 'index.html' : requestPath.substring(1)
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

        def baseUrl = config.baseUrl ?: "/"
        def port = config.server?.port ?: 8080
        this.server = HttpServer.create(new InetSocketAddress(port), 0)
        server.createContext(baseUrl, handler as com.sun.net.httpserver.HttpHandler)
        server.executor = null
        server.start()

        logger.lifecycle("Grimoire server started on http://localhost:${port}")
        logger.lifecycle("Serving files from: ${webRootDir.absolutePath}")
        logger.lifecycle("Press Ctrl+C to stop the server.")

        // This will block until the thread is interrupted or server is stopped
        Thread.currentThread().join()
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