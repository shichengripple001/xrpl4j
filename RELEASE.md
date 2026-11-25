# Release Guide

Follow this guide when releasing xrpl4j packages.

Releases are built, signed and published to [Maven Central](https://central.sonatype.com/) by the
**Release Pipeline** GitHub Actions workflow (`.github/workflows/release.yml`). You do not deploy from your own
machine. A [manual fallback](#manual-release-fallback) is kept at the end of this guide.

## How the pipeline works

You commit the release version on a release branch, then start the workflow against that branch.

1. **Validate**: reads the version from the root `pom.xml` of the release branch and stops the run if it is wrong
   (see [Version rules](#version-rules)).
2. **CI tests**: runs the full CI suite on the release branch. For pre-releases (rc/alpha/beta) the live-network jobs
   (Devnet, Testnet, Clio) and the macOS/Windows jobs are skipped.
3. **Build, SBOM and scan**: builds the jars once, attests their build provenance, generates a CycloneDX SBOM and scans
   it for CRITICAL/HIGH vulnerabilities. A GitHub issue labelled `security` is opened if any are found. Stable releases
   also open a draft PR from the release branch to `main`.
4. **Reviews**: a dev-team approval (`first-review` environment), then the approval in `beta-release` (pre-releases) or
   `official-release` (stable releases) before anything is published.
5. **Publish**: verifies the provenance of the built files, signs them with the release GPG key and with Sigstore,
   uploads one bundle to Maven Central and waits until Central reports it as published (it is published automatically
   after Central's validation; no click in the Central Portal is needed).
6. **GitHub release**: creates the tag `v[version]` on the release branch with generated release notes, the jars and the
   SBOM attached. Pre-releases are marked as pre-release; stable releases are marked as latest.

Published versions on Maven Central are immutable. If a run fails before step 5 nothing has been published and you can
start a new run. **Runs cannot be re-run** (the publish job refuses a second attempt); always start a new run from the
Actions tab instead.

## Version rules

- The release version is set **on the release branch** and **committed** there, because the workflow reads it from the
  pom. Do not release a `-SNAPSHOT` version.
- After a stable release the release branch is merged into `main` (see
  [Merging a stable release into main](#merging-a-stable-release-into-main)), so `main` carries the version of the
  release branch from then on. The workflow reads the version from the release branch, never from `main`.
- **Stable releases**: the version must be exactly `[major].[minor].[patch]` (for example `6.1.0`) and the branch name
  must start with `release-`, `release/`, `releases-` or `releases/` (for example `releases/v6.1`).
- **Pre-releases**: a version that contains `rc`, `alpha` or `beta` (for example `7.0.0-rc.6`) is treated as a
  pre-release. It can be released from any branch.
- The tag `v[version]` must not already exist, and the branch must not contain the internal Artifactory host name.

## Starting the workflow

1. In GitHub, open **Actions** → **Release Pipeline** → **Run workflow**.
2. Choose the `main` branch in "Use workflow from". The workflow refuses to start from any other branch.
3. Enter the `release_branch` (for example `releases/v6.1`).
4. Leave `dry_run` unchecked to publish. Check it to rehearse everything except the upload to Maven Central and the
   GitHub release.
5. Approve the review steps when asked. The run name shows the version and whether it is a `beta` or `official`
   release.

## Major/Minor Releases

Follow these steps to create a new major or minor release.

1. Checkout `main` and pull the latest changes.
   ```
   git checkout main
   git pull
   ```
2. Create a new release branch `releases/v[major].[minor]` from `main`.
   ```
   git checkout -b releases/v[major].[minor]
   ```
   > Example: `git checkout -b releases/v6.1`

3. Set the release version, then **commit and push it**.
   ```
   mvn versions:set -DnewVersion=[major].[minor].0
   git commit -am "Set version to [major].[minor].0"
   git push -u origin releases/v[major].[minor]
   ```
   > Example:
   > ```
   > mvn versions:set -DnewVersion=6.1.0
   > git commit -am "Set version to 6.1.0"
   > git push -u origin releases/v6.1
   > ```

4. Verify that integration tests executing on `Devnet` pass in GitHub Actions. Integration tests on `Testnet`
   might fail as the amendment may not yet be available there.
5. Run the Release Pipeline workflow for `releases/v[major].[minor]` as described in
   [Starting the workflow](#starting-the-workflow), and approve the review steps.
6. When the run has finished, bump the release branch to the next snapshot, then commit and push.
   ```
   mvn versions:set -DnewVersion=[major].[minor].[patch+1]-SNAPSHOT
   git commit -am "Bump version to [major].[minor].[patch+1]-SNAPSHOT"
   git push
   ```
   > Example: Bump from `6.1.0` to `6.1.1-SNAPSHOT`.
   > ```
   > mvn versions:set -DnewVersion=6.1.1-SNAPSHOT
   > git commit -am "Bump version to 6.1.1-SNAPSHOT"
   > git push
   > ```

7. Review the generated release notes on the GitHub release `v[major].[minor].0` and edit them if needed.
8. Merge the draft PR into `main` as described in
   [Merging a stable release into main](#merging-a-stable-release-into-main).

## Patch Releases

Follow these steps to release a patch from an existing `releases/v[major].[minor]` branch. Bug fixes should
already be merged or cherry-picked into this branch.

1. Checkout the existing `releases/v[major].[minor]` branch and pull the latest changes. The branch should
   already be at the next `[major].[minor].[patch]-SNAPSHOT` version from the previous release.
   ```
   git checkout releases/v[major].[minor]
   git pull
   ```
   > Example: `git checkout releases/v6.1 && git pull`

2. Merge or cherry-pick the necessary bug fixes into the release branch.
3. Verify that integration tests executing on `Devnet` pass in GitHub Actions.
4. Set the release version, then **commit and push it**.
   ```
   mvn versions:set -DnewVersion=[major].[minor].[patch]
   git commit -am "Set version to [major].[minor].[patch]"
   git push
   ```
   > Example:
   > ```
   > mvn versions:set -DnewVersion=6.1.1
   > git commit -am "Set version to 6.1.1"
   > git push
   > ```

5. Run the Release Pipeline workflow for `releases/v[major].[minor]` as described in
   [Starting the workflow](#starting-the-workflow), and approve the review steps.
6. When the run has finished, bump the version from `[major].[minor].[patch]` to
   `[major].[minor].[patch+1]-SNAPSHOT`, then commit and push.
   ```
   mvn versions:set -DnewVersion=[major].[minor].[patch+1]-SNAPSHOT
   git commit -am "Bump version to [major].[minor].[patch+1]-SNAPSHOT"
   git push
   ```
   > Example: Bump from `6.1.1` to `6.1.2-SNAPSHOT`.
   > ```
   > mvn versions:set -DnewVersion=6.1.2-SNAPSHOT
   > git commit -am "Bump version to 6.1.2-SNAPSHOT"
   > git push
   > ```

7. Review the generated release notes on the GitHub release `v[major].[minor].[patch]`.
8. Merge the draft PR into `main` as described in
   [Merging a stable release into main](#merging-a-stable-release-into-main).

## Merging a stable release into main

After a stable release, the pipeline has opened a draft PR from the release branch to `main`. Merge it so that `main`
contains the release version and the fixes made on the release branch.

1. Finish the release first, including the bump to the next `-SNAPSHOT` version on the release branch (step 6 above).
   The PR then brings `main` to that next snapshot version.
2. Mark the draft PR ready for review, get it approved and merge it. Resolve any conflicts in `pom.xml` files in favour
   of the release branch version.
3. Pre-releases (rc/alpha/beta) do not open a PR and are not merged into `main` unless their branch is merged as normal.

## Release Candidates

The steps for a release candidate are the same as a major/minor or patch release above. The only difference
is that the version uses an `-rc.N` suffix instead of a final version number (e.g., `6.1.0-rc.1` instead
of `6.1.0`). The pipeline treats it as a pre-release (see [Version rules](#version-rules)).

If the changes have not yet been merged into `main`, release the candidate directly from the feature
branch instead of a release branch: set the `-rc.N` version on that branch, commit and push it, and enter that
branch as the `release_branch`. The workflow itself is still started from `main`.

## Verifying a published release

Every file on Maven Central has a GPG signature (`.asc`) and a Sigstore bundle (`.sigstore.json`). The Sigstore bundle
records which GitHub Actions run signed the file.

```
pip install sigstore
sigstore verify github \
  --cert-identity "https://github.com/XRPLF/xrpl4j/.github/workflows/release.yml@refs/heads/main" \
  --repository XRPLF/xrpl4j \
  xrpl4j-core-[version].jar
```

Download the jar and its `.sigstore.json` from Maven Central into the same folder first. Build provenance for the same
files can also be checked with `gh attestation verify xrpl4j-core-[version].jar --repo XRPLF/xrpl4j`.

## Repository settings used by the pipeline

Maintainers configure these in the GitHub repository settings.

| Environment | Purpose |
| --- | --- |
| `first-review` | Required reviewers for the dev-team approval before publishing |
| `beta-release` | Required reviewers and secrets for pre-releases |
| `official-release` | Required reviewers and secrets for stable releases |

Secrets in `beta-release` and `official-release`: `MAVEN_GPG_PRIVATE_KEY`, `MAVEN_GPG_PASSPHRASE`,
`MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` (a Central Portal user token for the `org.xrpl` namespace).
Optional repository secrets: `OWASP_TOKEN` (uploads the SBOM to Dependency-Track) and `SLACK_TOKEN` (review
notifications). The repository needs a `security` label for the vulnerability issues.

## Manual Release (fallback)

Use this only if the pipeline cannot be used, for example when GitHub Actions is unavailable. A manual release has none
of the pipeline's checks: no provenance, no Sigstore bundle and no approval steps. **Never run a manual release and the
pipeline for the same version.**

You need a Central Portal user token for `org.xrpl` in `~/.m2/settings.xml` (server id `central`) and the release GPG
key.

1. Create the release branch and verify the tests as described above (steps 1-4 of the release type).
2. Create a tag `v[version]` and push it to the remote.
   ```
   git tag v[version]
   git push origin v[version]
   ```
3. Set the non-SNAPSHOT version to deploy and publish artifacts to
   [Maven Central](https://central.sonatype.com/). This will prompt you to sign the artifacts with your
   private key. **Do not commit this version change.**
   ```
   mvn versions:set -DnewVersion=[version]
   mvn clean deploy -DskipTests -P release
   ```
4. Approve the deployment on [Maven Central](https://central.sonatype.com/).
5. Discard the local version change, bump the release branch to the next `-SNAPSHOT` version, then commit and push.
6. Generate release notes on GitHub using the `v[version]` tag.
