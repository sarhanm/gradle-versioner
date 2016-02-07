package com.sarhanm.versioner

import com.sarhanm.resolver.Version
import nebula.test.IntegrationSpec

/**
 * Verify versioner options get picked up.
 * @author Mohammad Sarhan
 */
class VersionerOptionsBuildTest extends IntegrationSpec {

    static VERSIONER_PATTERN = ~/versioner:[^=]+=(.*)/

    static DEFAULT_BUILD = '''
            apply plugin: 'java'
            apply plugin: 'com.sarhanm.versioner'
        '''.stripIndent()

    def 'test build without options'(){
        buildFile << DEFAULT_BUILD
        when:
        def result = runTasks("build")
        def buildVersion = getVersionFromOutput(result.standardOutput)

        def version = new Version(buildVersion)

        then:
        buildVersion
        version.valid
        version.branch
    }

    def 'test build with ommit branch metadata option'() {
        buildFile << DEFAULT_BUILD + '''
        versioner{
            omitBranchMetadata=true
        }
        '''.stripIndent()

        when:
        def result = runTasksSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.standardOutput)
        def version = new Version(buildVersion)
        println "Build versionsss: $buildVersion"

        then:
        //No branch meta-data and thus not a valid versioner version.
        //but a valid project version nonetheless.
        !version.valid
        buildVersion ==~ /[0-9]+\.[0-9]+.[0-9]+/
    }

    private String getVersionFromOutput(String output)
    {
        def r = output.find(VERSIONER_PATTERN)
        return r[r.indexOf('=')+1..-1]
    }
}
