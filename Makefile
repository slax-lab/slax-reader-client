build-apk-test:
	./gradlew assembleDebug

build-apk-release:
	./gradlew assembleRelease

# Desktop JVM builds with CEF
setup-cef:
	@echo "Downloading CEF binaries..."
	./scripts/download-cef.sh

build-desktop-debug: setup-cef
	./gradlew desktopRun

build-desktop-release: setup-cef
	./gradlew packageDmg packageMsi packageDeb

clean-cef:
	@echo "Cleaning CEF bundles..."
	rm -rf kcef-bundle
	rm -rf composeApp/kcef-bundle

clean: clean-cef
	./gradlew clean
