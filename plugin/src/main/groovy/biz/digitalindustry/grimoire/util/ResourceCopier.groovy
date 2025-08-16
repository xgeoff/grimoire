package biz.digitalindustry.grimoire.util

import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.io.IOException

/**
 * A utility for recursively copying directory structures, designed to work
 * with both standard and virtual (e.g., JAR) filesystems.
 */
class ResourceCopier {

    /**
     * Recursively copies the contents of a source path to a target path.
     *
     * @param sourceRoot The root path of the resources to copy.
     * @param targetRoot The destination path where resources will be copied.
     */
    static void copy(Path sourceRoot, Path targetRoot) {
        try {
            Files.walk(sourceRoot).forEach { sourcePath ->
                def targetPath = targetRoot.resolve(sourceRoot.relativize(sourcePath).toString())

                if (Files.isDirectory(sourcePath)) {
                    // Always ensure the directory exists but never replace existing directories
                    Files.createDirectories(targetPath)
                } else {
                    // Overwrite existing files but skip if the destination is a directory
                    if (targetPath.parent != null) {
                        Files.createDirectories(targetPath.parent)
                    }
                    if (!Files.isDirectory(targetPath)) {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        } catch (IOException e) {
            throw new GradleException("Failed to copy resource from '${sourceRoot}' to '${targetRoot}': ${e.message}", e)
        }
    }
}