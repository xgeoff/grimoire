# Maven Central Publish Plugin Template

Use this template as the reference payload when you spin up a new Codex instance for building a reusable Maven-Central pipeline plugin. The template includes every task, helper, and configuration detail you need to wire into a fresh Gradle project so you can publish *any* Maven publication via Sonatype’s Portal API.

## Project Layout
```
publishing-plugin/
├── README.md                         # This file
├── gradle/                           # wrapper (optional)
├── build.gradle                      # applies the plugin
├── settings.gradle
└── src/
    └── main/groovy/
        └── biz/digitalindustry/publish/
            ├── CentralPublisherPlugin.groovy
            ├── CentralPublisherExtension.groovy
            └── CentralBundleTask.groovy
```

## Plugin Responsibilities
1. Provide an extension `centralPublisher` with:
   * `tokenName` / `tokenPassword`
   * `bundleName` (defaults to `project.name` + `version`)
   * `uploadUrl` (default `https://central.sonatype.com/api/v1/publisher/upload`)
   * `publishingType` (`USER_MANAGED` default)
   * `publications` collection to publish (default to `project.publishing.publications.matching { it.name == 'mavenJava' }`)
2. Add tasks:
   * `centralBundle`: stages selected publications into `build/central-bundle-repo` and zips them.
   * `uploadCentralBundle`: POSTs the resulting zip to Sonatype using `HttpClient`, adds bundle/query params, and reports the returned deployment ID.

## Sample Task Logic (Groovy pseudo-code)
```groovy
class CentralBundleTask extends DefaultTask {
    @OutputDirectory
    DirectoryProperty bundleDir = project.objects.directoryProperty()

    @InputFiles
    ConfigurableFileCollection publicationsFiles

    @OutputFile
    RegularFileProperty archiveFile = project.objects.fileProperty()

    @TaskAction
    void bundle() {
        bundleDir.get().asFile.deleteDir()
        publicationsFiles.each { File pubDir -> copy { from pubDir; into bundleDir } }
        ant.zip(destfile: archiveFile.get().asFile) { fileset(dir: bundleDir) }
    }
}
```

The real task should use the `publish<Publication>PublicationTo<Repo>` tasks instead of copying files manually, but this snippet shows the idea.

## Upload Task Sketch
```groovy
class UploadCentralBundleTask extends DefaultTask {
    @Input
    String tokenName
    @Input
    String tokenPassword
    @Input
    String uploadUrl
    @InputFile
    RegularFileProperty bundle

    @TaskAction
    void upload() {
        def auth = Base64.encoder.encodeToString("$tokenName:$tokenPassword".bytes)
        def request = HttpRequest.newBuilder()
            .uri(new URI("$uploadUrl?publishingType=$publishingType&name=$bundleName"))
            .header('Authorization', "Bearer $auth")
            .header('Content-Type', "multipart/form-data; boundary=...")
            .POST(...)
            .build()
        def response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString())
        if (response.statusCode() >= 400) throw new GradleException("Upload failed: ${response.body()}")
        logger.lifecycle("Deployment ID: ${response.body().trim()}")
    }
}
```

## Integration Notes
- Apply the plugin to any project with `maven-publish`/`signing`.
- Configure the extension in `build.gradle`:
  ```groovy
  centralPublisher {
      tokenName = findProperty('sonatypePublisherTokenName')
      tokenPassword = findProperty('sonatypePublisherTokenPassword')
      bundleName = "my-artifact-${version}"
  }
  ```
- Declare dependencies: none beyond the JDK.
- Tests can mirror our `HelperLoaderSpec` pattern but focus on ensuring the bundle zips targeted publications and the upload task constructs the correct HTTP request (mock `HttpClient` via Spock).

## Deliverables for Codex
When you hand this to another Codex instance, include:
1. This README (for context).
2. Real Groovy classes implementing `CentralPublisherPlugin`, `CentralBundleTask`, and `UploadCentralBundleTask`.
3. The extension class with all configurable properties.
4. A sample `build.gradle` showing how to apply and configure the plugin.
5. Sample integration tests that confirm the plugin publishes only the `grimoire`/`mavenJava` publication and uploads a bundle with a deployment ID.

With those pieces, the other Codex agent can generate a reusable plugin that matches our current release workflow. Let me know if you’d like me to expand this template into actual source files here before handing it off.	
