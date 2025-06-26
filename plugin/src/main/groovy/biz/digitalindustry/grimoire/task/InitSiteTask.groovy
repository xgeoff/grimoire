package biz.digitalindustry.grimoire.task

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
        def root = targetDir ? new File(project.projectDir, targetDir) : project.projectDir
        if (!root.exists()) {
            root.mkdirs()
            logger.lifecycle("Created root directory: $root")
        }

        // Directory structure
        def structure = [
                "layouts/default.hbs",
                "pages/index.md",
                "config.grim"
        ]

        structure.each { relativePath ->
            def file = new File(root, relativePath)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.text = getResourceText("templates/${relativePath}")
                logger.lifecycle("Created: ${file}")
            } else {
                logger.lifecycle("Already exists, skipping: ${file}")
            }
        }

        logger.lifecycle("Grimoire site skeleton initialized at: ${root}")
    }

    private String getResourceText(String path) {
        def stream = getClass().classLoader.getResourceAsStream(path)
        if (!stream) {
            throw new IllegalStateException("Template resource not found: $path")
        }
        return stream.text
    }
}
