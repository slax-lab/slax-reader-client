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

rn:
	 ./gradlew :composeApp:generateCodegenArtifactsFromSchema
	 ./gradlew kspCommonMainKotlinMetadata
	 mkdir -p build/generated/autolinking && cd react-native && npx react-native config > ../build/generated/autolinking/autolinking.json

rn-bundle:
	cd react-native && npm run bundle:ios && npm run bundle:android