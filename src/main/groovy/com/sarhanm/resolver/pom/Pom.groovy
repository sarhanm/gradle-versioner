/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.pom

import com.sarhanm.resolver.VersionManifest

/**
 * Represents a basic pom file
 * @author Mohammad Sarhan (mohammad@)
 */
class Pom {

    private Pom parent
    private Properties properties
    private List<Dependency> dependencies

    /**
     *
     * @param parent
     * @param properties - properties of this pom and the parent pom.
     * @param dependencies
     */
    Pom(parent, properties, dependencies) {
        this.parent = parent
        this.properties = properties
        this.dependencies = dependencies
    }

    Properties getProperties() {
        return properties
    }

    VersionManifest getVersionManifest() {
        def manifest = new VersionManifest()

        //We get all the dependencies in the right order first and then we resolve versions on
        // purpose to make sure that property overrides in child poms are honored.

        allDependencies?.each { dep ->
            def ver = interpolateVersion(dep.getVersion())
            if (ver)
                manifest.addVersion(dep.groupId, dep.artifactId, ver)
        }

        return manifest
    }

    /**
     *
     * @return all dependencies
     */
    private List<Dependency> getAllDependencies() {
        List<Dependency> list = []

        if (parent)
            list.addAll(parent.getAllDependencies())

        list.addAll(dependencies)

        return list
    }

    /**
     * Interpolate version string from properties.
     * @param ver
     * @return
     */
    String interpolateVersion(ver) {

        if (!ver.trim().startsWith('${'))
            return ver

        //Get value from properties and its default set.
        def interpolated = properties.getProperty(stripKey(ver))

        //could not find an interpolation, just return
        if (interpolated == null)
            return ver

        return interpolateVersion(interpolated)
    }

    String stripKey(String ver) {
        ver = ver.trim()

        if (ver.endsWith("}") && ver.startsWith('${')) {
            ver = ver - '${'
            ver = ver - '}'
            ver = ver.trim()
        }

        return ver
    }
}
