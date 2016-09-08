package com.sarhanm.resolver

import com.sarhanm.IntegrationSpec
import org.apache.commons.io.FileUtils

/**
 * @author Mohammad Sarhan ( mohammad@ ) 
 * @date 2/11/16
 */
class VersionResolverBuildTest extends IntegrationSpec {

    static DEFAULT_BUILD = '''
    plugins{
        id 'com.sarhanm.version-resolver'
    }
    apply plugin: 'java'
    apply plugin: 'maven'

    repositories{
        jcenter()
        mavenCentral()
    }


    '''.stripIndent()

    static DEFAULT_MANIFEST = '''

    versionResolver{
        versionManifest{
            url = "file://" + file('build/versions.yaml')
        }
    }
    '''.stripIndent()

    def versionsFile

    def setup() {
        versionsFile = file("build/versions.yaml")
        versionsFile.parentFile.mkdirs()
        setupLocalepo()
    }

    def 'test version resolution'() {
        buildFile << DEFAULT_BUILD + """
        versionResolver{
            versionManifest{
                url = file('build/versions.yaml')
            }
        }
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        def java = file('src/main/java/Hello.java')
        java.parentFile.mkdirs()
        java.text = 'public class Hello{}'

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-lang:commons-lang', '--stacktrace')
        def result = runner.build()

        then:
        println result.output
        //We should inherit the version of commons-lang from the commons-configuration dependencies
        result.output.contains("commons-lang:commons-lang:2.6\n")
    }

    def 'resolve via http options.url'(){
        buildFile << DEFAULT_BUILD + """
        versionResolver{
            versionManifest{
                url = 'https://raw.githubusercontent.com/sarhanm/gradle-versioner/master/src/test/resources/test-repo/test/manifest/2.0/manifest-2.0.yaml'
            }
        }
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        def java = file('src/main/java/Hello.java')
        java.parentFile.mkdirs()
        java.text = 'public class Hello{}'

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration', '--stacktrace')
        def result = runner.build()

        then:
        println result.output
        //We should inherit the version of commons-lang from the commons-configuration dependencies
        result.output.contains("commons-configuration:commons-configuration:auto -> 1.9\n")
    }

    def 'resolve release platform override of transitive dependency'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.0'
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('build', 'dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        //release platform is setting commons-lang to 2.0, so that should be the version
        // of our transitive dependency regardless of what version is defined in that pom.
        result.output.contains 'commons-lang:commons-lang:2.6 -> 2.0\n'

    }

    def 'resolve hard pinned version in build file'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """

        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
            //compile 'commons-lang:commons-lang:2.0'
        }
        """.stripIndent()


        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('build', 'dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        // This is the default gradle strategy. Gradle picks the newest and since we don't have a pinned version
        // in the manifest, the latest version is selected.
        result.output.contains 'commons-lang:commons-lang:2.6\n'

    }

    def 'hard pinned version in build with def in version manifest with transitive dep'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
            compile 'commons-lang:commons-lang:2.0'
        }
        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.5'
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('build', 'dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        // exists in the version manifest and is overridden by the build.gradle file
        result.output.contains('commons-lang:commons-lang:2.6 -> 2.0\n')
    }

    def 'resolve hard pinned version in build with auto transitive'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:1.9'
        }
        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('build', 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')

        then:
        // Allowing pinned version in build.gradle to override manifest.
        result.output.contains "commons-configuration:commons-configuration:1.9\n"

    }

    def 'resolve using gradles dynamic version'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:+'
        }
        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.5'
        """.stripIndent()

        when:

        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')
        runner.forwardOutput()
        def result = runner.build()

        then:
        // Allowing pinned version in build.gradle to override manifest.
        result.output ==~ /(?ms).*commons-configuration:commons-configuration:\+ -> [^\s]*\n.*/

    }

    def 'resolve using versioner dynamic version'() {
        buildFile << """
        plugins{
            id 'com.sarhanm.version-resolver'
        }
        apply plugin: 'java'
        apply plugin: 'maven'

        dependencies{
            compile 'test:test-jar:1.n.n.master+'
        }

        repositories{
            maven{ url file('build/.m2/repository').path }
        }

        """.stripIndent()

        when:
        def result = runSuccessfully('build', 'dependencyInsight', '--dependency', 'test:test-jar')

        then:
        result.output ==~ /(?ms).*test:test-jar:1.n.n.master\+ -> 1.0.2.master.abcdef\n.*/
    }

    def 'resolve without manifest'() {
        buildFile << DEFAULT_BUILD + """
        dependencies{
            compile 'commons-configuration:commons-configuration:1.9'
        }
        """.stripIndent()

        when:
        def result = runSuccessfully('build', 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')

        then:
        result.output.contains "commons-configuration:commons-configuration:1.9\n"

    }

    def setupLocalepo() {
        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)
    }
}
