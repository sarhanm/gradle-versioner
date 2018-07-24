/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.loaders

import com.sarhanm.resolver.VersionManifest
import com.sarhanm.resolver.pom.Dependency
import com.sarhanm.resolver.pom.Pom
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class PomLoader implements VersionManifestLoader {

    private static final Logger logger = Logging.getLogger(PomLoader)

    private Project project

    PomLoader(Project project) {
        this.project = project
    }

    VersionManifest load(String text) {
        try {
            Pom pom = buildPom(text)
            return pom.getVersionManifest()
        } catch (Exception e) {
            logger.error("Failed to parse XML document", e)
        }

        return new VersionManifest()
    }

    private Pom buildPom(String text) {

        def root = new XmlSlurper().parseText(text)

        def parentNode = root.breadthFirst().find { it.name() == 'parent' }
        Pom parent = null
        if (parentNode) {
            def file = resolveFile(parentNode.groupId.text(), parentNode.artifactId.text(), parentNode.version.text())
            if (file) {
                def parentText = file.text
                parent = buildPom(parentText)
            }
        }

        Properties props = parent?.getProperties() ? new Properties(parent.getProperties()) : new Properties()

        root.breadthFirst().find({ it.name() == 'properties' })?.children()?.each { n ->
            props.setProperty(n.name(), n.text())
        }

        List<Dependency> dependencies = []
        root?.dependencyManagement?.dependencies?.dependency?.each { n ->
            def group = n?.groupId?.text()
            def artifactId = n?.artifactId?.text()
            def version = n?.version?.text()
            dependencies.add new Dependency(group, artifactId, version)
        }

        return new Pom(parent, props, dependencies)
    }

    //public for testing
    File resolveFile(String group, String name, String version) {

        def notation = "$group:$name:$version@pom"

        logger.debug("Resolving file for {}", notation)

        def config = project.configurations.detachedConfiguration(project.dependencies.create(notation))
        def files = config.resolve()
        if (files && !files.empty)
            return files[0]
        return null
    }

}
