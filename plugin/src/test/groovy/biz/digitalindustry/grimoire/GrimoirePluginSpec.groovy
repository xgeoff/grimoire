package biz.digitalindustry.grimoire

import biz.digitalindustry.grimoire.task.SiteGenTask
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GrimoirePluginSpec extends Specification {

    def "plugin applies and configures tasks with defaults when no config exists"() {
        given: "A project with the grimoire plugin"
        def project = ProjectBuilder.builder().build()
        project.pluginManager.apply(GrimoirePlugin)

        when: "The grim-gen task is retrieved"
        def task = project.tasks.getByName("grim-gen") as SiteGenTask

        then: "The task is an instance of SiteGenTask"
        task instanceof SiteGenTask

        and: "The properties are set to their default values"
        task.configFile.get().asFile.name == "config.grim"
    }

    def "plugin configures tasks from an existing config.grim file"() {
        given: "A project with a custom config.grim file"
        def project = ProjectBuilder.builder().build()
        def configFile = new File(project.projectDir, "config.grim")
        configFile.text = """
            sourceDir = "my-content"
            outputDir = "dist"
        """

        and: "The grimoire plugin is applied"
        project.pluginManager.apply(GrimoirePlugin)

        when: "The grim-gen task is retrieved"
        def task = project.tasks.getByName("grim-gen") as SiteGenTask

        then: "The properties are configured from the file"
        task.sourceDir.get().asFile.name == "my-content"
        task.outputDir.get().asFile.path.endsWith("dist")
    }
}