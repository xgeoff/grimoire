package biz.digitalindustry.sitegen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class SiteGenTask extends DefaultTask {
    SiteGenExtension extension

    @TaskAction
    void generate() {
        def root = extension?.sourceDir ?: new File(project.projectDir, ".")
        def output = extension?.outputDir ?: new File(project.buildDir, "site")

        def pagesDir = new File(root, "pages")
        def layoutsDir = new File(root, "layouts")
        def layoutFile = new File(layoutsDir, "default.hbs")

        output.mkdirs()

        def engine = new TemplateEngine(layoutFile)

        pagesDir.eachFileMatch(~/.*\.html/) { File pageFile ->
            def content = pageFile.text
            def context = [title: pageFile.name.replace('.html', '').capitalize()]
            def finalHtml = engine.render(content, context)

            def target = new File(output, pageFile.name)
            target.text = finalHtml
            println "Generated: $target"
        }
    }
}
