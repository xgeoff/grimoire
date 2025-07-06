package biz.digitalindustry.grimoire

import biz.digitalindustry.grimoire.task.SiteGenTask
import biz.digitalindustry.grimoire.task.InitSiteTask
import biz.digitalindustry.grimoire.task.ServeTask
import biz.digitalindustry.grimoire.task.ScaffoldTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete

class SiteGenPlugin implements Plugin<Project> {

        @Override
        void apply(Project project) {
            project.plugins.apply('base')
            def extension = project.extensions.create("grimoire", SiteGenExtension)
            final String TASK_GROUP = "Grimoire"

            // --- FIX for grim-generate ---
            def generateTaskProvider = project.tasks.register("grim-generate", SiteGenTask) { task ->
                task.group = TASK_GROUP
                task.description = "Generates the static site."
                task.configFile.set(extension.configFile)
                task.sourceDir.set(project.layout.projectDirectory)

                // Use the new configurable output directory from the extension
                task.outputDir.set(extension.outputDir)
            }

            // --- FIX for grim-serve ---
            project.tasks.register("grim-serve", ServeTask) { task ->
                task.group = TASK_GROUP
                task.description = "Serves the generated site locally."
                task.configFile.set(extension.configFile)

                // The serve task should also get the directory from the extension
                task.webRootDir.set(extension.outputDir)

                // Ensure the site is generated before it's served
                task.dependsOn(generateTaskProvider)
            }

            // --- CRITICAL: Integrate with the 'clean' task ---
            // Since our output is now outside the build/ directory, the default
            // 'clean' task won't delete it. We must hook into it.
            project.tasks.named('clean', Delete) {
                delete extension.outputDir
            }

        // This task already follows the pattern you want.
        project.tasks.register("grim-init", InitSiteTask) {
            group = TASK_GROUP
            description = "Initializes a new Grimoire project structure."
        }
        // Add a task to clean the generated site
        // Register a 'grim-scaffold' task
        project.tasks.register("grim-scaffold", ScaffoldTask) {
            group = TASK_GROUP
            description = "Scaffolds a new page or post."
            // You would configure this task with properties as needed.
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