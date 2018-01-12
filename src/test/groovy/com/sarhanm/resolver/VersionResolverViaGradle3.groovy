package com.sarhanm.resolver

/**
 *
 * @author Mohammad Sarhan
 */
class VersionResolverViaGradle3 extends  VersionResolverViaGradleVersionsTest {

    @Override
    def String getAlternateGradleVersion() {
        return '3.1'
    }
}
