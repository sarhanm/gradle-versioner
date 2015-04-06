#!/usr/bin/env bash

# Only publish the master branch that are NOT pull requests.
if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    # Disabling for now, errors downloading dependencies
    #./gradlew check bintrayUpload  publishPlugins -Dgradle.publish.key=$gradlePublishKey -Dgradle.publish.secret=$gradlePublishSecret
    ./gradlew check bintrayUpload
else
    ./gradlew check
fi
