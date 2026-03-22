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
        def html = outputFile.text
        assert html.contains("Test-Bot")
        assert html.contains("/test/markdown-support.html")
    }

    def "groovy templates render inline scripts"() {
        when: "grim-gen processes pages with Groovy templating"
        GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "Rendered page contains Groovy-evaluated HTML"
        def outputFile = new File(testDir.toFile(), "public/groovy-callout.html")
        outputFile.exists()
        def html = outputFile.text
        html.contains('class="callout primary"')
        !html.contains('&lt;')
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

    def "text assets without front matter are copied unchanged"() {
        when:
        GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then:
        def outputFile = new File(testDir.toFile(), "public/text/plain.txt")
        outputFile.exists()
        outputFile.text == 'literal ${shouldNotRender}\n'
    }

    def "text assets with front matter are rendered and stripped"() {
        when:
        GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then:
        def outputFile = new File(testDir.toFile(), "public/css/themed.css")
        outputFile.exists()
        def css = outputFile.text
        css.contains('color: #e8a838;')
        !css.contains('accent =')
        !css.contains('---')
    }

    def "minified js asset without front matter copies successfully"() {
        when:
        GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then:
        def outputFile = new File(testDir.toFile(), "public/js/app.min.js")
        outputFile.exists()
        outputFile.text == 'const app=(()=>{const msg="${notGroovy}";return{boot(){console.log(msg)}}})();app.boot();\n'
    }

    def "binary assets are copied byte for byte"() {
        when:
        GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim-gen", "--stacktrace")
                .withPluginClasspath()
                .build()

        then:
        def sourceFile = new File(testDir.toFile(), "assets/images/favicon-32.png")
        def outputFile = new File(testDir.toFile(), "public/images/favicon-32.png")
        outputFile.exists()
        outputFile.bytes == sourceFile.bytes
    }

    def cleanupSpec() {
        File testProjectDir = testDir.toFile()
        println "Cleaning up test directory: ${testProjectDir.absolutePath}"
        if (testProjectDir != null && testProjectDir.exists()) {
            testProjectDir.deleteDir()
        }
    }
}
