# Fix KSP Error: KSTypeArgument.type should not have been null

The project is failing to build with the error `[ksp] java.lang.IllegalStateException: KSTypeArgument.type should not have been null, please file a bug. STAR null` during the `:app:kspDebugKotlin` task. This is a known issue in KSP 2 when processing star projections in certain libraries like Room or Hilt, especially with newer Kotlin versions.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///C:/Users/apollo/AndroidStudioProjects/Finebulary/gradle.properties)
Disable KSP2 to work around the internal KSP error. This reverts KSP to its stable KSP1 implementation while keeping compatibility with Kotlin 2.1.0.

#### [MODIFY] [libs.versions.toml](file:///C:/Users/apollo/AndroidStudioProjects/Finebulary/gradle/libs.versions.toml)
Update Room and Hilt to more recent versions that have better compatibility with Kotlin 2.1.0 and KSP.
*   Update Hilt to `2.54` (or latest stable compatible).
*   Ensure Room version is correct (it was set to `2.8.5` which might be a preview or an experimental version, I will verify if I should stick to it or move to a known stable one). Actually, `version_lookup` said `2.8.5` is stable, so I will keep it but update Hilt to match the era.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:kspDebugKotlin` to verify the KSP task completes successfully.
- Run `./gradlew assembleDebug` to ensure the entire project builds.

### Manual Verification
- None required as this is a build-time fix.
