apk-dev:
	./gradlew assembleDebug -Dbuildkonfig.flavor=dev

apk-release:
	./gradlew assembleRelease -Dbuildkonfig.flavor=release

appbundle-dev:
	./gradlew :composeApp:bundleDebug -Dbuildkonfig.flavor=dev

appbundle-release:
	./gradlew :composeApp:bundleRelease -Dbuildkonfig.flavor=release

gen-privacy:
	cd composeApp && python3 ../script/required_reason_finder.py

config:
	./gradlew generateBuildKonfig -Dbuildkonfig.flavor=dev