package biz.digitalindustry.grimoire.helpers

import com.github.jknack.handlebars.Handlebars
import groovy.lang.Binding
import groovy.lang.GroovyShell
import spock.lang.Specification

class OpenCloseHelperSpec extends Specification {

    private final Handlebars handlebars = new Handlebars()

    private Object loadHelper(String name) {
        def binding = new Binding([handlebars: handlebars])
        def shell = new GroovyShell(binding)
        def url = this.class.classLoader.getResource("scaffold/basic/helpers/${name}.groovy")
        shell.evaluate(url.toURI())
    }

    def "open helper renders tag with optional class"() {
        given:
        handlebars.registerHelper("open", loadHelper("open"))

        expect:
        handlebars.compileInline(template).apply([:]) == expected

        where:
        template                           || expected
        '{{open "div"}}'                   || '<div>'
        '{{open "div" class="container"}}' || '<div class="container">'
        '{{open "div" class="<evil>"}}'    || '<div class="&lt;evil&gt;">'
    }

    def "close helper renders closing tag"() {
        given:
        handlebars.registerHelper("close", loadHelper("close"))

        expect:
        handlebars.compileInline(template).apply([:]) == expected

        where:
        template         || expected
        '{{close "div"}}' || '</div>'
        '{{close "span"}}' || '</span>'
    }
}

