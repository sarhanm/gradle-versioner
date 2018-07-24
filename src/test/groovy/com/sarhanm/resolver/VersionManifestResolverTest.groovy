/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver

import spock.lang.Specification

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class VersionManifestResolverTest extends Specification{

    def 'test versoin manifest resolution'(){

        List<URI> locations = [new URI('https://raw.githubusercontent.com/sarhanm/gradle-versioner/master/src/test/resources/test-repo/test/manifest/2.0/manifest-2.0.yaml')]

        VersionManifestResolver resolver = new VersionManifestResolver(null,null)

        when:
        def manifest = resolver.getVersionManifest(locations)

        then:
        manifest



    }
}
