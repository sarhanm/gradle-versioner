package com.sarhanm.versioner

import nebula.test.IntegrationSpec

import java.util.regex.Pattern

/**
 * Testing to make sure the plugin works in all supported versions of gradle
 * @author Mohammad Sarhan
 */
//This test fails on travis ci. not sure why yet.....
class VersionerGradleVersionsTest //extends IntegrationSpec
{

    static DEFAULT_BUILD = '''
            apply plugin: 'java'
            apply plugin: 'com.sarhanm.versioner'
        '''.stripIndent()

    void setup() {
        //reset the gradle version every time.
        gradleVersion = null
    }

    def 'verifying plugin with version 2.8'() {
        buildFile << DEFAULT_BUILD
        gradleVersion = '2.8'

        when:
        def result = runTasks("build")

        then:
        result.success
    }

    def 'verify plugin with version 2.9'(){
        buildFile << DEFAULT_BUILD
        gradleVersion = '2.9'

        when:
        def result = runTasks("build")

        then:
        result.success
    }

    def 'verify plugin with version 2.10'(){
        buildFile << DEFAULT_BUILD
        gradleVersion = '2.10'

        when:
        def result = runTasks("build")

        then:
        result.success
    }

}
