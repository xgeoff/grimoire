package biz.digitalindustry.grimoire.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Scaffolds a new Grimoire site structure from a bundled template.
 * This task is ideal for initializing a new project.
 *
 * Example Usage:
 *   // Initializes using the default 'basic' scaffold type
 *   gradle grim-scaffold
 *
 *   // Explicitly choose a scaffold type and destination
 *   gradle grim-scaffold --type=basic --dest=my-new-site
 */
abstract class ScaffoldTask extends DefaultTask {

    /**
     * The destination directory where the site structure will be created.
     * Defaults to the current project directory.
     */
    @OutputDirectory
    @Option(option = "dest", description = "The destination directory for the new site.")
    abstract DirectoryProperty getDestinationDir()

    // --- NEW ---
    /**
     * The type of site to scaffold. Corresponds to a sub-directory in 'src/main/resources/scaffold'.
     */
    @Input
    @Option(option = "type", description = "The type of site to scaffold (e.g., 'basic').")
    abstract Property<String> getType()

    ScaffoldTask() {
        // Set default values for the properties
        destinationDir.convention(project.layout.projectDirectory)
        type.convention("basic") // Default to the 'basic' scaffold
    }

    @TaskAction
    void scaffold() {
        def destDir = destinationDir.get().asFile
        destDir.mkdirs() // Ensure the destination directory exists

        // Prevent accidental data loss by checking if the directory is empty
        if (destDir.list().length > 0) {
            project.delete(destDir)
        }

        logger.lifecycle("Initializing new Grimoire site in '{}' using the '{}' scaffold...", destDir.absolutePath, type.get())

        copyScaffoldFromResources(destDir)

        logger.lifecycle("Site structure copied.")

        modifyConfigFile(destDir)

        logger.lifecycle("✅ Grimoire site initialized successfully.")
    }

    // In: src/main/groovy/biz/digitalindustry/grimoire/task/ScaffoldTask.groovy

// ... inside the ScaffoldTask class ...

    private void copyScaffoldFromResources(File destDir) {
        def scaffoldType = type.get()
        def resourcePath = "/scaffold/${scaffoldType}"
        def resourceUri = getClass().getResource(resourcePath)?.toURI()

        if (resourceUri == null) {
            throw new GradleException("Could not find the scaffold template for type '${scaffoldType}' at path: '${resourcePath}'. Ensure it's in 'src/main/resources/scaffold/${scaffoldType}'.")
        }

        // --- THIS IS THE FIX ---
        // This logic now correctly handles running from an IDE (a 'file:' URI)
        // or from a packaged plugin (a 'jar:' URI).
        if (resourceUri.scheme == 'jar') {
            // Running from a JAR, so we use a virtual filesystem to look inside it
            FileSystems.newFileSystem(resourceUri, [:]).withCloseable { fs ->
                def sourcePath = fs.getPath(resourcePath)
                copyPath(sourcePath, destDir.toPath())
            }
        } else {
            // Running from the filesystem, so we can get the path directly
            def sourcePath = Paths.get(resourceUri)
            copyPath(sourcePath, destDir.toPath())
        }
    }

/**
 * Helper method to recursively copy a directory structure.
 */
    private void copyPath(java.nio.file.Path sourceRoot, java.nio.file.Path targetRoot) {
        Files.walk(sourceRoot).forEach { source ->
            def relativePath = sourceRoot.relativize(source).toString()
            // Skip the root of the walk which has an empty relative path
            if (relativePath) {
                def destination = targetRoot.resolve(relativePath)
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination)
                } else {
                    Files.copy(source, destination)
                }
            }
        }
    }

    private void modifyConfigFile(File destDir) {
        def configFile = new File(destDir, "config.grim")
        if (!configFile.exists()) {
            logger.warn("Could not find 'config.grim' in the scaffold to modify.")
            return
        }

        //def publicDir = publicDirName.get()
        //def configText = configFile.text
        configFile << "\nsourceDir = \"${destDir.getName()}\""

        // Use a regex to safely replace the outputDir property if it exists

        //if (configText.find(/outputDir\s*=/)) {
        //    configText = configText.replaceAll(/(outputDir\s*=\s*).*/, "\$1\"${publicDir}\"")
        //} else {
            // Otherwise, append it to the end of the file
        //    configText += "\noutputDir = \"${publicDir}\""
        //}

        //configFile.text = configText
        logger.lifecycle("Updated 'config.grim' to set outputDir = '{}'", destDir)
    }
}