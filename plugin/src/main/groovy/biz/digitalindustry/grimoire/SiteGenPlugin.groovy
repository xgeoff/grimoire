package biz.digitalindustry.grimoire

import biz.digitalindustry.grimoire.task.SiteGenTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class SiteGenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // Register the extension so users can write `sitegen { ... }`
        def extension = project.extensions.create("sitegen", SiteGenExtension)
// Use a descriptive group name. This will group all your tasks
        // together nicely in the `gradle tasks` output.
        final String TASK_GROUP = "Grimoire"

        // This task already follows the pattern you want.
        project.tasks.register("grim-init", InitSiteTask) {
            group = TASK_GROUP
            description = "Initializes a new Grimoire project structure."
        }

        project.tasks.register("grim-generate", SiteGenTask) { task ->
            task.group = TASK_GROUP
            task.description = "Generates the static site from templates and markdown."
            task.siteGenExtension = extension
        }
        // Add a task to clean the generated site
        // Register a 'grim-scaffold' task
        project.tasks.register("grim-scaffold", ScaffoldTask) {
            group = TASK_GROUP
            description = "Scaffolds a new page or post."
            // You would configure this task with properties as needed.
        }

        // Register a 'grim-serve' task
        project.tasks.register("grim-serve", ServeTask) {
            group = TASK_GROUP
            description = "Serves the generated site locally for development."
            // This task should likely run after the site is generated.
            dependsOn("grim-generate")
        }

        // Register the `grim` task and inject the extension
        project.tasks.register("grim", SiteGenTask) { task ->
            task.group = TASK_GROUP
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