#!/usr/bin/env bash

# Only publish the master branch
if [ "$TRAVIS_REPO_SLUG" == "steveperrin/gradle-versioner" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
    ./gradlew check bintrayUpload
else
    ./gradlew check
fi
