#!/usr/bin/env bash

# Only publish the master branch that are NOT pull requests.
if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    ./gradlew check bintrayUpload publishPlugins --debug --stacktrace
else
    ./gradlew check --debug --stacktrace
fi
