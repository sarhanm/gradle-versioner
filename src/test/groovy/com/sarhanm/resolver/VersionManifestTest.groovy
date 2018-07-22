/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver

import spock.lang.Specification

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class VersionManifestTest extends Specification {

    def 'test add and retrieve'() {

        VersionManifest m = new VersionManifest()

        when:
        m.addVersion("foobar", "jonny", "1.3.2")
        then:
        m.getVersion("foobar", "jonny") == "1.3.2"
    }

    def 'test override values manually'() {
        VersionManifest m = new VersionManifest()

        when:
        m.addVersion("foobar", "jonny", "1.3.2")
        then:
        m.getVersion("foobar", "jonny") == "1.3.2"

        when:
        m.addVersion("foobar", "jonny", "1.3.3")
        then:
        m.getVersion("foobar", "jonny") == "1.3.3"
    }

    def 'test override with other VersionManifest'() {
        VersionManifest m = new VersionManifest()

        when:
        m.addVersion("foobar", "jonny", "1.3.2")
        then:
        m.getVersion("foobar", "jonny") == "1.3.2"

        when:
        VersionManifest o = new VersionManifest()
        o.addVersion("foobar", "jonny", "1.3.5")
        o.addVersion("foobar", "boby", "5.4.3")
        m.addVersions(o)

        then:
        m.getVersion("foobar", "jonny") == "1.3.5"
        m.getVersion("foobar", "boby") == "5.4.3"
    }


}
