package com.sarhanm.versioner

import com.sarhanm.IntegrationSpec
import com.sarhanm.resolver.Version
import org.apache.tools.ant.types.Commandline
import org.gradle.testkit.runner.GradleRunner

/**
 * Verify versioner options get picked up.
 * @author Mohammad Sarhan
 */
class VersionerBuildTest extends IntegrationSpec {

    static VERSIONER_PATTERN = ~/versioner:[^=]+=(.*)/
    static DEFAULT_BRANCH_NAME = 'master'

    static DEFAULT_BUILD = '''
            plugins{
                id 'com.sarhanm.versioner'
            }
            apply plugin: 'java'
        '''.stripIndent()

    def setupGitRepo(String defaultBranch = DEFAULT_BRANCH_NAME) {
        execute('git init .')
        execute('git add .')
        execute('git config user.email "test@test.com"')
        execute('git config user.name "tester"')
        execute('git commit -m"commit" ')

        if (defaultBranch != DEFAULT_BRANCH_NAME) {
            execute("git branch -m $DEFAULT_BRANCH_NAME $defaultBranch")
        }
    }

    def 'test build without options'() {
        buildFile << DEFAULT_BUILD

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)

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

        setupGitRepo()

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        //No branch meta-data and thus not a valid versioner version.
        //but a valid project version nonetheless.
        !version.valid
        buildVersion ==~ /([0-9]+\.?)+/
    }

    def 'test versioned build'() {
        buildFile << DEFAULT_BUILD

        setupGitRepo()

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '1'
        assert version.branch == 'master'
    }

    def 'test feature branch build'() {
        buildFile << DEFAULT_BUILD

        setupGitRepo()
        execute('git checkout -b feature/a-feature')
        execute("git commit -m'adding commit in feature' --allow-empty")
        execute('git commit -m"adding commit in feature" --allow-empty')
        execute('git log')
        when:
        def runner = getRunner(true, "build");
        runner.forwardOutput()
        def result = runner.build()
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.branch == 'feature-a-feature'
        assert version.major == '0'
        assert version.minor == '0'
        assert version.point == '3'
    }

    def 'test tagged build'() {
        buildFile << DEFAULT_BUILD

        setupGitRepo()
        execute('git commit -m"adding commit in 2" --allow-empty')
        execute('git commit -m"adding commit in 3" --allow-empty')

        execute('git tag -a v6.8 -m "my version 6.8"')
        execute('git commit -m"adding commit in 4" --allow-empty')
        execute('git commit -m"adding commit in 5" --allow-empty')

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '6'
        assert version.minor == '8'
        assert version.point == '2' //number of commits from last tag
        assert version.branch == 'master'
    }

    def 'test tagged build with non-annotated tag'() {
        buildFile << DEFAULT_BUILD

        setupGitRepo()
        execute('git commit -m"adding commit in 2" --allow-empty')
        execute('git commit -m"adding commit in 3" --allow-empty')

        execute('git tag v6.8')
        execute('git commit -m"adding commit in 4" --allow-empty')
        execute('git commit -m"adding commit in 5" --allow-empty')

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '6'
        assert version.minor == '8'
        assert version.point == '2' //number of commits from last tag
        assert version.branch == 'master'
    }

    def 'test hotfix build'() {
        buildFile << DEFAULT_BUILD

        setupGitRepo()
        execute('git commit -m"adding commit in master" --allow-empty')
        execute('git checkout -b hotfix/some-hotfix')
        execute('git commit -m"adding file in hotfix" --allow-empty')
        execute('git commit -m"adding file in hotfix" --allow-empty')

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '2'
        assert version.hotfix == '2'
        assert version.branch == 'hotfix-some-hotfix'
    }

    def 'test hotfix build on main'() {
        buildFile << DEFAULT_BUILD

        setupGitRepo('main')
        execute('git commit -m"adding commit in main" --allow-empty')
        execute('git checkout -b hotfix/some-hotfix')
        execute('git commit -m"adding file in hotfix" --allow-empty')

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '2'
        assert version.hotfix == '1'
        assert version.branch == 'hotfix-some-hotfix'
    }

    def 'test release/hotfix build'() {
        buildFile << DEFAULT_BUILD

        setupGitRepo()
        execute('git commit -m"adding commit in master" --allow-empty')
        execute('git checkout -b release/hotfix')
        execute('git commit -m"adding file in hotfix" --allow-empty')
        execute('git commit -m"adding file in hotfix" --allow-empty')

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
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
        buildFile << DEFAULT_BUILD

        setupGitRepo()
        execute('git commit -m"adding commit in master" --allow-empty')
        execute('git checkout -b hotfix/release')
        execute('git commit -m"adding file in hotfix" --allow-empty')
        execute('git commit -m"adding file in hotfix" --allow-empty')

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '2'
        assert version.hotfix == '2'
        assert version.branch == 'hotfix-release'
    }

    def 'test with publish plugin'() {
        buildFile << DEFAULT_BUILD + """

        apply plugin: 'maven-publish'
        publishing{
            publications {
                mavenIosApp(MavenPublication) {
                    from project.components.java
                    version project.version
                }
            }
        }
        """.stripIndent()

        setupGitRepo()
        execute('git commit -m"adding 2nd commit in master" --allow-empty')
        execute('git checkout -b hotfix/release')
        execute('git commit -m"adding file in hotfix" --allow-empty')
        execute('git commit -m"adding file in hotfix" --allow-empty')

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '0'
        assert version.point == '2'
        assert version.hotfix == '2'
        assert version.branch == 'hotfix-release'

    }

    def 'test lightweight tags'(){
        buildFile << DEFAULT_BUILD

        setupGitRepo()
        execute('git commit -m"adding 2nd commit in master" --allow-empty')
        execute('git tag v7.13')

        when:
        def result = runSuccessfully("build")
        def buildVersion = getVersionFromOutput(result.output)
        def version = new Version(buildVersion)

        then:
        version.valid
        assert version.major == '7'
        assert version.minor == '13'
        assert version.point == '0' //number of commits from last tag
        assert version.branch == 'master'
    }

    def 'test lots of commits'(){
        buildFile << DEFAULT_BUILD

        setupGitRepo()
        execute('git tag v1.2')
        execute('touch file.txt')
        execute('git add .')
        execute("git commit -am'change'")


        execute('git checkout master')
        (1..25).each {
            execute('git commit -m"empty commit" --allow-empty')
        }

        execute('git checkout -b feature/otherbranch')
        (1..100).each {
            execute('git commit -m"empty commit" --allow-empty')
        }

        execute('git checkout -b feature/different')
        (1..100).each {
            execute('git commit -m"empty commit" --allow-empty')
        }

        // There are over a total of 225 commits in this test repo
        // We are testing that a git clone of 50 from the master branch (which only has 25ish commits)
        // will still have
        // 1. the correct version
        // 2. and the same version of a clone of 1000
        execute('git checkout master')
        execute("git clone --depth 50   file://${projectDir.absolutePath} ./shallow_50")
        execute("git clone --depth 1000 file://${projectDir.absolutePath} ./shallow_1000")

        when:
        def shallow50Runner = GradleRunner.create()
                .withProjectDir(new File(projectDir, "shallow_50"))
                .withArguments("build").withPluginClasspath()

        def shallow150Runner = GradleRunner.create()
                .withProjectDir(new File(projectDir, "shallow_1000"))
                .withArguments("build").withPluginClasspath()

        def shallow50Version = getVersionFromOutput(shallow50Runner.build().output)
        def shallow1000Version = getVersionFromOutput(shallow150Runner.build().output)

        then:
        shallow50Version == shallow1000Version

        when:
        def version = new Version(shallow50Version)

        then:
        version.valid
        assert version.major == '1'
        assert version.minor == '2'
        assert version.branch == 'master'
    }


    private String getVersionFromOutput(String output) {
        def r = output.find(VERSIONER_PATTERN)
        return r[r.indexOf('=') + 1..-1]
    }

    def execute(String cmd) {
        Commandline.translateCommandline(cmd).execute([], projectDir.absoluteFile).waitFor()
    }
}
