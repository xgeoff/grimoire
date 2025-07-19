
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification

class ScaffoldSpec extends Specification {
    @Shared
    File testProjectDir

    def setup() {
        // Use a predictable directory for the Gradle project that will run the task
        def projectRoot = new File(".").getCanonicalFile()
        testProjectDir = new File(projectRoot, "grimoire-scaffold-test")

        // Clean up from previous runs
        if (testProjectDir.exists()) {
            assert testProjectDir.deleteDir()
        }
        assert testProjectDir.mkdirs()

        // Create the build script in the test project directory
        new File(testProjectDir, 'build.gradle') << """
            plugins {
                id 'biz.digitalindustry.grimoire'
            }
        """
        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name = 'grimoire-scaffold-test'
        """
    }

    def "scaffolds site with default type into a specified directory"() {
        given: "A target directory name"
        def targetDirName = "my-basic-site"

        when: "The grim-scaffold task is run with a destination"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("grim-init", "--dest=${targetDirName}", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "The basic scaffold structure is created in the target subdirectory"
        def scaffoldRoot = new File(testProjectDir, targetDirName)
        assert scaffoldRoot.isDirectory()

        def configFile = new File(testProjectDir, "config.grim")
        assert configFile.exists()
        assert new File(scaffoldRoot, "pages/index.html").exists()
        assert new File(scaffoldRoot, "layouts/default.hbs").exists()
        assert new File(scaffoldRoot, "assets/style.css").exists()

        and: "The config file has the default source directory"
        assert configFile.text.contains("sourceDir = \"${targetDirName}")
    }


    def "fails when an invalid scaffold type is provided"() {
        when: "The grim-scaffold task is run with a non-existent type"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
        // We must provide a --dest so the task doesn't fail on the "not empty" check first
                .withArguments("grim-init", "--dest=test-dest", "--type=non-existent", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail()

        then: "The build fails with a helpful error message about the type"
        result.output.contains("Could not find the scaffold template for type 'non-existent'")
    }

    // This method will run ONCE, after the last test method in this class has finished.
    def cleanupSpec() {
        println "Cleaning up test directory: ${testProjectDir.absolutePath}"
        if (testProjectDir != null && testProjectDir.exists()) {
            testProjectDir.deleteDir()
        }
    }
}