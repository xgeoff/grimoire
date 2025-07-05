package biz.digitalindustry.grimoire.task

import biz.digitalindustry.grimoire.SiteGenExtension
import biz.digitalindustry.grimoire.parser.FrontmatterParser
import biz.digitalindustry.grimoire.parser.MarkdownParser
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.io.FileTemplateLoader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import io.bit3.jsass.*

class SiteGenTask extends DefaultTask {

    @Internal // prevents Gradle from treating this as an input for up-to-date checking
    SiteGenExtension siteGenExtension
    @Internal
    @Option(option = "outputDir", description = "Output directory")
    String outputDir

    @TaskAction
    void generate() {
        def context = loadConfig()
        def sourceDir = context?.sourceDir
        def sourceRoot = sourceDir == null ? project.projectDir : new File(project.projectDir, sourceDir)
        def outputDir = outputDir ?: context?.outputDir ?: "public"
        def outputRoot = new File(project.projectDir, outputDir)
        def layoutDir = new File(sourceRoot, "layouts")
        def pagesDir = new File(sourceRoot, "pages")
        def partialsDir = new File(sourceRoot, "partials")
        def helpersDir = new File(sourceRoot, "helpers")
        def assetsDir = new File(sourceRoot, "assets")
        def loader = new FileTemplateLoader(partialsDir, ".hbs")
        def engine = new Handlebars(loader)

        if (outputRoot.exists()) {
            outputRoot.deleteDir() // Groovy method: deletes dir + all contents recursively
        }
        outputRoot.mkdirs()

        def logfile = new File(project.projectDir, "log.txt")
        logfile << "Grimoire Site Generator Log\n"
        logfile << "Source root: ${sourceRoot}\n"

        logfile << "Context content: ${context}\n"
        logfile << "Author name : ${context.author?.name}\n"
        logfile << "Author year : ${context.year?.name}\n"
        /*if (partialsDir.exists()) {
            logfile << "\n Processing partials from: ${partialsDir}"
            partialsDir.eachFileMatch(~/.*\.hbs/) { File partial ->
                def name = partial.name.replaceFirst(/\.hbs$/, "")
                def content = partial.text
                engine.registerTemplate(name, engine.compileInline(content))
                println "Registered partial: $name"
            }
        }*/

        if (helpersDir.exists()) {
            logfile << "\n Processing helpers from: ${helpersDir}"
            helpersDir.eachFileMatch(~/.*\.groovy/) { File helperFile ->
                def helperName = helperFile.name.replaceFirst(/\.groovy$/, "")
                logfile << "\n About to parse helper: ${helperName}"
                def script = new GroovyShell(this.class.classLoader).parse(helperFile)
                logfile << "\n About to run helper: ${helperName}"
                def helper = script.run()
                if (helper instanceof com.github.jknack.handlebars.Helper) {
                    engine.registerHelper(helperName, helper)
                    logfile << "Registered helper: $helperName"
                } else {
                    logfile << "⚠️ Helper file '${helperFile.name}' did not return a valid Handlebars Helper"
                }
            }
        }

        logfile << "\nGenerating site from: ${sourceRoot}, output to: ${outputDir}"

        logfile << "\nPages directory: ${pagesDir.absolutePath}"
        // --- Render pages ---
        if (pagesDir.exists()) {
            logfile << "\nProcessing pages in: ${pagesDir}"
            logfile << "\n${pagesDir.list().join("\n")}"
            pagesDir.eachFileMatch(~/.*\.(html|md)/) { File pageFile ->
                logfile << "\nparsing: ${pageFile.name}"
                def parsed = FrontmatterParser.parse(pageFile)
                logfile << "\nparsed: ${pageFile.name}"
                def pageContext = parsed.metadata
                def mergedContext = context + pageContext

                // If .md, convert content to HTML
                def contentIsMarkdown = pageFile.name.endsWith(".md")
                logfile << "\ncontent is markdown: ${contentIsMarkdown}"
                def bodyContent = contentIsMarkdown
                        ? MarkdownParser.toHtml(parsed.content)
                        : parsed.content

                // Render content with Handlebars
                def renderedContent = engine.compileInline(bodyContent).apply(mergedContext)

                // Select layout
                logfile << "\nlayout in page context: ${pageContext.layout}"
                def layoutName = pageContext.layout ?: "default"
                logfile << "\nlayout is: ${layoutName}"
                def layoutFile = new File(layoutDir, "${layoutName}.hbs")
                logfile << "\nlayout file is: ${layoutFile.name}"
                if (!layoutFile.exists()) {
                    logfile << "\nlayout was not found: ${layoutFile.path}"
                    throw new IllegalStateException("Layout not found: ${layoutFile}")
                }
                logfile << "\ncompiling layout: ${layoutFile.name}"
                def layoutTemplate = engine.compileInline(layoutFile.text)
                logfile << "\nfinal output being prepared"
                def finalOutput = layoutTemplate.apply(mergedContext + [content: renderedContent])
                logfile << "\npreparing output file"
                def outFile = new File(outputRoot, pageFile.name.replaceAll(/\.(html|md)$/, '.html'))
                logfile << "\noutput file: ${outFile.name}"
                outFile.parentFile.mkdirs()
                outFile.text = finalOutput
                logfile << "\nGenerated page: ${outFile.name} (layout: ${layoutName})"
                println "Generated page: ${outFile.name} (layout: ${layoutName})"
            }
        }

        // --- Process assets ---
        if (assetsDir.exists()) {
            logfile << "\nProcessing assets in: ${assetsDir}"
            def destAssets = new File(outputRoot, "assets")

            assetsDir.eachFileRecurse { file ->
                if (file.isFile()) {
                    def relPath = file.absolutePath - assetsDir.absolutePath
                    def targetFile = new File(destAssets, relPath)

                    def rendered = engine.compileInline(file.text).apply(context)

                    if (file.name.endsWith(".scss") || file.name.endsWith(".sass")) {
                        def cssOut = new File(targetFile.parent, file.name.replaceAll(/\.s[ac]ss$/, '.css'))
                        compileSass(rendered, cssOut)
                        println "Compiled SASS: ${file.name} → ${cssOut.name}"
                    } else {
                        targetFile.parentFile.mkdirs()
                        targetFile.text = rendered
                        println "Processed asset: ${targetFile.name}"
                    }
                }
            }
        }
    }

    String compileSass(String sassText) {
        def compiler = new Compiler()
        def options = new Options()

        try {
            def output = compiler.compileString(sassText, options)
            return output.css

        } catch (CompilationException e) {
            println "SASS compile error:\n${e.message}"
            throw new RuntimeException("SASS compilation failed for ${outputFile.name}", e)
        }
    }

    Map<String, Object> loadConfig() {
        def configFile = new File(project.projectDir, 'config.grim')
        def binding = new Binding([
                outputDir : 'public',
                siteTitle : 'Untitled Site'
        ])

        if (configFile.exists()) {
            logger.lifecycle("Loading config from config.grim")
            new GroovyShell(binding).evaluate(configFile)
        } else {
            logger.lifecycle("No config.grim found. Using default settings.")
        }

        return binding.variables
    }
}
