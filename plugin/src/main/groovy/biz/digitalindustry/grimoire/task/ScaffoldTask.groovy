package biz.digitalindustry.grimoire.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.text.SimpleDateFormat

/**
 * Scaffolds a new content file (e.g., a post or a page) from a template.
 * This task is designed to be run from the command line with options.
 *
 * Example Usage:
 *   gradle grim-scaffold --type=post --title="My First Post"
 *   gradle grim-scaffold --type=page --title="About Us"
 */
abstract class ScaffoldTask extends DefaultTask {

    /**
     * The type of content to scaffold. Expected values: 'post', 'page'.
     * This is configured via a command-line option.
     */
    @Input
    @Option(option = "type", description = "The type of content to create (e.g., 'post', 'page').")
    abstract Property<String> getType()

    /**
     * The title for the new content file.
     * This is configured via a command-line option.
     */
    @Input
    @Option(option = "title", description = "The title of the new post or page.")
    abstract Property<String> getTitle()

    /**
     * The main configuration file for the Grimoire site.
     * Used to find the correct directories for pages and posts.
     */
    @InputFile
    abstract RegularFileProperty getConfigFile()

    /**
     * The root source directory for the Grimoire project.
     */
    @InputDirectory
    abstract DirectoryProperty getSourceDir()

    @TaskAction
    void scaffold() {
        // 1. Validate command-line inputs
        validateInputs()

        // 2. Load configuration to find content paths
        def config = parseConfig()
        def scaffoldType = type.get().toLowerCase()
        def contentTitle = title.get()

        // 3. Determine the target directory based on the type
        def targetDir = getTargetDirectory(config, scaffoldType)
        targetDir.mkdirs() // Ensure the directory exists

        // 4. Generate the filename and the full file path
        def slug = slugify(contentTitle)
        def datePrefix = (scaffoldType == 'post') ? new SimpleDateFormat("yyyy-MM-dd-").format(new Date()) : ""
        def newFileName = "${datePrefix}${slug}.md"
        def newFile = new File(targetDir, newFileName)

        if (newFile.exists()) {
            throw new GradleException("File already exists: ${newFile.path}. Aborting to prevent overwrite.")
        }

        // 5. Create the content from a template
        def fileContent = createContentTemplate(contentTitle)

        // 6. Write the new file
        newFile.text = fileContent

        logger.lifecycle("✅ Successfully created ${scaffoldType}: ${newFile.path}")
    }

    /**
     * Validates that required command-line options are provided.
     */
    private void validateInputs() {
        if (!type.isPresent() || !title.isPresent()) {
            throw new GradleException("Missing required options. Usage: gradle grim-scaffold --type=<type> --title=<title>")
        }
        def scaffoldType = type.get().toLowerCase()
        if (scaffoldType !in ['post', 'page']) {
            throw new GradleException("Invalid type '${type.get()}'. Must be 'post' or 'page'.")
        }
    }

    /**
     * Parses the main config.grim file.
     */
    private def parseConfig() {
        def configFile = this.configFile.get().asFile
        return configFile.exists() ? new ConfigSlurper().parse(configFile.toURI().toURL()) : [:]
    }

    /**
     * Determines the correct output directory based on the scaffold type and config.
     */
    private File getTargetDirectory(def config, String scaffoldType) {
        def sourceRoot = sourceDir.get().asFile
        // Use paths from config, with sensible defaults
        def pathKey = (scaffoldType == 'post') ? 'posts' : 'pages'
        def contentPath = config.paths?."${pathKey}" ?: pathKey // e.g., config.paths.posts or "posts"
        return new File(sourceRoot, contentPath)
    }

    /**
     * Creates the default frontmatter and content for the new file.
     */
    private String createContentTemplate(String contentTitle) {
        def creationDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date())
        return """\
            ---
            title: "${contentTitle}"
            date: ${creationDate}
            ---

            Write your content here.
            """.stripIndent()
    }

    /**
     * Converts a string into a URL-friendly "slug".
     * Example: "My First Post!" -> "my-first-post"
     */
    private String slugify(String text) {
        return text.toLowerCase()
                .replaceAll(/\s+/, '-')           // Replace spaces with -
                .replaceAll(/[^\w\-]+/, '')      // Remove all non-word chars except -
                .replaceAll(/\-\-+/, '-')         // Replace multiple - with single -
                .replaceAll(/^-+/, '')           // Trim - from start of text
                .replaceAll(/-+$/, '')           // Trim - from end of text
    }
}