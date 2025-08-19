package biz.digitalindustry.grimoire.helper

import com.github.jknack.handlebars.*
import spock.lang.Specification

class PagesHelperSpec extends Specification {
    def "includes markdown and html files"() {
        given:
        def tmpDir = File.createTempDir()
        new File(tmpDir, 'a.md').text = ''
        new File(tmpDir, 'b.html').text = ''
        new File(tmpDir, 'c.htm').text = ''

        def helper = new GroovyShell().evaluate(new File('src/main/resources/scaffold/basic/helpers/pages.groovy'))

        def context = Context.newBuilder([pagesDir: tmpDir]).build()
        def options = new Options.Builder(new Handlebars(), '', TagType.VAR, context, Template.EMPTY).build()

        when:
        def items = helper.apply(null, options)

        then:
        items.collect { it.path }.toSet() == ['a', 'b', 'c'] as Set
    }
}
