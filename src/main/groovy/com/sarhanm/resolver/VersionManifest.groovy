/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class VersionManifest {


    private Map<String, String> versions

    VersionManifest() {
        versions = new HashMap<>()
    }

    String getVersion(group, name) {
        String key = getKey(group, name)

        if (!versions.containsKey(key))
            return null

        return versions.get(key)
    }

    void addVersions(VersionManifest other) {

        if (other != null && other.versions != null)
            this.versions.putAll(other.versions)
    }

    void addVersion(group, name, version) {
        String key = getKey(group, name)
        versions[key] = version
    }

    private String getKey(group, name) {

        String key = "$group:$name"
        return key
    }


}
