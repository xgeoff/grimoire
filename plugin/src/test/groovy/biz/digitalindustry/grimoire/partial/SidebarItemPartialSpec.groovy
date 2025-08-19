package biz.digitalindustry.grimoire.partial

import com.github.jknack.handlebars.Handlebars
import spock.lang.Specification

class SidebarItemPartialSpec extends Specification {
    def "includes baseUrl in file links"() {
        given:
        def handlebars = new Handlebars()
        def eqHelper = new GroovyShell().evaluate(new File('src/main/resources/scaffold/basic/helpers/eq.groovy'))
        handlebars.registerHelper('eq', eqHelper)
        def htmlPathHelper = new GroovyShell().evaluate(new File('src/main/resources/scaffold/basic/helpers/htmlPath.groovy'))
        handlebars.registerHelper('htmlPath', htmlPathHelper)
        def partial = new File('src/main/resources/scaffold/basic/partials/sidebarItem.hbs').text
        def template = handlebars.compileInline(partial)
        def context = [baseUrl: '/test', type: 'file', name: 'index', path: 'index']

        when:
        def output = template.apply(context)

        then:
        output.contains('<a href="/test/index.html">index</a>')
    }

    def "strips non-html extensions"() {
        given:
        def handlebars = new Handlebars()
        def eqHelper = new GroovyShell().evaluate(new File('src/main/resources/scaffold/basic/helpers/eq.groovy'))
        handlebars.registerHelper('eq', eqHelper)
        def htmlPathHelper = new GroovyShell().evaluate(new File('src/main/resources/scaffold/basic/helpers/htmlPath.groovy'))
        handlebars.registerHelper('htmlPath', htmlPathHelper)
        def partial = new File('src/main/resources/scaffold/basic/partials/sidebarItem.hbs').text
        def template = handlebars.compileInline(partial)
        def context = [baseUrl: '/test', type: 'file', name: 'README', path: 'README.md']

        when:
        def output = template.apply(context)

        then:
        output.contains('<a href="/test/README.html">README</a>')
    }

    def "keeps html extension"() {
        given:
        def handlebars = new Handlebars()
        def eqHelper = new GroovyShell().evaluate(new File('src/main/resources/scaffold/basic/helpers/eq.groovy'))
        handlebars.registerHelper('eq', eqHelper)
        def htmlPathHelper = new GroovyShell().evaluate(new File('src/main/resources/scaffold/basic/helpers/htmlPath.groovy'))
        handlebars.registerHelper('htmlPath', htmlPathHelper)
        def partial = new File('src/main/resources/scaffold/basic/partials/sidebarItem.hbs').text
        def template = handlebars.compileInline(partial)
        def context = [baseUrl: '/test', type: 'file', name: 'index', path: 'index.html']

        when:
        def output = template.apply(context)

        then:
        output.contains('<a href="/test/index.html">index</a>')
    }
}
