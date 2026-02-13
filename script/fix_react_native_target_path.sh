#!/bin/bash
set -e

ARTIFACTS_DIR="reactNativeApp/artifacts"

for XCFW in "$ARTIFACTS_DIR"/*.xcframework; do
  NAME=$(basename "$XCFW" .xcframework)

  for FW in "$XCFW"/*/; do
    BINARY="$FW$NAME.framework/$NAME"
    [ -f "$BINARY" ] || continue

    CURRENT=$(otool -D "$BINARY" | tail -1)
    EXPECTED="@rpath/$NAME.framework/$NAME"

    if [ "$CURRENT" != "$EXPECTED" ]; then
      echo "fix: $CURRENT → $EXPECTED"
      install_name_tool -id "$EXPECTED" "$BINARY"
    else
      echo "right: $BINARY"
    fi
  done
done