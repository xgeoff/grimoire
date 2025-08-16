package biz.digitalindustry.grimoire.task

import biz.digitalindustry.grimoire.util.ResourceCopier
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
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

    @Internal // This is an input for logic, but not for up-to-date checks
    abstract DirectoryProperty getProjectRootDir()
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
        destDir.mkdirs()

        if (destDir.list().length > 0) {
            logger.warn("Destination directory '{}' is not empty. Cleaning it before scaffolding.", destDir.absolutePath)
            if (!destDir.deleteDir()) {
                throw new GradleException("Could not clean destination directory: ${destDir}")
            }
            destDir.mkdirs()
        }

        logger.lifecycle("Initializing new Grimoire site in '{}' using the '{}' scaffold...", destDir.absolutePath, type.get())

        // --- NEW, MORE ROBUST LOGIC ---
        def scaffoldType = type.get()
        // 1. Use a known file as an anchor to reliably find the scaffold root.
        def anchorResourcePath = "/scaffold/${scaffoldType}"
        def anchorResourceUrl = getClass().getResource(anchorResourcePath)
        if (anchorResourceUrl == null) {
            throw new GradleException("Could not find the scaffold template for type '${scaffoldType}'. " +
                    "Ensure you have the correct type and that it exists in 'src/main/resources/scaffold'.")
        }
        def anchorUri = anchorResourceUrl?.toURI()

        // 2. Handle both JAR and filesystem contexts to get the sourceRoot Path.
        if ('jar' == anchorUri.scheme) {
            // For a JAR, the URI is complex. We open a virtual filesystem to navigate it.
            FileSystems.newFileSystem(anchorUri, [:]).withCloseable { fs ->
                // Get the path to the anchor file *inside* the JAR's virtual filesystem.
                def anchorPathInJar = fs.getPath(anchorResourcePath)
                // The path inside the JAR already points at the scaffold folder we want.
                def sourceRoot = anchorPathInJar

                ResourceCopier.copy(sourceRoot, destinationDir.get().asFile.toPath())

                copyAndModifyConfig(sourceRoot)
            }
        } else {
            // For a direct filesystem, it's simpler.
            def sourceRoot = Paths.get(anchorUri)
            ResourceCopier.copy(sourceRoot, destinationDir.get().getAsFile().toPath())
            copyAndModifyConfig(sourceRoot)
        }

        logger.lifecycle("✅ Grimoire site initialized successfully.")
    }


    /**
     * Copies and modifies the config file from the scaffold's source path.
     * This method now operates on reliable Path objects.
     */
    void copyAndModifyConfig(Path anchorPath) {
        def configFile = anchorPath.resolve("config.grim")

        if (!Files.exists(configFile)) {
            logger.warn("No 'config.grim' found in scaffold. Skipping.")
            return
        }

        def destConfigFile = new File(projectRootDir.get().asFile, "config.grim")
        // Read the text content from the source path and write to the destination file.
        destConfigFile.text = Files.readString(configFile)

        // Now, append the sourceDir configuration.
        def destDirName = destinationDir.get().asFile.name
        destConfigFile << "\nsourceDir = \"${destDirName}\""

        logger.lifecycle("Created 'config.grim' at project root and set sourceDir = '{}'", destDirName)
    }
}