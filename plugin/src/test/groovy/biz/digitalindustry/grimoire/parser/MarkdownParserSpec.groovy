package biz.digitalindustry.grimoire.parser


import spock.lang.Specification

class MarkdownParserSpec extends Specification {

    def "converts markdown to HTML"() {
        given:
        def fixture = new File("src/test/resources/test-projects/basic-site/pages/markdown-support.md")
        def rawMarkdown = FrontmatterParser.parse(fixture).content

        when:
        def html = MarkdownParser.toHtml(rawMarkdown)

        then:
        (html =~ /<h[1-6][^>]*>/).find()
        html.contains("<p>")
        html.contains("<table")
        (html =~ /<h[1-6][^>]*id="[^"]+"/).find()
    }
}
