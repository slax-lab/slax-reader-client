#!/bin/bash
set -e

export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export BUILD_FLAVOR=release

./gradlew :composeApp:syncXcodeVersionConfig -Pbuildkonfig.flavor=$BUILD_FLAVOR

./gradlew :composeApp:syncFirebaseIOS -Pbuildkonfig.flavor=$BUILD_FLAVOR

xcodebuild clean \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Release

xcodebuild archive \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Release \
  -archivePath build/iosApp.xcarchive \
  -destination 'generic/platform=iOS' \
  BUILD_FLAVOR=$BUILD_FLAVOR

xcodebuild -exportArchive \
  -archivePath build/iosApp.xcarchive \
  -exportPath build/ipa \
  -exportOptionsPlist iosApp/iosApp/ExportOptions.plist

echo "ipa PATH: build/ipa/Slax Reader.ipa"
echo "upload to connect"

cp firebase/GoogleService-Info.dev.plist iosApp/iosApp/GoogleService-Info.plist
rm iosApp/iosApp/GoogleService-Info.plist.backup

iTMSTransporter -m upload \
  -u $APPLE_UPLOAD_EMAIL \
  -p $APPLE_UPLOAD_PASSWORD \
  -assetFile  ./build/ipa/Slax\ Reader.ipa \
  -asc_provider 8G3A932HJG \
  -v informational