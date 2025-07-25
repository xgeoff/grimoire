package biz.digitalindustry.grimoire.task

import biz.digitalindustry.grimoire.parser.FrontmatterParser
import biz.digitalindustry.grimoire.parser.MarkdownParser
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.CompositeTemplateLoader
import com.github.jknack.handlebars.io.FileTemplateLoader
import groovy.io.FileType
import io.bit3.jsass.Compiler
import io.bit3.jsass.Options
import io.bit3.jsass.Output
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * The main task for generating the static site.
 * This version is refactored to use modern Gradle APIs for better performance and maintainability.
 */
@CacheableTask // This task can be cached if inputs/outputs are the same across builds
abstract class SiteGenTask extends DefaultTask {

    // --- Task Inputs ---
    // By declaring inputs, we enable Gradle's up-to-date checking.
    // The task will only run if one of these files or directories changes.

    @InputFile
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract RegularFileProperty getConfigFile()

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getSourceDir()

    // --- Task Outputs ---
    // Declaring the output directory allows Gradle to know what this task creates.
    // This is crucial for incremental builds, caching, and dependency management.

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    void generate() {
        // 1. Setup: Clean output directory and parse configuration
        cleanOutputDir()
        def config = parseConfig()
        def sourceRoot = sourceDir.get().asFile
        def outputRoot = outputDir.get().asFile

        // 2. Initialize Handlebars engine
        def engine = createHandlebarsEngine(sourceRoot, config)

        // 3. Process content and assets
        processPages(sourceRoot, outputRoot, config, engine)
        processAssets(sourceRoot, outputRoot, config, engine)

        logger.lifecycle("✅ Grimoire site generation complete.")
    }

    /**
     * Deletes the output directory to ensure a clean build.
     */
    private void cleanOutputDir() {
        def outputRoot = outputDir.get().asFile
        if (outputRoot.exists()) {
            // FIX: Replace the project.delete() call with a standard file operation.
            // The deleteDir() method is a Groovy extension that recursively deletes.
            // This makes the task compatible with Gradle's configuration caching.
            if (!outputRoot.deleteDir()) {
                throw new GradleException("Could not clean output directory: ${outputRoot}")
            }
        }
        outputRoot.mkdirs()
        logger.info("Cleaned output directory: {}", outputRoot)
    }

    /**
     * Parses the main config.grim file.
     */
    private ConfigObject parseConfig() {
        def configFile = this.configFile.get().asFile
        if (configFile.exists()) {
            logger.info("Loading config from {}", configFile)
            return new ConfigSlurper().parse(configFile.toURI().toURL())
        }
        logger.warn("No config file found at {}. Using empty configuration.", configFile)
        return new ConfigObject()
    }

/**
 * Creates and configures the Handlebars template engine.
 */
    private Handlebars createHandlebarsEngine(File sourceRoot, ConfigObject config) {
        def layoutsDir = new File(sourceRoot, config.paths?.layouts ?: "layouts")
        def partialsDir = new File(sourceRoot, config.paths?.partials ?: "partials")
        def helpersDir = new File(sourceRoot, config.paths?.helpers ?: "helpers")

        // --- FIX: Create loaders for both layouts and partials ---
        def layoutLoader = new FileTemplateLoader(layoutsDir, ".hbs")
        def partialLoader = new FileTemplateLoader(partialsDir, ".hbs")

        // Combine them so Handlebars can find templates in either directory
        def compositeLoader = new CompositeTemplateLoader(layoutLoader, partialLoader)
        def engine = new Handlebars(compositeLoader)

        // Register helpers from .groovy files (this part remains the same)
        if (helpersDir.exists()) {
            logger.info("Registering helpers from: {}", helpersDir)
            helpersDir.eachFileMatch(~/.*\.groovy/) { File helperFile ->
                def helperName = helperFile.name.replaceFirst(/\.groovy$/, "")
                try {
                    def script = new GroovyShell(this.class.classLoader).parse(helperFile)
                    def helper = script.run()
                    if (helper instanceof com.github.jknack.handlebars.Helper) {
                        engine.registerHelper(helperName, helper)
                        logger.debug("Registered helper: {}", helperName)
                    } else {
                        logger.warn("Helper file '{}' did not return a valid Handlebars Helper instance.", helperFile.name)
                    }
                } catch (Exception e) {
                    logger.error("Failed to load helper '${helperName}'", e)
                }
            }
        }
        return engine
    }

