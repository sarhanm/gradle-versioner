#!/usr/bin/env bash

# Only publish the master branch
if [ "$TRAVIS_REPO_SLUG" == "sarhanm/gradle-versioner" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
    ./gradlew check bintrayUpload
else
    ./gradlew check
fi
