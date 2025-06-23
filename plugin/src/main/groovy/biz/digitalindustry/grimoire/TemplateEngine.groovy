package biz.digitalindustry.sitegen

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template

class TemplateEngine {
    File layoutFile
    Handlebars handlebars = new Handlebars()

    TemplateEngine(File layoutFile) {
        this.layoutFile = layoutFile
    }

    String render(String pageHtml, Map<String, Object> context = [:]) {
        context.content = pageHtml
        Template template = handlebars.compile(layoutFile.path)
        return template.apply(context)
    }
}
