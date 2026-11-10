#!/bin/sh
# Apache Maven Wrapper startup script (simplified)
WRAPPER_JAR="$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  mkdir -p "$(dirname "$WRAPPER_JAR")"
  echo "Downloading Maven Wrapper..."
  if command -v curl >/dev/null 2>&1; then curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL";
  elif command -v wget >/dev/null 2>&1; then wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"; fi
fi
JAVA_EXE="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVA_EXE" -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
