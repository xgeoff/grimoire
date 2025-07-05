package biz.digitalindustry.grimoire

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Unit tests for the SiteGenExtension.
 * Verifies default values and user configuration.
 */
class SiteGenExtensionSpec extends Specification {

    // Use a lateinit var for the project so it's fresh for each test
    private Project project

    // The setup() method is run before each test feature
    def setup() {
        // ProjectBuilder creates a temporary, lightweight Project instance for testing
        project = ProjectBuilder.builder().build()
    }

    def "should default configFile to 'config.grim' in the project root"() {
        given: "A dummy config file exists at the default location"
        def expectedDefaultFile = new File(project.projectDir, "config.grim")
        expectedDefaultFile.text = "site.title = 'Default Test Site'"

        when: "The extension is created on the project"
        // We create the extension directly, which is what `project.extensions.create` does.
        // This simulates the plugin being applied.
        def extension = project.extensions.create("grimoire", SiteGenExtension)

        then: "The configFile property points to the correct default file"
        extension instanceof SiteGenExtension
        extension.configFile.isPresent()
        extension.configFile.get().asFile == expectedDefaultFile
        extension.configFile.get().asFile.name == "config.grim"
    }

    def "should allow a user to override the default configFile location"() {
        given: "A custom config file exists in a different location"
        def customConfigDir = new File(project.projectDir, "config")
        customConfigDir.mkdir()
        def customConfigFile = new File(customConfigDir, "my-site.conf")
        customConfigFile.text = "server.port = 9090"

        and: "The extension is created on the project"
        project.extensions.create("grimoire", SiteGenExtension)

        when: "The user configures the extension in their build script"
        // This closure simulates how a user would configure the extension in build.gradle
        project.grimoire {
            configFile = project.file("config/my-site.conf")
        }
        def extension = project.extensions.findByName("grimoire")

        then: "The configFile property points to the user-provided file"
        extension.configFile.get().asFile == customConfigFile
        extension.configFile.get().asFile.name == "my-site.conf"
    }
}