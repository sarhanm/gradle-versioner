package com.sarhanm.versioner

import com.sarhanm.resolver.Version
import nebula.test.IntegrationSpec

/**
 * Verify versioner options get picked up.
 * @author Mohammad Sarhan
 */
class VersionerBuildTest extends IntegrationSpec {

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

        then:
        //No branch meta-data and thus not a valid versioner version.
        //but a valid project version nonetheless.
        !version.valid
        buildVersion ==~ /([0-9]+\.?)+/
    }

    def 'test versioned build'() {
        buildFile << '''

        import com.sarhanm.versioner.GitExecutor
        def gx = new GitExecutor(project)
        gx.execute('init .')
        gx.execute('add .')
        gx.execute("commit -m'a commit' ")

        apply plugin: 'java'
        apply plugin: 'com.sarhanm.versioner'
        println project.version
        '''.stripIndent()

        when:
        def result = runTasksSuccessfully("build")
        println result.standardOutput
        println result.standardError
        def buildVersion = getVersionFromOutput(result.standardOutput)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '1'
        assert version.branch == 'master'
    }

    def 'test feature branch build'(){
        buildFile << '''

        import com.sarhanm.versioner.GitExecutor
        def gx = new GitExecutor(project)
        gx.execute('init .')
        gx.execute('add .')
        gx.execute("commit -m'a commit' ")

        gx.execute('checkout -b feature/a-feature')
        gx.execute('commit -m"adding commit in feature" --allow-empty')
        gx.execute('commit -m"adding commit in feature" --allow-empty')


        apply plugin: 'java'
        apply plugin: 'com.sarhanm.versioner'
        println project.version
        '''.stripIndent()

        when:
        def result = runTasksSuccessfully("build")
        println result.standardOutput
        println result.standardError
        def buildVersion = getVersionFromOutput(result.standardOutput)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '0'
        assert version.minor == '0'
        assert version.point == '3'
        assert version.branch == 'feature-a-feature'
    }

    def 'test tagged build'(){
        buildFile << '''

        import com.sarhanm.versioner.GitExecutor
        def gx = new GitExecutor(project)
        gx.execute('init .')
        gx.execute('add .')
        gx.execute("commit -m'a commit' ")
        gx.execute('commit -m"adding commit in 2" --allow-empty')
        gx.execute('commit -m"adding commit in 3" --allow-empty')

        gx.execute('tag -a v6.8 -m "my version 6.8"')
        gx.execute('commit -m"adding commit in 4" --allow-empty')
        gx.execute('commit -m"adding commit in 5" --allow-empty')


        apply plugin: 'java'
        apply plugin: 'com.sarhanm.versioner'
        println project.version
        '''.stripIndent()

        when:
        def result = runTasksSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.standardOutput)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '6'
        assert version.minor == '8'
        assert version.point == '2' //number of commits from last tag
        assert version.branch == 'master'
    }


    def 'test hotfix build'() {
        buildFile << '''

        import com.sarhanm.versioner.GitExecutor
        def gx = new GitExecutor(project)
        gx.execute('init .')
        gx.execute('add .')
        gx.execute("commit -m'a commit' ")
        gx.execute('commit -m"adding commit in master" --allow-empty')
        gx.execute('checkout -b hotfix/some-hotfix')
        gx.execute('commit -m"adding file in hotfix" --allow-empty')
        gx.execute('commit -m"adding file in hotfix" --allow-empty')


        apply plugin: 'java'
        apply plugin: 'com.sarhanm.versioner'
        println project.version
        '''.stripIndent()

        when:
        def result = runTasksSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.standardOutput)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '2'
        assert version.hotfix == '2'
        assert version.branch == 'hotfix-some-hotfix'
    }

    def 'test release/hotfix build'() {
        buildFile << '''

        import com.sarhanm.versioner.GitExecutor
        def gx = new GitExecutor(project)
        gx.execute('init .')
        gx.execute('add .')
        gx.execute("commit -m'a commit' ")
        gx.execute('commit -m"adding commit in master" --allow-empty')
        gx.execute('checkout -b release/hotfix')
        gx.execute('commit -m"adding file in hotfix" --allow-empty')
        gx.execute('commit -m"adding file in hotfix" --allow-empty')


        apply plugin: 'java'
        apply plugin: 'com.sarhanm.versioner'
        println project.version
        '''.stripIndent()

        when:
        def result = runTasksSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.standardOutput)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '2'
        assert version.hotfix == '2'
        assert version.branch == 'release-hotfix'
    }

    def 'test hotfix/release build'() {
        buildFile << '''

        import com.sarhanm.versioner.GitExecutor
        def gx = new GitExecutor(project)
        gx.execute('init .')
        gx.execute('add .')
        gx.execute("commit -m'a commit' ")
        gx.execute('commit -m"adding commit in master" --allow-empty')
        gx.execute('checkout -b hotfix/release')
        gx.execute('commit -m"adding file in hotfix" --allow-empty')
        gx.execute('commit -m"adding file in hotfix" --allow-empty')


        apply plugin: 'java'
        apply plugin: 'com.sarhanm.versioner'
        println project.version
        '''.stripIndent()

        when:
        def result = runTasksSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.standardOutput)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '2'
        assert version.hotfix == '2'
        assert version.branch == 'hotfix-release'
    }

    private String getVersionFromOutput(String output)
    {
        def r = output.find(VERSIONER_PATTERN)
        return r[r.indexOf('=')+1..-1]
    }
}
