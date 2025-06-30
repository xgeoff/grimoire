package biz.digitalindustry.grimoire

import biz.digitalindustry.grimoire.task.SiteGenTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class SiteGenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // Register the extension so users can write `sitegen { ... }`
        def extension = project.extensions.create("sitegen", SiteGenExtension)

        project.tasks.register("grim-init", InitSiteTask) {
            group = "documentation"
            description = "Generate a Grimoire project"
        }
        // Register the `grim` task and inject the extension
        project.tasks.register("grim", SiteGenTask) { task ->
            task.group = "documentation"
            task.description = "Generate a static site from templates and markdown"
            task.siteGenExtension = extension
            doLast {
                project.exec {
                    commandLine "echo", "Hello from grim task"
                    standardOutput = System.out
                    errorOutput = System.err
                }
            }
        }
    }
}