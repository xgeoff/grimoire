package biz.digitalindustry.grimoire.parser

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

class MarkdownParser {

    private static final MutableDataSet OPTIONS = new MutableDataSet()
        .set(Parser.EXTENSIONS, [TablesExtension.create()])
        .set(HtmlRenderer.GENERATE_HEADER_ID, true)
        .set(HtmlRenderer.RENDER_HEADER_ID, true)
    static final Parser parser = Parser.builder(OPTIONS).build()
    static final HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build()

    static String toHtml(String markdown) {
        def document = parser.parse(markdown)
        def html = renderer.render(document)
        html.replace('&quot;', '"')
    }
}
