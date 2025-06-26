import spock.lang.Specification

class SiteGenPluginSpec extends Specification {

    File testDir

    def setup() {
        def fixture = new File("src/test/resources/test-projects/basic-site")
        testDir = Files.createTempDirectory("grimoire-test").toFile()
        copyProject(fixture, testDir)
    }

    def "renders site from fixture project"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testDir)
                .withArguments("generateSite")
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("Generated page")
        new File(testDir, "build/site/index.html").text.contains("Hello")
    }

    void copyProject(File source, File dest) {
        source.eachFileRecurse { file ->
            def relPath = file.path - source.path
            def target = new File(dest, relPath)
            if (file.isDirectory()) {
                target.mkdirs()
            } else {
                target.parentFile.mkdirs()
                file.withInputStream { i -> target.withOutputStream { o -> o << i } }
            }
        }
    }
}
