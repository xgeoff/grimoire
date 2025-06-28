import spock.lang.*
import java.nio.file.*

class FileTestSpec extends Specification {

    Path tempDir

    def setup() {
        tempDir = Files.createTempDirectory("test-output-")
        println "Created temp directory at $tempDir"
    }

    def cleanup() {
        deleteRecursively(tempDir)
        println "Deleted temp directory at $tempDir"
    }

    def "test using temp directory"() {
        given:
        Path testFile = tempDir.resolve("example.txt")
        Files.write(testFile, "hello world".bytes)

        expect:
        Files.exists(testFile)
    }

    private static void deleteRecursively(Path path) {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder()) // delete children first
                    .forEach { Files.deleteIfExists(it) }
        }
    }
}
