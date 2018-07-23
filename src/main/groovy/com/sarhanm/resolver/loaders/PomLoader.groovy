/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.loaders

import com.sarhanm.resolver.VersionManifest
import com.sarhanm.resolver.loaders.VersionManifestLoader
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class PomLoader implements VersionManifestLoader {

    private static final Logger logger = Logging.getLogger(PomLoader)

    @Override
    VersionManifest load(String text) {

        def versionManifest = new VersionManifest()

        try {

            def projectRoot = new XmlSlurper().parseText(text)

            def dependencyList = projectRoot?.dependencyManagement?.dependencies?.dependency

            dependencyList?.each { node ->

                def ver = node?.version?.text()
                def group = node?.groupId?.text()
                def name = node?.artifactId?.text()

                if (ver && group && name) {
                    versionManifest.addVersion(group, name, ver)
                }
            }

        } catch (Exception e) {
            logger.error("Failed to parse XML document", e)
        }

        return versionManifest
    }
}
