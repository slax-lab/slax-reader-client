build-apk-test:
	./gradlew assembleDebug

build-apk-release:
	./gradlew assembleRelease

pod-install:
	./gradlew :composeApp:podInstall

gen-privacy:
	cd composeApp && python3 ../script/required_reason_finder.py