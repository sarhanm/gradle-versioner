#!/usr/bin/env bash

# Only publish the master branch that are NOT pull requests.
if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    #./gradlew check jacocoTestReport coveralls bintrayUpload publishPlugins --info --stacktrace
    ./gradlew check jacocoTestReport coveralls publish closeAndReleaseSonatypeStagingRepository publishPlugins --info --stacktrace
else
    ./gradlew check --info --stacktrace
fi
