#!/bin/sh
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
MAIN_CLASS=org.gradle.wrapper.GradleWrapperMain
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Gradle wrapper JAR not found at $WRAPPER_JAR" >&2
  echo "Please run 'gradle wrapper' with a local Gradle installation to generate the wrapper files." >&2
  exit 1
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD="java"
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

exec "$JAVA_CMD" $DEFAULT_JVM_OPTS -classpath "$WRAPPER_JAR" $MAIN_CLASS "$@"
