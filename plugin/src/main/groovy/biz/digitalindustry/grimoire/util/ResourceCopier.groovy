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
     * @param skip Either a collection of path strings/Path objects relative to {@code sourceRoot}
     *             or a predicate/closure that, given a relative {@link Path}, returns {@code true}
     *             if that path (and its descendants) should be skipped.
     */
    static void copy(Path sourceRoot, Path targetRoot, def skip = null) {
        // Build a predicate for paths to skip
        def shouldSkip
        if (skip == null) {
            shouldSkip = { Path p -> false }
        } else if (skip instanceof Collection) {
            def skipList = skip.collect { it.toString() }
            shouldSkip = { Path p -> skipList.any { p.toString().startsWith(it) } }
        } else if (skip instanceof java.util.function.Predicate) {
            shouldSkip = { Path p -> (skip as java.util.function.Predicate<Path>).test(p) }
        } else if (skip instanceof Closure) {
            shouldSkip = { Path p -> (skip as Closure).call(p) }
        } else {
            shouldSkip = { Path p -> false }
        }

        try {
            Files.walk(sourceRoot).forEach { sourcePath ->
                def relativePath = sourceRoot.relativize(sourcePath)
                if (shouldSkip(relativePath)) {
                    return
                }

                def targetPath = targetRoot.resolve(relativePath.toString())

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