package biz.digitalindustry.grimoire

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Configuration extension for the Grimoire plugin.
 * Allows users to configure the site generation process in their build.gradle.
 */
abstract class SiteGenExtension {

    /**
     * The main configuration file for the Grimoire site.
     * Defaults to 'config.grim' in the project root.
     */
    abstract RegularFileProperty getConfigFile()

    @Inject
    SiteGenExtension(ObjectFactory objects) {
        // Set the default location for the config file.
        // Users can override this in their build script.
        configFile.convention(objects.fileProperty().fileValue(new File("config.grim")))
    }
}