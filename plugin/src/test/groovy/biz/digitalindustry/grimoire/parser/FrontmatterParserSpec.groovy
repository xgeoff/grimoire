package biz.digitalindustry.grimoire.parser


import spock.lang.Specification

class FrontmatterParserSpec extends Specification {

    def "parses valid frontmatter with content"() {
        given:
        def fixture = new File("src/test/resources/test-projects/basic-site/pages/markdown-support.md")

        when:
        def result = FrontmatterParser.parse(fixture)

        then:
        result.metadata.title == "Markdown Support"
        result.metadata.author == "Jane"
        result.metadata.layout == "default"
    }

    def "ignores missing frontmatter and returns full content"() {
        given:
        def fixture = new File("src/test/resources/test-projects/basic-site/pages/index.html")

        when:
        def result = FrontmatterParser.parse(fixture)

        then:
        result.metadata.isEmpty()
        result.content.contains("Welcome")
    }
}
