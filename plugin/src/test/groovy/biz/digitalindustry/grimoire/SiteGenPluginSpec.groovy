import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification
import java.nio.file.Files
import org.gradle.testkit.runner.GradleRunner
import biz.digitalindustry.grimoire.util.ResourceCopier
import java.nio.file.Path
import java.nio.file.Paths

class SiteGenPluginSpec extends Specification {
    @Shared
    Path testDir
    Project project = ProjectBuilder.builder().build()

    def setup() {
        def fixture = Paths.get("src/test/resources/test-projects/basic-site")
        Path projectRoot = Paths.get(".").toAbsolutePath().normalize()
        //testDir = project.getProjectDir().toPath().resolve("grimoire-test-site")
        testDir = projectRoot.resolve("grimoire-test-site")
        def testDirFile = testDir.toFile()
        if (testDirFile.exists()) {
            assert testDirFile.deleteDir()
        }
        Files.createDirectories(testDir)
        //testDir = Files.createDirectory(project.getProjectDir().toPath(),"grimoire-test-site")
        ResourceCopier.copy(fixture, testDir)
        //copyProject(fixture, testDir)
    }

    def "plugin registers grim task"() {
        given:
        project != null

        when:
        project.plugins.apply("biz.digitalindustry.grimoire")

        then:
        project.tasks.findByName("grim") != null
    }

    def "plugin registers grim-init task"() {
        given:
        project != null

        when:
        project.plugins.apply("biz.digitalindustry.grimoire")

        then:
        project.tasks.findByName("grim-init") != null
    }

    def "renders site from fixture project"() {
        when: "The grim-generate task is run"
        def result = GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "The output file contains the expected content in the new location"
        // --- THIS IS THE FIX ---
        // The output directory now defaults to 'public' at the project root.
        def outputFile = new File(testDir.toFile(), "public/index.html")

        // Check that the output file was actually created
        assert outputFile.exists()

        // Check the content
        assert outputFile.text.contains("Test-Bot")
    }

    def "helper quoted arguments render without HTML entities"() {
        when: "grim-gen processes markdown with helpers"
        GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "Rendered page contains helper output without &quot; entities"
        def outputFile = new File(testDir.toFile(), "public/quoted-helper.html")
        outputFile.exists()
        outputFile.text.contains("<div>")
        !outputFile.text.contains("&quot;")
    }

    def "skips page generation when destination is directory"() {
        given: "An existing directory where a page would be generated"
        def conflictDir = new File(testDir.toFile(), "public/index.html")
        conflictDir.mkdirs()

        when: "grim-gen runs"
        GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "The directory remains and no file is written"
        conflictDir.isDirectory()
        !new File(testDir.toFile(), "public/index.html").isFile()
    }

    def "markdown tables render to HTML tables"() {
        when: "grim-gen processes a markdown file with a pipe table"
        def result = GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "The generated HTML contains a <table> element"
        def outputFile = new File(testDir.toFile(), "public/markdown-table.html")
        outputFile.exists()
        def html = outputFile.text
        html.contains("<table") && (html.contains("<td") || html.contains("<th"))
    }

    def cleanupSpec() {
        File testProjectDir = testDir.toFile()
        println "Cleaning up test directory: ${testProjectDir.absolutePath}"
        if (testProjectDir != null && testProjectDir.exists()) {
            testProjectDir.deleteDir()
        }
    }
}
