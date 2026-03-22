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

    def "parses nested front matter structures"() {
        given:
        def fixture = File.createTempFile("nested", ".md")
        fixture.text = '''---
title = "Nested"
tags = ["alpha", "beta"]
sidebar {
    title = "Meta"
    sections = [
        [
            title: "Intro",
            links: [
                [label: "Option", href: "option.html"]
            ]
        ]
    ]
}
---

# Nested
---'''

        when:
        def result = FrontmatterParser.parse(fixture)

        then:
        result.metadata.sidebar.title == "Meta"
        result.metadata.sidebar.sections[0].links[0].label == "Option"
        result.metadata.tags == ["alpha", "beta"]

        cleanup:
        fixture.delete()
    }

    def "preserves asset body exactly after stripping front matter"() {
        given:
        def text = '''---
accent = "#e8a838"
---
body { color: ${accent}; }
'''

        when:
        def result = FrontmatterParser.parse(text)

        then:
        result.metadata.accent == "#e8a838"
        result.content == 'body { color: ${accent}; }\n'
    }
}
