/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver

import com.sarhanm.resolver.loaders.PomLoader
import com.sarhanm.resolver.loaders.VersionManifestLoader
import com.sarhanm.resolver.loaders.YamlLoader
import groovyx.net.http.ApacheHttpBuilder
import groovyx.net.http.util.SslUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Ability to read the version options and the versionManifest configuration
 * and resolve pom or yaml files.
 * @author Mohammad Sarhan (mohammad@)
 */
class VersionManifestResolver {

    private static final Logger logger = Logging.getLogger(VersionManifestResolver)

    private VersionManifestOption options
    private Map<String, VersionManifestLoader> fileResolvers

    VersionManifestResolver(Project project, VersionManifestOption options) {
        this.options = options

        fileResolvers = new HashMap<>()
        fileResolvers['pom'] = new PomLoader(project)
        fileResolvers['yaml'] = new YamlLoader()
    }

    VersionManifest getVersionManifest(List<URI> locations) {

        // It is possible to use the plugin without providing a manifest.
        // For example, if you just wanted to use version ranges.
        if (locations.isEmpty())
            return new VersionManifest()


        VersionManifest allVersions = new VersionManifest()

        locations.each { URI location ->

            def parser = "yaml"
            if (location.path.endsWith(".pom"))
                parser = "pom"

            String manifestData = null
            if (location.scheme.startsWith("http")) {

                logger.debug("Remote Version Manifest: {}", location)

                def builder = ApacheHttpBuilder.configure {
                    request.uri = location

                    if (options?.username != null) {
                        request.auth.basic options.username, options.password
                    }

                    //Set timeout values
                    client.clientCustomizer { HttpClientBuilder builder ->
                        RequestConfig.Builder requestBuilder = RequestConfig.custom()
                        requestBuilder.connectTimeout = 10000
                        requestBuilder.connectionRequestTimeout = 10000
                        builder.defaultRequestConfig = requestBuilder.build()
                    }

                    if (options?.ignoreSSL)
                        SslUtils.ignoreSslIssues execution
                }

                manifestData = builder.get {

                    response.failure {
                        logger.error("FAILED to retrieve version manifest from {}", location)
                    }
                }

            } else {
                def url = location.toURL()
                logger.debug("Version Manifest: {}", url)
                manifestData = url.text
            }


            if (manifestData) {

                logger.debug("Loaded version manifest {}\n{}", location, manifestData)

                //load from pom or yaml loader
                def manifest = fileResolvers[parser].load(manifestData)

                if (manifest)
                    allVersions.addVersions(manifest)
            } else {
                logger.error("Failed to retrieve manifest from $location")
            }
        }

        return allVersions
    }
}
