package biz.digitalindustry.grimoire.task

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Unit tests for the ServeTask.
 * Focuses on testing the configuration logic without starting a real server.
 */
class ServeTaskSpec extends Specification {

    @TempDir
    File testProjectDir // Spock will create and clean up this temporary directory

    private Project project
    private ServeTask task

    def setup() {
        // Create a fresh project and task for each test
        project = ProjectBuilder.builder().withProjectDir(testProjectDir).build()
        // Register the task type so we can create an instance
        task = project.tasks.create("grim-serve", ServeTask)
    }

    def "should resolve port from config file when specified"() {
        given: "A config file that specifies a server port"
        def configFile = new File(testProjectDir, "config.grim")
        configFile.text = """
            server {
                port = 9999
            }
        """
        task.configFile.set(configFile)

        when: "The configuration is parsed and the port is resolved"
        def config = task.parseConfig()
        def resolvedPort = task.resolvePort(config)

        then: "The port from the config file is used"
        resolvedPort == 9999
    }

    def "should fall back to default port when not specified in config"() {
        given: "A config file that does NOT specify a server port"
        def configFile = new File(testProjectDir, "config.grim")
        configFile.text = """
            site {
                title = "My Test Site"
            }
        """
        task.configFile.set(configFile)

        when: "The configuration is parsed and the port is resolved"
        def config = task.parseConfig()
        def resolvedPort = task.resolvePort(config)

        then: "The task's default port (8080) is used"
        resolvedPort == 8080
    }

    def "should fall back to default port for an empty config file"() {
        given: "An empty config file"
        def configFile = new File(testProjectDir, "config.grim")
        configFile.text = "" // Empty
        task.configFile.set(configFile)

        when: "The configuration is parsed and the port is resolved"
        def config = task.parseConfig()
        def resolvedPort = task.resolvePort(config)

        then: "The task's default port (8080) is used"
        resolvedPort == 8080
    }

    def "should fall back to default port when config file does not exist"() {
        given: "A path to a config file that does not exist"
        def configFile = new File(testProjectDir, "non-existent-config.grim")
        // We ensure it doesn't exist
        assert !configFile.exists()
        task.configFile.set(configFile)

        when: "The configuration is parsed and the port is resolved"
        def config = task.parseConfig()
        def resolvedPort = task.resolvePort(config)

        then: "The task's default port (8080) is used"
        resolvedPort == 8080
    }
}