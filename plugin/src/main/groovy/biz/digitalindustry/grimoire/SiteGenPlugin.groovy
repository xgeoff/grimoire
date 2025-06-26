package biz.digitalindustry.sitegen

import org.gradle.api.Plugin
import org.gradle.api.Project

class SiteGenPlugin implements Plugin<Project> {
    void apply(Project project) {

        project.tasks.register('grim', SiteGenTask) {
            it.group = 'Site'
            it.description = 'Generates static HTML content from templates'
        }
    }
}
