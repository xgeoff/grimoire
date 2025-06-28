package biz.digitalindustry.grimoire.parser


import spock.lang.Specification

class MarkdownParserSpec extends Specification {

    def "converts markdown to HTML"() {
        given:
        def fixture = new File("src/test/resources/test-projects/basic-site/site/pages/markdown-support.md")
        def rawMarkdown = FrontmatterParser.parse(fixture).content

        when:
        def html = MarkdownParser.toHtml(rawMarkdown)

        then:
        html.contains("<h1>") || html.contains("<h2>")
        html.contains("<p>")
    }
}
