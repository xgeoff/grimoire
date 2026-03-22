package biz.digitalindustry.grimoire.parser

import java.util.regex.Pattern

class FrontmatterParser {

    private static final Pattern FRONTMATTER_PATTERN = ~/(?ms)^---\s*\n(.*?)^---\s*\n?/

    static class ParsedFrontmatter {
        Map<String, Object> metadata
        String content
    }

    static ParsedFrontmatter parse(File file) {
        return parse(file.text)
    }

    static ParsedFrontmatter parse(String text) {
        def matcher = FRONTMATTER_PATTERN.matcher(text)

        if (matcher.find()) {
            def frontmatterText = matcher.group(1)
            def bodyStart = matcher.end()
            def bodyText = text.substring(bodyStart)

            def config = new ConfigSlurper().parse(frontmatterText)

            return new ParsedFrontmatter(
                    metadata: config as Map<String, Object>,
                    content: bodyText
            )
        } else {
            return new ParsedFrontmatter(
                    metadata: [:],
                    content: text
            )
        }
    }

    static boolean hasFrontmatter(String text) {
        return FRONTMATTER_PATTERN.matcher(text).find()
    }
}
