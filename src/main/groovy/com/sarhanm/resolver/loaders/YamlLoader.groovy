/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.loaders

import com.sarhanm.resolver.VersionManifest
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.yaml.snakeyaml.Yaml

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class YamlLoader implements VersionManifestLoader {

    private static final Logger logger = Logging.getLogger(YamlLoader)

    @Override
    VersionManifest load(String text) {

        //Load manifest and append to existing set of modules
        Yaml yamlLoader = new Yaml()

        Map yaml = yamlLoader.load(text)

        if (yaml.containsKey("modules")) {

            Map modules = yaml['modules']

            if (modules) {

                VersionManifest m = new VersionManifest()

                modules.each { k, v ->
                    def parts = k.split(":")
                    if (parts.length > 1) {
                        m.addVersion(parts[0], parts[1], v)
                    }
                }

                return m
            }
        }

        return null
    }
}
