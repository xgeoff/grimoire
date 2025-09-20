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
        // --- TASK REGISTRATION ---
        /*
        project.tasks.named('clean', Delete) {
            delete extension.outputDir
        }*/
        project.tasks.register('grim-serve', ServeTask) { task ->
            task.group = TASK_GROUP
            task.description = 'Serves the static site.'
            // Single source of truth: the config file drives server behavior
            task.configFile.set(extension.configFile)
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
            // Single source of truth: pass only the config file here
            task.configFile.set(extension.configFile)
            // Provide conservative defaults; replaced afterEvaluate using config.grim
            task.sourceDir.set(project.file('.'))
            task.outputDir.set(project.file('public'))
        }

        // Register the 'grim' task as an alias for 'grim-generate'
        project.tasks.register("grim") { task ->
            task.group = TASK_GROUP
            task.description = "Alias for grim-generate. Generates the static site."
            task.dependsOn(generateTaskProvider)
        }

        // Configure from config.grim immediately (for tests and simple builds)
        def initialCf = extension.configFile.get().asFile
        def initialParsed = new ConfigObject()
        if (initialCf.exists()) {
            initialParsed = new ConfigSlurper().parse(initialCf.toURI().toURL())
        }
        def initialSourcePath = initialParsed.sourceDir ?: '.'
        def initialOutputPath = initialParsed.outputDir ?: 'public'
        generateTaskProvider.configure { task ->
            task.sourceDir.set(project.file(initialSourcePath))
            task.outputDir.set(project.file(initialOutputPath))
        }

        // Resolve configuration after users had a chance to set grimoire { configFile = ... }
        project.afterEvaluate {
            def cf = extension.configFile.get().asFile
            def parsed = new ConfigObject()
            if (cf.exists()) {
                parsed = new ConfigSlurper().parse(cf.toURI().toURL())
            }
            def sourcePath = parsed.sourceDir ?: '.'
            def outputPath = parsed.outputDir ?: 'public'
            generateTaskProvider.configure { task ->
                task.sourceDir.set(project.file(sourcePath))
                task.outputDir.set(project.file(outputPath))
            }
        }
    }
}
