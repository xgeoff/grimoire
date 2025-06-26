package biz.digitalindustry.grimoire.parser

class FrontmatterParser {

    static class ParsedFrontmatter {
        Map<String, Object> metadata
        String content
    }

    static ParsedFrontmatter parse(File file) {
        def text = file.text

        // Match frontmatter block: starts with --- and ends with ---
        def pattern = ~/(?ms)^---\s*\n(.*?)^---\s*\n?/
        def matcher = pattern.matcher(text)

        if (matcher.find()) {
            def frontmatterText = matcher.group(1)
            def bodyStart = matcher.end()
            def bodyText = text.substring(bodyStart)

            def config = new ConfigSlurper().parse(frontmatterText)

            return new ParsedFrontmatter(
                    metadata: config as Map<String, Object>,
                    content: bodyText.trim()
            )
        } else {
            // No frontmatter, return full content
            return new ParsedFrontmatter(
                    metadata: [:],
                    content: text.trim()
            )
        }
    }
}
