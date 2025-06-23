package biz.digitalindustry.grimoire

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class SiteGenTask extends DefaultTask {

    SiteGenExtension extension

    @TaskAction
    void generate() {
        def sourceRoot = extension?.sourceDir ?: project.projectDir
        def outputDir = extension?.outputDir ?: new File(project.layout.buildDirectory.get().asFile, "site")
        def context = extension?.context ?: [:]
        def layoutDir = new File(sourceRoot, "layouts")
        def pagesDir = new File(sourceRoot, "pages")
        def engine = new Handlebars()
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

        println "Generating site from: ${sourceRoot}, output to: ${outputDir}"
        outputDir.mkdirs()

        // --- Render pages ---
        if (pagesDir.exists()) {
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
}
