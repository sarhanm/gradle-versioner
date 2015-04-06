#!/usr/bin/env bash

plugin_publish_keys_file=`pwd`/plugin-publish-keys.properties
echo "gradle.publish.key=$gradlePublishKey" > ${plugin_publish_keys_file}
echo "gradle.publish.secret=$gradlePublishSecret" >> ${plugin_publish_keys_file}

# Only publish the master branch that are NOT pull requests.
if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    ./gradlew check bintrayUpload publishPlugins -Dcom.gradle.login.properties.file=$plugin_publish_keys_file
else
    ./gradlew check
fi
