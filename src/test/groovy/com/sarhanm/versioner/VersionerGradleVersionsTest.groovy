package com.sarhanm.versioner

import com.sarhanm.IntegrationSpec
import org.apache.commons.io.FileUtils
import spock.lang.Ignore

/**
 * Testing to make sure the plugin works in all supported versions of gradle
 * @author Mohammad Sarhan
 */
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

    @Ignore
    def 'verify version range on new gradle version'(){
        buildFile << DEFAULT_BUILD + '''

        repositories{
            System.setProperty('maven.repo.local', file('build/.m2/repository').path)
            mavenLocal()
            jcenter()
        }

        dependencies{
            compile 'test:test-jar:1.n.n.master+'
        }
        '''.stripIndent()
        gradleVersion = '3.1'

        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)

        when:
        def runner = getRunner(true, "build").withGradleVersion(gradleVersion)
        def result = runner.build()

        then:
        result
    }

}
