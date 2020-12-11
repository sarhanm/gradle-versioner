package com.sarhanm.resolver

/**
 *
 * @author Mohammad Sarhan
 */
class VersionResolverViaGradle5 extends  VersionResolverViaGradleVersionsTest {

    @Override
    def String getAlternateGradleVersion() {
        return '5.6.4'
    }
}
