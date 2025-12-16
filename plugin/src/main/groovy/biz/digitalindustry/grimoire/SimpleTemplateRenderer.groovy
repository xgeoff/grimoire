package biz.digitalindustry.grimoire

import groovy.text.SimpleTemplateEngine

/**
 * Renders Groovy simple templates with support for partials.
 * Partials are loaded from a directory and can be referenced from templates
 * using the `partial` closure, e.g. `${partial('name')}`.
 */
class SimpleTemplateRenderer {
    private final SimpleTemplateEngine engine = new SimpleTemplateEngine()
    private final Map<String, String> partials = [:]

    SimpleTemplateRenderer(File partialsDir) {
        if (partialsDir?.exists()) {
            partialsDir.eachFileMatch(~/.*\.gtpl/) { File f ->
                def name = f.name.replaceFirst(/\.gtpl$/, '')
                partials[name] = f.text
            }
        }
    }

    String render(String template, Map<String, Object> context) {
        def hasSyntax = (template =~ /\$\{[^}]+\}/) || (template =~ /(?m)\$[A-Za-z_][A-Za-z0-9_]*/) || template.contains('<%')
        if (!hasSyntax) {
            return template
        }
        Closure partial = null
        partial = { String name, Map<String, Object> model = [:] ->
            def tpl = partials[name]
            if (tpl == null) return ''
            render(tpl, (context + model) + [partial: partial])
        }
        try {
            return engine.createTemplate(template).make(context + [partial: partial]).toString()
        } catch (IllegalArgumentException ignored) {
            return template
        }
    }
}
