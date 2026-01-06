package biz.digitalindustry.grimoire.util

import groovy.util.ConfigObject
import spock.lang.Specification

class HelperLoaderSpec extends Specification {

    File helpersDir

    def setup() {
        helpersDir = File.createTempDir("helpers", "grimoire")
    }

    def cleanup() {
        helpersDir?.deleteDir()
    }

    def "loads helper scripts into the context map"() {
        given:
        def helperFile = new File(helpersDir, "dateFormat.groovy")
        helperFile.text = """
return { String isoDate ->
    Date.parse('yyyy-MM-dd', isoDate).format('MMM d, yyyy')
}
"""

        when:
        def helpers = HelperLoader.load(helpersDir, new ConfigObject())

        then:
        helpers.dateFormat instanceof Closure
        helpers.dateFormat('2025-12-25') == 'Dec 25, 2025'
    }
}
