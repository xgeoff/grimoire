package biz.digitalindustry.grimoire.task

import biz.digitalindustry.grimoire.SiteGenExtension
import biz.digitalindustry.grimoire.parser.FrontmatterParser
import biz.digitalindustry.grimoire.parser.MarkdownParser
import com.github.jknack.handlebars.Handlebars
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

class SiteGenTask extends DefaultTask {

    @org.gradle.api.tasks.Internal // prevents Gradle from treating this as an input for up-to-date checking
    SiteGenExtension siteGenExtension
/*
    @Option(option = "outputDir", description = "Output directory")
    String outputDir
*/
    @TaskAction
    void generate() {
        def context = loadConfig()
        def sourceRoot = new File(context?.sourceDir)
        def outputDir = new File(sourceRoot, context?.outputDir)
        def layoutDir = new File(sourceRoot, "layouts")
        def pagesDir = new File(sourceRoot, "pages")
        def engine = new Handlebars()



        def partialsDir = new File(sourceRoot, "partials")
        if (partialsDir.exists()) {
            partialsDir.eachFileMatch(~/.*\.hbs/) { File partial ->
                def name = partial.name.replaceFirst(/\.hbs$/, "")
                def content = partial.text
                engine.registerTemplate(name, engine.compileInline(content))
                println "Registered partial: $name"
            }
        }

        def helpersDir = new File(sourceRoot, "helpers")
        if (helpersDir.exists()) {
            helpersDir.eachFileMatch(~/.*\.groovy/) { File helperFile ->
                def helperName = helperFile.name.replaceFirst(/\.groovy$/, "")
                def script = new GroovyShell().parse(helperFile)
                def helper = script.run()
                if (helper instanceof com.github.jknack.handlebars.Helper) {
                    engine.registerHelper(helperName, helper)
                    println "Registered helper: $helperName"
                } else {
                    println "⚠️ Helper file '${helperFile.name}' did not return a valid Handlebars Helper"
                }
            }
        }

        def msg = "Generating site from: ${sourceRoot}, output to: ${outputDir}"

        if (outputDir.exists()) {
            outputDir.deleteDir() // Groovy method: deletes dir + all contents recursively
        }
        outputDir.mkdirs()

        def logfile = new File(outputDir, "log.txt")
        logfile << "hello world"
        logfile << "\n$msg"
        msg = pagesDir.exists() ? "Found pages directory: ${pagesDir}" : "No pages directory found"
        logfile << "\n$msg"
        logfile << "\n${pagesDir.absolutePath}"
        // --- Render pages ---
        if (pagesDir.exists()) {
            logfile << pagesDir.list().joinToString("\n")
            pagesDir.eachFileMatch(~/.*\.(html|md)/) { File pageFile ->
                def parsed = FrontmatterParser.parse(pageFile)
                def pageContext = parsed.metadata
                def mergedContext = context + pageContext

                // If .md, convert content to HTML
                def contentIsMarkdown = pageFile.name.endsWith(".md")
                def bodyContent = contentIsMarkdown
                        ? MarkdownParser.toHtml(parsed.content)
                        : parsed.content

                // Render content with Handlebars
                def renderedContent = engine.compileInline(bodyContent).apply(mergedContext)

                // Select layout
                def layoutName = pageContext.layout ?: "default"
                def layoutFile = new File(layoutDir, "${layoutName}.hbs")
                if (!layoutFile.exists()) {
                    throw new IllegalStateException("Layout not found: ${layoutFile}")
                }

                def layoutTemplate = engine.compile(layoutFile.path)
                def finalOutput = layoutTemplate.apply(mergedContext + [content: renderedContent])

                def outFile = new File(outputDir, pageFile.name.replaceAll(/\.(html|md)$/, '.html'))
                outFile.parentFile.mkdirs()
                outFile.text = finalOutput

                println "Generated page: ${outFile.name} (layout: ${layoutName})"
            }
        }

        // --- Process assets ---
        def assetsDir = new File(sourceRoot, "assets")
        if (assetsDir.exists()) {
            def destAssets = new File(outputDir, "assets")

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

    void compileSass(String sassText, File outputFile) {
        def tmpFile = File.createTempFile("grimoire-sass", ".scss")
        tmpFile.text = sassText

        def cmd = ["sass", "--no-source-map", tmpFile.absolutePath, outputFile.absolutePath]
        def proc = cmd.execute()
        proc.waitFor()

        if (proc.exitValue() != 0) {
            println "SASS compile error:\n${proc.err.text}"
            throw new RuntimeException("SASS compilation failed for ${outputFile.name}")
        }

        tmpFile.delete()
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

        def sourceDirFile
        def sourceDir = binding.getVariable('sourceDir')
        if (!sourceDir) {
            sourceDir = ""
            binding.setVariable('sourceDir', sourceDir)
            sourceDirFile = project.projectDir
            logger.lifecycle("Using default source directory: ${sourceDir}")
        } else {
            sourceDirFile = new File(project.projectDir, sourceDir)
            if (!sourceDirFile.exists()) {
                throw new IllegalStateException("Source directory does not exist: ${sourceDirFile}")
            }
            logger.lifecycle("Using source directory: ${sourceDirFile}")
        }

        def layoutsDir = new File(sourceDirFile, "layouts")
        def pagesDir = new File(sourceDirFile, 'pages')

        if (!layoutFile.exists()) throw new IllegalStateException("Missing layout file: ${layoutFile}")
        if (!pagesDir.exists()) throw new IllegalStateException("Missing pages directory: ${pagesDir}")

        return binding.variables
    }
}
