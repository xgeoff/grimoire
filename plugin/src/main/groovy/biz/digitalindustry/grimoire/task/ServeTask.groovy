package biz.digitalindustry.grimoire.task

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

import java.nio.file.Files

abstract class ServeTask extends DefaultTask {

    @InputDirectory
    abstract DirectoryProperty getWebRootDir()

    @InputFile
    abstract RegularFileProperty getConfigFile()

    @Input
    abstract Property<Integer> getPort()

    ServeTask() {
        port.convention(8080)
    }

    @TaskAction
    void serve() {
        // 1. The action now delegates to testable logic methods
        def config = parseConfig()
        def serverPort = resolvePort(config)
        def rootDir = webRootDir.get().asFile

        // 2. Side-effects (server creation, blocking) remain here
        def server = createAndConfigureServer(serverPort, rootDir)
        server.start()

        logger.lifecycle("Grimoire server started on http://localhost:${serverPort}")
        logger.lifecycle("Serving files from: ${rootDir.absolutePath}")
        logger.lifecycle("Press Ctrl+C to stop the server.")

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

    // --- Private helper methods for server implementation ---

    private HttpServer createAndConfigureServer(int serverPort, File rootDir) {
        def server = HttpServer.create(new InetSocketAddress(serverPort), 0)
        server.createContext("/", { exchange ->
            exchange.with {
                try {
                    def requestPath = requestURI.path
                    def effectivePath = (!requestPath || requestPath == '/') ? 'index.html' : requestPath.substring(1)
                    def requestedFile = new File(rootDir, effectivePath)

                    if (!requestedFile.canonicalPath.startsWith(rootDir.canonicalPath)) {
                        sendError(403, "Forbidden")
                        return
                    }

                    if (requestedFile.isFile()) {
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
        return server
    }

    private void sendError(int code, String message) {
        def response = message.bytes
        sendResponseHeaders(code, response.length)
        responseBody.withStream { it.write(response) }
    }
}