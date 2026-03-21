package biz.digitalindustry.grimoire

import biz.digitalindustry.grimoire.task.SiteGenTask
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.nio.file.Files

class PageMetadataSpec extends Specification {
    def "page metadata is injected safely"() {
        given:
        def project = ProjectBuilder.builder().build()
        def task = project.tasks.create("pageMeta", SiteGenTask)
        File siteDir = new File("src/test/resources/test-projects/page-meta-site").absoluteFile
        task.sourceDir.set(siteDir)
        task.configFile.set(new File(siteDir, "config.grim"))
        File outDir = Files.createTempDirectory("grim-page-meta").toFile()
        task.outputDir.set(outDir)

        when:
        task.generate()

        then:
        def withSidebar = new File(outDir, "with-sidebar.html").text
        withSidebar.contains("Sidebar title: Learn Grim")
        withSidebar.contains("Sections link: Canonical Grammar")
        withSidebar.contains("Page url: /docs-site/with-sidebar")
        withSidebar.contains("Tags: alpha, beta")
        def withoutSidebar = new File(outDir, "without-sidebar.html").text
        withoutSidebar.contains("Sidebar title: none")
        withoutSidebar.contains("Page path: without-sidebar")
        withoutSidebar.contains("Sections link: missing")

        cleanup:
        outDir?.deleteDir()
    }
}
