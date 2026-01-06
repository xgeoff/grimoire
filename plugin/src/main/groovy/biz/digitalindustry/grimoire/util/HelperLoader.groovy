package biz.digitalindustry.grimoire.util

import groovy.lang.Binding
import groovy.lang.GroovyShell

class HelperLoader {

    static Map<String, Object> load(File helpersDir, ConfigObject config) {
        if (!helpersDir?.exists() || !helpersDir.isDirectory()) {
            return [:]
        }
        def shell = new GroovyShell(new Binding([config: config]))
        def helpers = [:]
        helpersDir.eachFileMatch(~/.*\.groovy/) { File helper ->
            def helperName = helper.name.replaceFirst(/\.groovy$/, '')
            try {
                def helperValue = shell.evaluate(helper)
                helpers[helperName] = helperValue
            } catch (Exception e) {
                // Log action would happen in task context; leave empty map entry
            }
        }
        helpers
    }
}
