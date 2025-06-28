import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import java.nio.file.Files
import org.gradle.testkit.runner.GradleRunner

import java.nio.file.Path
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

class SiteGenPluginSpec extends Specification {

    Path testDir

    def setup() {
        def fixture = Paths.get("src/test/resources/test-projects/basic-site")
        testDir = Files.createTempDirectory("grimoire-test-site")

        copyProject(fixture, testDir)
    }

    def "plugin registers grim task"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("biz.digitalindustry.grimoire")

        then:
        project.tasks.findByName("grim") != null
    }

    def "plugin registers grim-init task"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("biz.digitalindustry.grimoire")

        then:
        project.tasks.findByName("grim-init") != null
    }

    def "renders site from fixture project"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim")
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("Generated page")
        new File(testDir, "build/site/index.html").text.contains("Hello")
    }

    void copyProject(Path source, Path target) {
        Files.walk(source).forEach { path ->
            Path relative = source.relativize(path)
            Path dest = target.resolve(relative)
            if (Files.isDirectory(path)) {
                Files.createDirectories(dest)
            } else {
                Files.copy(path, dest, REPLACE_EXISTING)
            }
        }
    }
}
