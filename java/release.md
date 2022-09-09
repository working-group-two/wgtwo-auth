# Release to Maven central

## Optional: Set version
```shell
./mvnw build-helper:parse-version versions:set -DnewVersion={REPLACE ME} versions:commit
```

## Release/deploy
1. `./mvnw build-helper:parse-version release:prepare -B`
2. `./mvnw release:perform`
3. Access [Sonatype's repo manager](https://s01.oss.sonatype.org/) and confirm the artifacts for release.

## Troubleshooting
If you get an error about "gpg: signing failed: Inappropriate ioctl for device", run these commands in your shell and try again:
```shell
GPG_TTY=$(tty)
export GPG_TTY
```
