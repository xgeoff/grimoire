package biz.digitalindustry.grimoire

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.TaskAction

class InitSiteTask extends DefaultTask {

    private String targetDir

    @Option(option = "dir", description = "Target directory for the Grimoire project skeleton")
    void setDir(String dir) {
        this.targetDir = dir
    }

    @TaskAction
    void init() {
        def targetRoot = targetDir ? new File(project.projectDir, targetDir) : project.projectDir
        if (!targetRoot.exists()) {
            targetRoot.mkdirs()
            logger.lifecycle("Created root directory: $targetRoot")
        }

        def basePath = "basic-site-scaffold/site/"
        def classLoader = getClass().classLoader
        def scaffoldRoot = classLoader.getResource(basePath)

        if (!scaffoldRoot) {
            throw new IllegalStateException("Could not find scaffold at: $basePath")
        }

        // Iterate over resources recursively
        def uri = scaffoldRoot.toURI()
        def fileRoot = new File(uri)

        if (!fileRoot.directory) {
            throw new IllegalStateException("Scaffold root is not a directory: $fileRoot")
        }

        fileRoot.eachFileRecurse { sourceFile ->
            if (sourceFile.isFile()) {
                def relativePath = sourceFile.path - fileRoot.path
                def destFile = new File(targetRoot, relativePath)

                if (!destFile.exists()) {
                    destFile.parentFile.mkdirs()
                    destFile.bytes = sourceFile.bytes
                    logger.lifecycle("Created: $destFile")
                } else {
                    logger.lifecycle("Already exists, skipping: $destFile")
                }
            }
        }

        logger.lifecycle("Grimoire site scaffold initialized at: $targetRoot")
    }
}
