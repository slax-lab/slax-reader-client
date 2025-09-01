build-apk-test:
	./gradlew assembleDebug

build-apk-release:
	./gradlew assembleRelease

pod-install:
	./gradlew :composeApp:podInstall