apk-dev:
	./gradlew assembleDebug -Pbuildkonfig.flavor=dev

apk-release:
	make rn && ./gradlew :composeApp:bundleAndroidReleaseJs && ./gradlew assembleRelease -Pbuildkonfig.flavor=release

appbundle-dev:
	./gradlew :composeApp:bundleDebug -Pbuildkonfig.flavor=dev

appbundle-release:
	make rn && ./gradlew :composeApp:bundleAndroidReleaseJs && ./gradlew :composeApp:bundleRelease -Pbuildkonfig.flavor=release

gen-privacy:
	cd composeApp && python3 ../script/required_reason_finder.py

config:
	./gradlew generateBuildKonfig -Pbuildkonfig.flavor=dev

bridge:
	./gradlew :composeApp:swiftklibStoreKitWrapperIosArm64
	./gradlew :composeApp:swiftklibStoreKitWrapperIosSimulatorArm64
	./gradlew :composeApp:swiftklibStoreKitWrapperIosX64

adb-proxy:
	 ~/Library/Android/sdk/platform-tools/adb reverse tcp:8081 tcp:8081

rn-ln:
	cd reactNativeApp/ios/Pods/hermes-engine/destroot/Library/Frameworks/universal/ && ln -s hermes.xcframework hermesvm.xcframework && ls -la

CURRENT := ./reactNativeApp

rn-debug:
	cd $(CURRENT) && npx expo prebuild --platform ios --clean
	cd reactNativeApp/ios/Pods/hermes-engine/destroot/Library/Frameworks/universal/ && ln -s hermes.xcframework hermesvm.xcframework
	cd $(CURRENT) && sed -i '' '/ExpoAppDelegateWrapper/d' ios/ReactNativeAppTarget/ReactNativeHostManager.swift
	cd $(CURRENT) && npx expo-brownfield build-ios --debug
	cd $(CURRENT) && npx expo-brownfield build-android --all --repository MavenLocal
	bash ./script/fix_react_native_target_path.sh

rn-build:
	cd $(CURRENT) && npx expo-brownfield build-ios --release
	cd $(CURRENT) && npx expo-brownfield build-android --all --repository MavenLocal
	bash ./script/fix_react_native_target_path.sh

rn-bundle:
	./gradlew :composeApp:bundleAndroidReleaseJs
	./gradlew :composeApp:bundleIOSReleaseJs