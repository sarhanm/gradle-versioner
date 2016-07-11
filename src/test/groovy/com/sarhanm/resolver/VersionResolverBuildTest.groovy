package com.sarhanm.resolver

import com.sarhanm.IntegrationSpec

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

    versionResolver{
        versionManifest{
            url = "file://" + file('build/versions.yaml')
        }
    }
    '''.stripIndent()

    def versionsFile

    def setup(){
        versionsFile = file("build/versions.yaml")
        versionsFile.parentFile.mkdirs()
    }

    def 'test version resolution'() {
        buildFile << DEFAULT_BUILD + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:

        //We should inherit the version of commons-lang from the commons-configuration dependencies
        result.output.contains("commons-lang:commons-lang:2.6\n")
    }

    def 'resolve release platform override of transitive dependency'(){
        buildFile << DEFAULT_BUILD + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.0'
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        //release platform is setting commons-lang to 2.0, so that should be the version
        // of our transitive dependency regardless of what version is defined in that pom.
        result.output.contains 'commons-lang:commons-lang:2.6 -> 2.0\n'

    }

    def 'resolve hard pinned version in build file'() {
        buildFile << DEFAULT_BUILD + """

        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
            //compile 'commons-lang:commons-lang:2.0'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        // This is the default gradle strategy. Gradle picks the newest and since we don't have a pinned version
        // in the manifest, the latest version is selected.
        result.output.contains 'commons-lang:commons-lang:2.6\n'

    }

    def 'hard pinned version in build with def in version manifest with transitive dep'() {
        buildFile << DEFAULT_BUILD + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
            compile 'commons-lang:commons-lang:2.0'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.5'
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        // exists in the version manifest and is overridden by the build.gradle file
        result.output.contains('commons-lang:commons-lang:2.6 -> 2.0\n')
    }

    def 'resolve hard pinned version in build with auto transitive'() {
        buildFile << DEFAULT_BUILD + """
        dependencies{
            compile 'commons-configuration:commons-configuration:1.9'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')

        then:
        // Allowing pinned version in build.gradle to override manifest.
        result.output.contains "commons-configuration:commons-configuration:1.9\n"

    }

    def addJavaFile() {
        //Need a java file to actually make compile go and resolution fail when we have an issue
        //otherwise resolution fails silently
        def javaFile = file("src/main/java/HelloWorld.java")
        javaFile.parentFile.mkdirs()
        javaFile.text =  """
        public class HelloWorld{
            public static void main(String[] args){
            }
        }
        """.stripIndent()
    }
}
