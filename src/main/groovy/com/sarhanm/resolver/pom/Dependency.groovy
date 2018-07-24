/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.pom

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class Dependency {

    private String groupId
    private String artifactId
    private String version

    Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
    }

    String getGroupId() {
        return groupId
    }

    String getArtifactId() {
        return artifactId
    }

    String getVersion() {
        return version
    }
}
