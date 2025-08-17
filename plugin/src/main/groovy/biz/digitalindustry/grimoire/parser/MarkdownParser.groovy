package biz.digitalindustry.grimoire.parser

import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.html.HtmlRenderer

class MarkdownParser {

    static final Parser parser = Parser.builder().build()
    static final HtmlRenderer renderer = HtmlRenderer.builder().build()

    static String toHtml(String markdown) {
        def document = parser.parse(markdown)
        def html = renderer.render(document)
        // Unescape HTML entities that may appear inside Handlebars helpers
        html.replace('&quot;', '"')
    }
}
