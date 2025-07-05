package biz.digitalindustry.grimoire.task

import com.sun.net.httpserver.HttpServer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

/**
 * A Gradle task that serves a static directory over HTTP.
 * This version is written in a more idiomatic Groovy style.
 */
abstract class ServeTask extends DefaultTask {

    @InputDirectory
    abstract DirectoryProperty getWebRootDir()

    @Input
    abstract Property<Integer> getPort()

    ServeTask() {
        port.convention(8080)
    }

    @TaskAction
    void serve() {
        def rootDir = webRootDir.get().asFile
        def serverPort = port.get()

        def server = HttpServer.create(new InetSocketAddress(serverPort), 0)
        server.createContext("/", { exchange ->
            // Use Groovy's 'with' to reduce repetition on the 'exchange' object
            exchange.with {
                try {
                    // Groovy Truth: an empty or null string is 'false'
                    def requestPath = requestURI.path
                    def effectivePath = (!requestPath || requestPath == '/') ? 'index.html' : requestPath.substring(1)

                    // Use Groovy's GDK for simpler File handling
                    def requestedFile = new File(rootDir, effectivePath)

                    // Security check using canonical paths to prevent directory traversal
                    if (!requestedFile.canonicalPath.startsWith(rootDir.canonicalPath)) {
                        sendError(403, "Forbidden")
                        return
                    }

                    // Groovy GDK's isFile() is cleaner than Files.exists() && !Files.isDirectory()
                    if (requestedFile.isFile()) {
                        // The .bytes property is a Groovy shortcut for reading all bytes from a file
                        def fileBytes = requestedFile.bytes
                        def contentType = Files.probeContentType(requestedFile.toPath()) ?: 'application/octet-stream'

                        responseHeaders.set("Content-Type", contentType)
                        sendResponseHeaders(200, fileBytes.length)
                        responseBody.withStream { it.write(fileBytes) }
                    } else {
                        sendError(404, "Not Found: ${requestPath}")
                    }
                } catch (Exception e) {
                    logger.error("Error handling request", e)
                    sendError(500, "Internal Server Error")
                } finally {
                    close()
                }
            }
        })

        server.executor = null
        server.start()

        logger.lifecycle("Grimoire server started on http://localhost:${serverPort}")
        logger.lifecycle("Serving files from: ${rootDir.absolutePath}")
        logger.lifecycle("Press Ctrl+C to stop the server.")

        // Block the task from finishing so the server can run
        Thread.currentThread().join()
    }

    /**
     * Helper to send an error response.
     * The 'exchange' object is the implicit 'delegate' from the parent 'with' block.
     */
    private void sendError(int code, String message) {
        def response = message.bytes
        sendResponseHeaders(code, response.length)
        responseBody.withStream { it.write(response) }
    }
}