package com.sarhanm.resolver

/**
 *
 * @author Mohammad Sarhan
 */
class VersionResolverViaGradle4 extends  VersionResolverViaGradleVersionsTest {

    @Override
    def String getAlternateGradleVersion() {
        return '4.4.1'
    }
}
