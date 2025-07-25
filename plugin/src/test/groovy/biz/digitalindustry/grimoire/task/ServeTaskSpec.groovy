package biz.digitalindustry.grimoire.task

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

import java.io.IOException
import java.net.ConnectException
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import biz.digitalindustry.grimoire.util.ResourceCopier

class ServeTaskSpec extends Specification {

    // Use a standard File object for a predictable, real directory
    private File testProjectDir

    // We need to hold references to stop them during cleanup
    private Thread serverThread
    private ServeTask task

    def setup() {
        // This setup logic is excellent and now correctly initializes the instance variable.
        def projectRoot = new File(".").getCanonicalFile()
        testProjectDir = new File(projectRoot, "grimoire-serve-test")

        // --- Robust Cleanup & Setup ---
        if (testProjectDir.exists()) {
            assert testProjectDir.deleteDir()
        }
        assert testProjectDir.mkdirs()
    }

    def cleanup() {
        // This cleanup logic is correct and remains unchanged.
        task?.stopServer()
        serverThread?.interrupt()
    }

    def cleanupSpec() {
        // FIX: Since testProjectDir is no longer a shared field, we recalculate
        // the path here to perform a final, safe cleanup after all tests are done.
        def projectRoot = new File(".").getCanonicalFile()
        def finalTestDir = new File(projectRoot, "grimoire-serve-test")
        if (finalTestDir.exists()) {
            finalTestDir.deleteDir()
        }
    }

    def "serves content and respects port from config.grim"() {
        given: "A project with a config file specifying a custom port"
        // Use the real directory created in setup()
        def project = ProjectBuilder.builder().withProjectDir(testProjectDir).build()
        def publicDir = new File(testProjectDir, "public")
        def configFile = new File(testProjectDir, "config.grim")
        int testPort = 9090

        configFile.text = """
baseUrl="/"
server { port = ${testPort} }
"""
        ResourceCopier.copy(Paths.get("src/test/resources/sample-sites/basic/public"), publicDir.toPath())

        and: "The ServeTask is configured"
        task = project.tasks.create("testServe", ServeTask)

        when: "The serve task is run in a background thread"
        serverThread = new Thread({ task.serve() })
        serverThread.start()

        waitForServer(testPort)

        and: "HTTP requests are made"
        def rootContent = new URL("http://localhost:${testPort}/").text
        def cssContent = new URL("http://localhost:${testPort}/assets/style.css").text

        then: "The content is served correctly"
        rootContent.contains("<h1>Welcome to Grimoire!</h1>")
        cssContent.contains("font-family: sans-serif;")
    }

    def "serves a 404 for a non-existent file"() {
        given: "A running server"
        def project = ProjectBuilder.builder().withProjectDir(testProjectDir).build()
        def publicDir = new File(testProjectDir, "public")
        def configFile = new File(testProjectDir, "config.grim")
        int testPort = 8080

        configFile.text = """
baseUrl="/"
server { port = ${testPort} }
"""

        ResourceCopier.copy(Paths.get("src/test/resources/sample-sites/basic/public"), publicDir.toPath())

        task = project.tasks.create("testServe", ServeTask)

        serverThread = new Thread({ task.serve() })
        serverThread.start()

        waitForServer(testPort)

        when: "A request is made to a file that does not exist"
        def connection = new URL("http://localhost:${testPort}/no-such-file.html").openConnection()

        then: "The server responds with a 404 Not Found status code"
        connection.responseCode == 404
    }

    /**
     * Polls the specified port until it is open or a timeout is reached.
     * This is a robust replacement for Thread.sleep() when testing servers.
     */
    private void waitForServer(int port, int timeoutMillis = 3000) {
        long startTime = System.currentTimeMillis()
        boolean connected = false
        Exception lastException = null

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                new Socket("localhost", port).withCloseable {
                    connected = true
                }
                break // Exit the loop on successful connection
            } catch (ConnectException e) {
                lastException = e
                // Port not open yet, wait a bit and retry.
                Thread.sleep(100)
            }
        }

        if (!connected) {
            throw new RuntimeException("Server on port $port did not start within ${timeoutMillis}ms.", lastException)
        }
    }
    /*
    private void copyPath(java.nio.file.Path sourceRoot, java.nio.file.Path targetRoot) {
        try {
            Files.walk(sourceRoot).forEach { sourcePath ->
                def targetPath = targetRoot.resolve(sourceRoot.relativize(sourcePath).toString())
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath)
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } catch (IOException e) {
            throw new GradleException("Failed to copy test resource: ${e.message}", e)
        }
    }*/
}