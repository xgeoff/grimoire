package biz.digitalindustry.grimoire

import biz.digitalindustry.grimoire.task.ScaffoldTask
import biz.digitalindustry.grimoire.task.SiteGenTask
import biz.digitalindustry.grimoire.task.ServeTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class GrimoirePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.plugins.apply('base')
        def extension = project.extensions.create("grimoire", SiteGenExtension)
        final String TASK_GROUP = "Grimoire"
        // --- CONFIGURATION LOGIC ---
        // This logic runs during Gradle's configuration phase.

        // 1. Define the conventional location for the config file.
        def configFile = project.file("config.grim")
        def config = new ConfigObject()

        // 2. Parse the config file if it exists.
        if (configFile.exists()) {
            config = new ConfigSlurper().parse(configFile.toURI().toURL())
        }

        // 3. Determine the source and output directories based on the config.
        //    Default to 'src' and 'build/grimoire' if not specified.
        def sourcePath = config.sourceDir ?: '.'
        def outputPath = config.outputDir ?: '.'

        // --- TASK REGISTRATION ---
        /*
        project.tasks.named('clean', Delete) {
            delete extension.outputDir
        }*/
        project.tasks.register('grim-serve', ServeTask) { task ->
            task.group = TASK_GROUP
            task.description = 'Serves the static site.'

            task.configFile.set(configFile)
            task.outputDir.set(project.file(outputPath))
        }

        project.tasks.register('grim-init', ScaffoldTask) { task ->
            task.group = TASK_GROUP
            task.description = 'Initializes a new Grimoire site structure.'
            // FIX: Configure the new projectRootDir property.
            task.projectRootDir.set(project.layout.projectDirectory)
        }

        def generateTaskProvider = project.tasks.register('grim-gen', SiteGenTask) { task ->
            task.group = TASK_GROUP
            task.description = 'Generates the static site.'

            task.configFile.set(configFile)
            task.sourceDir.set(project.file(sourcePath))
            //task.outputDir.set(project.layout.buildDirectory.dir(outputPath))
            task.outputDir.set(project.file(outputPath))
        }

        // Register the 'grim' task as an alias for 'grim-generate'
        project.tasks.register("grim") { task ->
            task.group = TASK_GROUP
            task.description = "Alias for grim-generate. Generates the static site."
            task.dependsOn(generateTaskProvider)
        }
    }
}