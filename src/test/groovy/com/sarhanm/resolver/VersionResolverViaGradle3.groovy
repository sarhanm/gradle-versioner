package com.sarhanm.resolver

/**
 *
 * @author Mohammad Sarhan
 */
class VersionResolverViaGradle3 extends  VersionResolverViaGradleVersionsTest {

    @Override
    def getAlternateGradleVersion() {
        return '3.1'
    }
}
