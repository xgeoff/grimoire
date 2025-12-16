package biz.digitalindustry.grimoire

import biz.digitalindustry.grimoire.task.SiteGenTask
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import java.nio.file.Files

class GroovyTemplateSupportSpec extends Specification {
    def "renders groovy templates and partials"() {
        given:
        def project = ProjectBuilder.builder().build()
        def task = project.tasks.create("site", SiteGenTask)
        File siteDir = new File("src/test/resources/test-projects/groovy-site").absoluteFile
        task.sourceDir.set(siteDir)
        task.configFile.set(new File(siteDir, "config.grim"))
        File outDir = Files.createTempDirectory("grim-out").toFile()
        task.outputDir.set(outDir)

        when:
        task.generate()

        then:
        def output = new File(outDir, "index.html").text
        output.contains("<h1>Test</h1>")
        output.contains("<p>Hi Test</p>")
        output.contains("<footer>2024</footer>")
    }
}
