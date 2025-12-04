apk-dev:
	./gradlew assembleDebug -Pbuildkonfig.flavor=dev

apk-release:
	./gradlew assembleRelease -Pbuildkonfig.flavor=release

appbundle-dev:
	./gradlew :composeApp:bundleDebug -Pbuildkonfig.flavor=dev

appbundle-release:
	./gradlew :composeApp:bundleRelease -Pbuildkonfig.flavor=release

gen-privacy:
	cd composeApp && python3 ../script/required_reason_finder.py

config:
	./gradlew generateBuildKonfig -Pbuildkonfig.flavor=dev

bridge:
	./gradlew :composeApp:swiftklibStoreKitWrapperIosArm64
	./gradlew :composeApp:swiftklibStoreKitWrapperIosSimulatorArm64
	./gradlew :composeApp:swiftklibStoreKitWrapperIosX64