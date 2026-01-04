# Publishing to Maven Central (Central Portal API)

This project uses the Central Portal “bundle upload” flow instead of the legacy Nexus staging upload. Follow these steps to sign artifacts and publish.

## Prerequisites
- GPG private key with passphrase.
- Sonatype Central Portal access token (from https://central.sonatype.com → Account → Access Tokens).
- JDK 21 (wrapper downloads Gradle 8.6 automatically).

## Configure signing
Put these in `~/.gradle/gradle.properties` (or export as `ORG_GRADLE_PROJECT_*`):
```
signing.password=<your key passphrase>
signing.secretKeyRingFileBase64=<base64 of your armored private key>
# optional; we let Gradle derive it, but you can set a valid 8/16 hex keyId if you want:
# signing.keyId=ABCDEFGH
```

Generate `signing.secretKeyRingFileBase64` (macOS/BSD):
```
KEYID=<your key id>
gpg --armor --export-secret-keys "$KEYID" > /tmp/secret-key.asc
base64 -i /tmp/secret-key.asc | tr -d '\n'
shred -u /tmp/secret-key.asc
```
Paste that one-line base64 into `signing.secretKeyRingFileBase64`.

Verify Gradle sees the props:
```
./gradlew checkSigningProps
./gradlew validateSigningKey   # ensures the armor header is present
./gradlew signMavenJavaPublication
```

## Configure Central Portal token
Add to `~/.gradle/gradle.properties` (or export as env vars):
```
sonatypePublisherTokenName=<portal token name>
sonatypePublisherTokenPassword=<portal token password>
# optional:
# sonatypePublisherPublishingType=USER_MANAGED   # or AUTOMATIC
# sonatypePublisherBundleName=grimoire-0.1.0
# sonatypePublisherUploadUrl=https://central.sonatype.com/api/v1/publisher/upload
```

## Build and upload the bundle
1) Create the bundle zip:
```
./gradlew centralBundle
```
This stages only the `mavenJava` publication (artifactId `grimoire`) into `build/central-bundle-repo` and zips to `build/distributions/central-bundle-<project>-<version>.zip`. The plugin marker publication is excluded from the bundle to avoid duplicate coordinates.

> **Note:** Publications now publish as `biz.digitalindustry:grimoire:<version>`, and both the sources and javadoc jars are included automatically, satisfying Central’s requirements.

2) Upload via the Portal Publisher API:
```
./gradlew uploadCentralBundle
```
The task POSTs the bundle to Central and prints the returned `deploymentId`.

## Publish or monitor
- Check status: `POST https://central.sonatype.com/api/v1/publisher/status?id=<deploymentId>` (or view in Portal UI).
- If `publishingType=USER_MANAGED`, publish via Portal UI or `POST /api/v1/publisher/deployment/<deploymentId>`.
- Drop a failed/abandoned deployment: `DELETE /api/v1/publisher/deployment/<deploymentId>`.

## Notes and pitfalls
- The signing block ignores `signing.keyId` if it isn’t valid hex and lets Gradle derive the key ID from the private key.
- If signing fails with “Could not read PGP secret key,” recheck that the base64 decodes to an armored private key (`-----BEGIN PGP PRIVATE KEY BLOCK-----`) and the passphrase matches.
- For local installs without signing, use `./gradlew -x signMavenJavaPublication publishToMavenLocal`.
