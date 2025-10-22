apk-dev:
	./gradlew assembleDebug -Dbuildkonfig.flavor=dev

apk-release:
	./gradlew assembleRelease -Dbuildkonfig.flavor=prod

appbundle-dev:
	./gradlew :composeApp:bundleDebug -Dbuildkonfig.flavor=dev

appbundle-release:
	./gradlew :composeApp:bundleRelease -Dbuildkonfig.flavor=prod

gen-privacy:
	cd composeApp && python3 ../script/required_reason_finder.py

config:
	./gradlew generateBuildKonfig