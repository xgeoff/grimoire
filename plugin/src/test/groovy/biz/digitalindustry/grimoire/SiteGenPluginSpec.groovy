import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import java.nio.file.Files
import org.gradle.testkit.runner.GradleRunner

import java.nio.file.Path
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

class SiteGenPluginSpec extends Specification {

    Path testDir
    Project project = ProjectBuilder.builder().build()

    def setup() {
        def fixture = Paths.get("src/test/resources/test-projects/basic-site")
        Path projectRoot = Paths.get(".").toAbsolutePath().normalize()
        //testDir = project.getProjectDir().toPath().resolve("grimoire-test-site")
        testDir = projectRoot.resolve("grimoire-test-site")
        testDir.toFile().delete()
        Files.createDirectories(testDir)
        //testDir = Files.createDirectory(project.getProjectDir().toPath(),"grimoire-test-site")

        copyProject(fixture, testDir)
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
        when:
        /*def result = GradleRunner.create()
                .withProjectDir(testDir.toFile())
                .withArguments("grim")
                .withPluginClasspath()
                .build()*/
        def result = GradleRunner.create()
        result.withProjectDir(testDir.toFile())
                .withArguments("grim", "--rerun-tasks")
                .withPluginClasspath()
                .withDebug(true)
                .build()

        then:
        //result.output.contains("Generated page")
        new File(testDir.toFile(), "public/index.html").text.contains("Test-Bot")
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
