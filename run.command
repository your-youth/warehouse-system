#!/bin/bash
cd "$(dirname "$0")"

JAR="target/warehouse-system.jar"
if [ ! -f "$JAR" ]; then
    echo "File $JAR not found."
    echo "Build it first: IntelliJ -> Maven -> Lifecycle -> package"
    read -p "Press Enter to close..."
    exit 1
fi

JAVA_HOME_21="$(/usr/libexec/java_home -v 21 2>/dev/null)"
if [ -n "$JAVA_HOME_21" ]; then
    JAVA="$JAVA_HOME_21/bin/java"
else
    JAVA="java"
fi

"$JAVA" -jar "$JAR"