    // In: src/main/groovy/biz/digitalindustry/grimoire/task/SiteGenTask.groovy
    /**
     * Finds and renders all page files (.html, .md).
     */
    private void processPages(File sourceRoot, File outputRoot, ConfigObject config, Handlebars engine) {
        def pagesDir = new File(sourceRoot, config.paths?.pages ?: "pages")
        def layoutDir = new File(sourceRoot, config.paths?.layouts ?: "layouts")

        if (!pagesDir.exists()) return

        config.put("pagesDir", pagesDir)
        logger.lifecycle("Processing pages from: {}", pagesDir)
        pagesDir.eachFileMatch(~/.*\.(html|md)/) { File pageFile ->
            try {
                def parsed = FrontmatterParser.parse(pageFile)
                def pageContext = parsed.metadata
                def mergedContext = config + pageContext

                def bodyContent = pageFile.name.endsWith(".md") ? MarkdownParser.toHtml(parsed.content) : parsed.content
                def renderedContent = engine.compileInline(bodyContent).apply(mergedContext)

                def layoutName = pageContext.layout ?: "default"
                def layoutFile = new File(layoutDir, "${layoutName}.hbs")

                if (!layoutFile.exists()) {
                    throw new GradleException("Layout not found: ${layoutFile.path} for page ${pageFile.name}")
                }

                // --- FIX: Compile by name, letting the loader find the file ---
                // The loader will automatically add the '.hbs' suffix.
                def layoutTemplate = engine.compile(layoutName)
                def finalOutput = layoutTemplate.apply(mergedContext + [content: renderedContent])

                def outFile = new File(outputRoot, pageFile.name.replaceAll(/\.(html|md)$/, '.html'))
                outFile.parentFile.mkdirs()
                outFile.text = finalOutput
                logger.info("Generated page: {} (layout: {})", outFile.name, layoutName)
            } catch (Exception e) {
                throw new GradleException("Failed to generate page from ${pageFile.name}", e)
            }
        }
    }

    // In: src/main/groovy/biz/digitalindustry/grimoire/task/SiteGenTask.groovy

    /**
     * Copies and processes all asset files.
     * This version is refactored to be compatible with Gradle's configuration cache.
     */
    private void processAssets(File sourceRoot, File outputRoot, ConfigObject config, Handlebars engine) {
        def assetsDir = new File(sourceRoot, config.paths?.assets ?: "assets")
        if (!assetsDir.exists()) return

        logger.lifecycle("Processing assets from: {}", assetsDir)
        def destAssets = new File(outputRoot, "assets")

        // This manual traversal replaces the non-cache-safe `project.copy` call.
        assetsDir.eachFileRecurse(FileType.FILES) { file ->
            // Calculate the relative path to preserve the directory structure.
            def relPath = assetsDir.toURI().relativize(file.toURI()).path

            if (file.name.endsWith(".scss") || file.name.endsWith(".sass")) {
                // Handle SASS compilation.
                def cssOutFile = new File(destAssets, relPath.replaceAll(/\.s[ac]ss$/, '.css'))
                compileSass(file, cssOutFile)
                logger.info("Compiled SASS: {} -> {}", file.name, cssOutFile.name)
            } else {
                // Handle all other assets (copy and apply Handlebars templating).
                def outFile = new File(destAssets, relPath)
                outFile.parentFile.mkdirs()

                try {
                    def rendered = engine.compileInline(file.text).apply(config)
                    outFile.text = rendered
                } catch (Exception e) {
                    throw new GradleException("Failed to template asset file ${file.name}", e)
                }
            }
        }
    }

    /**
     * Compiles a SASS/SCSS file to a CSS file.
     */
    private void compileSass(File inputFile, File outputFile) {
        def compiler = new Compiler()
        def options = new Options()
        try {
            Output output = compiler.compileFile(inputFile.toURI(), outputFile.toURI(), options)
            outputFile.parentFile.mkdirs()
            outputFile.text = output.css
        } catch (Exception e) {
            throw new GradleException("SASS compilation failed for ${inputFile.name}", e)
        }
    }
}