package com.sarhanm.resolver

import org.junit.BeforeClass
import org.junit.Test
import spock.lang.Specification

import static org.junit.Assert.*

class VersionResolutionConfigurerTest extends Specification{


    def testGetMajorVersionFromGradleVersionString() {
        when:
        def v = VersionResolutionConfigurer.getMajorVersionFromGradleVersionString("7.1.1")
        then:
        7 == v

        when:
        v = VersionResolutionConfigurer.getMajorVersionFromGradleVersionString("6.7.1")
        then:
        6 == v

        when:
        v = VersionResolutionConfigurer.getMajorVersionFromGradleVersionString("12.7.1")
        then:
        12 == v

        when:
        v = VersionResolutionConfigurer.getMajorVersionFromGradleVersionString("1.7.1")
        then:
        1 == v

        when:
        v = VersionResolutionConfigurer.getMajorVersionFromGradleVersionString("4.0.0")
        then:
        4 == v

        when:
        v = VersionResolutionConfigurer.getMajorVersionFromGradleVersionString("3.5.0")
        then:
        3 == v

        when:
        v = VersionResolutionConfigurer.getMajorVersionFromGradleVersionString("2.1.1")
        then:
        2 == v
    }
}
