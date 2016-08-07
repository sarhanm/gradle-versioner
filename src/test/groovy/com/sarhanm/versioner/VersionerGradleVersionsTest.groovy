package com.sarhanm.versioner

import com.sarhanm.IntegrationSpec

/**
 * Testing to make sure the plugin works in all supported versions of gradle
 * @author Mohammad Sarhan
 */
//This test fails on travis ci. not sure why yet.....
class VersionerGradleVersionsTest extends IntegrationSpec {

    static DEFAULT_BUILD = '''
        plugins{
          id 'com.sarhanm.versioner'
        }
        apply plugin: 'java'
        '''.stripIndent()

    def gradleVersion

    def 'verifying plugin with version 2.8'() {
        buildFile << DEFAULT_BUILD
        gradleVersion = '2.8'

        when:
        def runner = getRunner(true, "build").withGradleVersion(gradleVersion)
        def result = runner.build()

        then:
        result
    }

    def 'verify plugin with version 2.9'() {
        buildFile << DEFAULT_BUILD
        gradleVersion = '2.9'

        when:
        def runner = getRunner(true, "build").withGradleVersion(gradleVersion)
        def result = runner.build()

        then:
        result
    }

    def 'verify plugin with version 2.10'() {
        buildFile << DEFAULT_BUILD
        gradleVersion = '2.10'

        when:
        def runner = getRunner(true, "build").withGradleVersion(gradleVersion)
        def result = runner.build()

        then:
        result
    }

}
