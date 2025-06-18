package biz.digitalindustry.sitegen

import org.gradle.api.Plugin
import org.gradle.api.Project

class SiteGenPlugin implements Plugin<Project> {
    void apply(Project project) {
        def extension = project.extensions.create('sitegen', SiteGenExtension)

        project.tasks.register('generateSite', SiteGenTask) {
            it.group = 'Site'
            it.description = 'Generates static HTML content from templates'
            it.extension = extension
        }
    }
}
