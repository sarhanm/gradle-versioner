package com.sarhanm.resolver

/**
 *
 * @author Mohammad Sarhan
 */
class VersionResolverViaGradle2 extends  VersionResolverViaGradleVersionsTest {

    @Override
    def getAlternateGradleVersion() {
        return '2.14.1'
    }
}
