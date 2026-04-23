# GitHub Actions Workflow Instructions

Due to security restrictions, I was unable to push the GitHub Actions workflow file directly to your repository. To enable automated production builds, please manually create a file at `.github/workflows/build.yml` in your repository with the following content:

```yaml
name: Build Production App

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew assembleRelease

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: app-release
        path: app/build/outputs/apk/release/*.apk

    - name: Build Bundle (AAB)
      run: ./gradlew bundleRelease

    - name: Upload AAB
      uses: actions/upload-artifact@v4
      with:
        name: app-bundle
        path: app/build/outputs/bundle/release/*.aab
```

This workflow will automatically build a production APK and AAB whenever you push changes to the `main` branch. You can find the build artifacts in the "Actions" tab of your GitHub repository.
