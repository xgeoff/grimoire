import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification

class ScaffoldSpec extends Specification {
    @Shared
    File testProjectDir

    def setup() {
        // Use a predictable directory for the Gradle project that will run the task
        def projectRoot = new File(".").getCanonicalFile()
        testProjectDir = new File(projectRoot, "grimoire-scaffold-test")

        // Clean up from previous runs
        if (testProjectDir.exists()) {
            assert testProjectDir.deleteDir()
        }
        assert testProjectDir.mkdirs()

        // Create the build script in the test project directory
        new File(testProjectDir, 'build.gradle') << """
            plugins {
                id 'biz.digitalindustry.grimoire'
            }
        """
        new File(testProjectDir, 'settings.gradle') << """
            rootProject.name = 'grimoire-scaffold-test'
        """
    }

    def "scaffolds site into current directory by default"() {
        when: "The grim-scaffold task is run without a destination"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("grim-init", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "The scaffold structure is created in the project root"
        new File(testProjectDir, "pages/index.html").exists()
        new File(testProjectDir, "layouts/default.hbs").exists()
        new File(testProjectDir, "assets/style.css").exists()

        and: "The config file points to the current directory"
        def configFile = new File(testProjectDir, "config.grim")
        assert configFile.exists()
        assert configFile.text.contains('sourceDir = "."')
    }

    def "scaffolds site into specified directory"() {
        given: "A target directory name"
        def targetDirName = "my-basic-site"

        when: "The grim-scaffold task is run with a destination"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("grim-init", "--dest=${targetDirName}", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "The basic scaffold structure is created in the target subdirectory"
        def scaffoldRoot = new File(testProjectDir, targetDirName)
        assert scaffoldRoot.isDirectory()
        assert new File(scaffoldRoot, "pages/index.html").exists()
        assert new File(scaffoldRoot, "layouts/default.hbs").exists()
        assert new File(scaffoldRoot, "assets/style.css").exists()

        and: "The config file has the correct source directory"
        def configFile = new File(testProjectDir, "config.grim")
        assert configFile.exists()
        assert configFile.text.contains("sourceDir = \"${targetDirName}\"")
    }

    def "fails when a scaffold file already exists"() {
        given: "An existing scaffold file"
        def pagesDir = new File(testProjectDir, "pages")
        pagesDir.mkdirs()
        new File(pagesDir, "index.html") << "hello"

        when: "grim-init is run with --force to bypass non-empty check"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("grim-init", "--force", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail()

        then: "The build fails to avoid overwriting existing files"
        result.output.contains("Cannot overwrite existing file")
    }

    def "fails in non-empty directory without force"() {
        given: "An existing non-scaffold file in the destination"
        def existing = new File(testProjectDir, "existing.txt")
        existing.text = "original"

        when: "grim-init is run without --force"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("grim-init", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail()

        then: "The build fails and the original file remains untouched"
        result.output.contains("not empty")
        existing.text == "original"
        !new File(testProjectDir, "pages").exists()
    }

    def "scaffolds in non-empty directory with force"() {
        given: "An existing non-scaffold file in the destination"
        def existing = new File(testProjectDir, "existing.txt")
        existing.text = "original"

        when: "grim-init is run with --force"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("grim-init", "--force", "--stacktrace")
                .withPluginClasspath()
                .build()

        then: "The scaffold structure is created and the existing file is preserved"
        new File(testProjectDir, "pages/index.html").exists()
        new File(testProjectDir, "layouts/default.hbs").exists()
        new File(testProjectDir, "assets/style.css").exists()
        existing.text == "original"
        and:
        def configFile = new File(testProjectDir, "config.grim")
        assert configFile.exists()
        assert configFile.text.contains('sourceDir = "."')
    }

    def "scaffolds site from packaged plugin jar"() {
        given: "The plugin is packaged as a JAR and a target directory name"
        def pluginProjectDir = new File('.').getCanonicalFile()
        // Package the compiled classes and resources into a temporary JAR
        def jarDir = new File(pluginProjectDir, 'build/tmp/test-plugin')
        if (jarDir.exists()) { jarDir.deleteDir() }
        jarDir.mkdirs()
        def pluginJar = new File(jarDir, 'plugin.jar')
        new AntBuilder().jar(destfile: pluginJar) {
            fileset(dir: new File(pluginProjectDir, 'build/classes/groovy/main'))
            fileset(dir: new File(pluginProjectDir, 'build/resources/main'))
        }
        assert pluginJar.exists()

        def targetDirName = 'jar-basic-site'

        when: "grim-init is run using the packaged plugin"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("grim-init", "--dest=${targetDirName}", "--stacktrace")
                .withPluginClasspath([pluginJar])
                .build()

        then: "The scaffold structure is created correctly"
        def scaffoldRoot = new File(testProjectDir, targetDirName)
        assert scaffoldRoot.isDirectory()
        assert new File(scaffoldRoot, "pages/index.html").exists()
        assert new File(scaffoldRoot, "layouts/default.hbs").exists()
        assert new File(scaffoldRoot, "assets/style.css").exists()

        and: "The config file has the default source directory"
        def configFile = new File(testProjectDir, "config.grim")
        assert configFile.exists()
        assert configFile.text.contains("sourceDir = \"${targetDirName}\"")
    }

    def "scaffolds site from built plugin jar"() {
        given: "The plugin jar exists in the build/libs directory and a target directory name"
        def pluginProjectDir = new File('.').getCanonicalFile()
        def libsDir = new File(pluginProjectDir, 'build/libs')
        libsDir.mkdirs()
        def pluginJar = new File(libsDir, 'plugin.jar')
        new AntBuilder().jar(destfile: pluginJar) {
            fileset(dir: new File(pluginProjectDir, 'build/classes/groovy/main'))
            fileset(dir: new File(pluginProjectDir, 'build/resources/main'))
        }
        assert pluginJar.exists()

        def targetDirName = 'built-jar-site'

        when: "grim-init is run using the built plugin jar"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("grim-init", "--dest=${targetDirName}", "--stacktrace")
                .withPluginClasspath([pluginJar])
                .build()

        then: "The scaffold structure is created correctly"
        def scaffoldRoot = new File(testProjectDir, targetDirName)
        assert scaffoldRoot.isDirectory()
        assert new File(scaffoldRoot, "pages/index.html").exists()
        assert new File(scaffoldRoot, "layouts/default.hbs").exists()
        assert new File(scaffoldRoot, "assets/style.css").exists()

        and: "The config file has the correct source directory"
        def configFile = new File(testProjectDir, "config.grim")
        assert configFile.exists()
        assert configFile.text.contains("sourceDir = \"${targetDirName}\"")
    }

    def "fails when an invalid scaffold type is provided"() {
        when: "The grim-scaffold task is run with a non-existent type"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                // Provide a custom destination to isolate from any default directory
                .withArguments("grim-init", "--dest=test-dest", "--type=non-existent", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail()

        then: "The build fails with a helpful error message about the type"
        result.output.contains("Could not find the scaffold template for type 'non-existent'")
    }

    // This method will run ONCE, after the last test method in this class has finished.
    def cleanupSpec() {
        println "Cleaning up test directory: ${testProjectDir.absolutePath}"
        if (testProjectDir != null && testProjectDir.exists()) {
            testProjectDir.deleteDir()
        }
    }
}
