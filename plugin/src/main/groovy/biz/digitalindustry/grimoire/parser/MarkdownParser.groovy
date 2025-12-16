package biz.digitalindustry.grimoire.parser

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser

class MarkdownParser {

    // Enable Markdown table support (and ensure renderer knows about it)
    static final def extensions = [TablesExtension.create()]
    static final Parser parser = Parser.builder().extensions(extensions).build()
    static final HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build()

    static String toHtml(String markdown) {
        def document = parser.parse(markdown)
        def html = renderer.render(document)
        // Unescape HTML entities that may appear inside Handlebars helpers
        html.replace('&quot;', '"')
    }
}
