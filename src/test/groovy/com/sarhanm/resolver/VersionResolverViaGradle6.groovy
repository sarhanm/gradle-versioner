package com.sarhanm.resolver

/**
 *
 * @author Mohammad Sarhan
 */
class VersionResolverViaGradle6 extends  VersionResolverViaGradleVersionsTest {

    @Override
    def String getAlternateGradleVersion() {
        return '6.7.1'
    }
}
