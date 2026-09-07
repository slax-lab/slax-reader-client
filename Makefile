.PHONY: apk-dev apk-release appbundle-dev appbundle-release gen-privacy config bridge adb-proxy test test-ui test-build

apk-dev:
	$(MAKE) test
	./gradlew assembleDebug -Pbuildkonfig.flavor=dev

apk-release:
	$(MAKE) test
	make rn && ./gradlew :composeApp:bundleAndroidReleaseJs && ./gradlew assembleRelease -Pbuildkonfig.flavor=release

appbundle-dev:
	$(MAKE) test
	./gradlew :composeApp:bundleDebug -Pbuildkonfig.flavor=dev

appbundle-release:
	$(MAKE) test
	./gradlew :composeApp:bundleRelease -Pbuildkonfig.flavor=release

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

test:
	./gradlew :composeApp:testDebugUnitTest :composeApp:testReleaseUnitTest :composeApp:allTests

test-ui:
	./gradlew :composeApp:connectedDebugAndroidTest

test-build:
	./gradlew :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest
