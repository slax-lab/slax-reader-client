apk-test:
	./gradlew assembleDebug

apk-beta:
	./gradlew assembleRelease

apk-release:
	./gradlew assembleRelease

gen-privacy:
	cd composeApp && python3 ../script/required_reason_finder.py

config:
	./gradlew generateBuildKonfig