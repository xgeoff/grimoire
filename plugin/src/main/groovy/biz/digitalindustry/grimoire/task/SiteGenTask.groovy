package biz.digitalindustry.grimoire.task

import biz.digitalindustry.grimoire.parser.FrontmatterParser
import biz.digitalindustry.grimoire.parser.MarkdownParser
import biz.digitalindustry.grimoire.SimpleTemplateRenderer
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
        // 1. Parse configuration first so we can honor overrides for dirs
        def config = parseConfig()
        // Normalize baseUrl for template usage: empty string for root, otherwise "/subpath" (no trailing slash)
        config.baseUrl = normalizeBaseUrlForTemplates(config.baseUrl as String)
        // Prefer directories from config.grim when provided; fall back to task properties.
        // Avoid accessing Task.project at execution time (configuration cache safety) by
        // resolving any relative paths against the config file's parent directory.
        File baseDir = configFile.get().asFile.parentFile
        def sourceRoot = (config.sourceDir ? new File(baseDir, config.sourceDir as String) : sourceDir.get().asFile)
        def outputRoot = (config.outputDir ? new File(baseDir, config.outputDir as String) : outputDir.get().asFile)

        // 2. Clean the effective output directory
        cleanOutputDir(outputRoot)

        // 3. Initialize template engines
        def engine = createHandlebarsEngine(sourceRoot, config)
        def groovyRenderer = new SimpleTemplateRenderer(new File(sourceRoot, config.paths?.partials ?: "partials"))

        // Log effective directories for clarity
        logger.lifecycle("Generating site: sourceDir={}, outputDir={}", sourceRoot.absolutePath, outputRoot.absolutePath)

        // 4. Process content and assets
        processPages(sourceRoot, outputRoot, config, engine, groovyRenderer)
        processAssets(sourceRoot, outputRoot, config, engine, groovyRenderer)

        logger.lifecycle("✅ Grimoire site generation complete.")
    }

    /**
     * Deletes the output directory to ensure a clean build.
     */
    private void cleanOutputDir(File outputRoot) {
        if (outputRoot.exists()) {
            outputRoot.eachFileRecurse(FileType.FILES) { file ->
                if (!file.delete()) {
                    throw new GradleException("Could not delete file: ${file}")
                }
            }
        } else {
            outputRoot.mkdirs()
        }
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
    private void processPages(File sourceRoot, File outputRoot, ConfigObject config, Handlebars engine, SimpleTemplateRenderer groovyRenderer) {
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

                def renderedContent
                if (pageFile.name.endsWith(".md")) {
                    def templatedMarkdown = SiteGenTask.renderAll(parsed.content, mergedContext, engine, groovyRenderer)
                    renderedContent = MarkdownParser.toHtml(templatedMarkdown)
                } else {
                    renderedContent = SiteGenTask.renderAll(parsed.content, mergedContext, engine, groovyRenderer)
                }

                def layoutName = pageContext.layout ?: "default"
                def layoutFile = new File(layoutDir, "${layoutName}.hbs")
                def groovyLayoutFile = new File(layoutDir, "${layoutName}.gtpl")

                String finalOutput
                if (layoutFile.exists()) {
                    // Compile by name, letting the loader find the file
                    def layoutTemplate = engine.compile(layoutName)
                    def hbOutput = layoutTemplate.apply(mergedContext + [content: renderedContent])
                    finalOutput = groovyRenderer.render(hbOutput, mergedContext + [content: renderedContent])
                } else if (groovyLayoutFile.exists()) {
                    def layoutText = groovyLayoutFile.text
                    finalOutput = SiteGenTask.renderAll(layoutText, mergedContext + [content: renderedContent], engine, groovyRenderer)
                } else {
                    throw new GradleException("Layout not found: ${layoutName} for page ${pageFile.name}")
                }

                def outFile = new File(outputRoot, pageFile.name.replaceAll(/\.(html|md)$/, '.html'))
                if (outFile.exists() && outFile.isDirectory()) {
                    logger.warn("Skipping page generation for '{}'; destination is a directory: {}", pageFile.name, outFile)
                } else {
                    outFile.parentFile.mkdirs()
                    outFile.text = finalOutput
                    logger.info("Generated page: {} (layout: {})", outFile.name, layoutName)
                }
            } catch (Exception e) {
                throw new GradleException("Failed to generate page from ${pageFile.name}: ${e.message}", e)
            }
        }
    }

    // In: src/main/groovy/biz/digitalindustry/grimoire/task/SiteGenTask.groovy

    /**
     * Copies and processes all asset files.
     * This version is refactored to be compatible with Gradle's configuration cache.
     */
    private void processAssets(File sourceRoot, File outputRoot, ConfigObject config, Handlebars engine, SimpleTemplateRenderer groovyRenderer) {
        def assetsDir = new File(sourceRoot, config.paths?.assets ?: "assets")
        if (!assetsDir.exists()) return

        logger.lifecycle("Processing assets from: {}", assetsDir)
        // Copy asset contents directly into the output root rather than nested under an 'assets' directory
        def destAssets = outputRoot

        // This manual traversal replaces the non-cache-safe `project.copy` call.
        assetsDir.eachFileRecurse(FileType.FILES) { file ->
            // Calculate the relative path to preserve the directory structure.
            def relPath = assetsDir.toURI().relativize(file.toURI()).path

            if (file.name.endsWith(".scss") || file.name.endsWith(".sass")) {
                // Handle SASS compilation.
                def cssOutFile = new File(destAssets, relPath.replaceAll(/\.s[ac]ss$/, '.css'))
                compileSass(file, cssOutFile)
                if (cssOutFile.exists() && !cssOutFile.isDirectory()) {
                    logger.info("Compiled SASS: {} -> {}", file.name, cssOutFile.name)
                }
            } else {
                // Handle all other assets. Only template known text files; copy binaries as-is.
                def outFile = new File(destAssets, relPath)
                if (outFile.exists() && outFile.isDirectory()) {
                    logger.warn("Skipping asset copy for '{}'; destination is a directory: {}", file.name, outFile)
                } else {
                    outFile.parentFile.mkdirs()

                    // Determine whether to treat as text/template or binary
                    def lowerName = file.name.toLowerCase()
                    def dot = lowerName.lastIndexOf('.')
                    def ext = dot >= 0 ? lowerName.substring(dot + 1) : ''
                    def textExts = [
                        'css', 'js', 'html', 'txt', 'svg', 'json', 'xml', 'map', 'hbs'
                    ]

                    try {
                        if (textExts.contains(ext)) {
                            // If it's an .hbs asset, drop the .hbs extension in the output
                            def effectiveOutFile = ext == 'hbs' ? new File(destAssets, relPath.replaceAll(/\.hbs$/, '')) : outFile
                            def rendered = SiteGenTask.renderAll(file.text, config, engine, groovyRenderer)
                            effectiveOutFile.text = rendered
                        } else {
                            // Binary or unknown type: copy bytes as-is
                            outFile.bytes = file.bytes
                        }
                    } catch (Exception e) {
                        throw new GradleException("Failed to process asset file ${file.name}", e)
                    }
                }
            }
        }
    }

    /**
     * Applies both Handlebars and Groovy simple templating to the given text.
     */
    private static String renderAll(String template, Map<String, Object> context, Handlebars engine, SimpleTemplateRenderer groovyRenderer) {
        def hbResult = engine.compileInline(template).apply(context)
        return groovyRenderer.render(hbResult, context)
    }

    /**
     * Compiles a SASS/SCSS file to a CSS file.
     */
    private void compileSass(File inputFile, File outputFile) {
        def compiler = new Compiler()
        def options = new Options()
        if (outputFile.exists() && outputFile.isDirectory()) {
            logger.warn("Skipping SASS output for '{}'; destination is a directory: {}", inputFile.name, outputFile)
            return
        }
        try {
            Output output = compiler.compileFile(inputFile.toURI(), outputFile.toURI(), options)
            outputFile.parentFile.mkdirs()
            outputFile.text = output.css
        } catch (Exception e) {
            throw new GradleException("SASS compilation failed for ${inputFile.name}", e)
        }
    }
    private static String normalizeBaseUrlForTemplates(String raw) {
        if (!raw) return ""
        String base = raw.trim()
        // Convert explicit root into empty, so templates don't produce double slashes
        if (base == "/") return ""
        if (!base.startsWith("/")) base = "/" + base
        if (base.length() > 1 && base.endsWith("/")) base = base.substring(0, base.length() - 1)
        return base
    }
}
