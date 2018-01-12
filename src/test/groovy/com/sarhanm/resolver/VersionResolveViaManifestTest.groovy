package com.sarhanm.resolver

import groovy.mock.interceptor.MockFor
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleVersionSelector
import org.junit.Test
import spock.lang.Specification

import static org.junit.Assert.fail

/**
 *
 * @author mohammad sarhan
 */
class VersionResolveViaManifestTest extends Specification{

    def 'test using manifest version'(){

        given:


        def file = new File("src/test/resources/versions.yaml")

        def options = getOption(file.toURI().toString())

        def selector = Mock(ModuleVersionSelector)
        selector.getVersion() >> 'auto'
        selector.getGroup() >> 'com.coinfling'
        selector.getName() >> 'auth-service-api'

        def details = Mock(DependencyResolveDetails)
        details.getRequested() >> selector

        def project = Mock(Project)
        project.file({ param -> param.endsWith('src/test/resources/versions.yaml')}) >>  file
        project.getAllprojects() >> Collections.emptySet()
        project.getRootProject() >> project

        when:
        def resolver = new VersionResolverInternal(project, options,null, null)
        def ver = resolver.resolveVersionFromManifest(details)

        then:
        ver == "1.0-SNAPSHOT"

    }


    def 'test version manifest missing'(){
        given:

        def file = new File("src/test/resources/versions.yaml")

        def options = getOption(file.toURI().toString())

        def selector = Mock(ModuleVersionSelector)
        selector.getVersion() >> 'auto'
        selector.getGroup() >> 'com.coinfling'
        selector.getName() >> 'not-there'

        def details = Mock(DependencyResolveDetails)
        details.getRequested() >> selector

        def project = Mock(Project)
        project.file({ param -> param.endsWith('src/test/resources/versions.yaml')}) >>  file
        project.getAllprojects() >> Collections.emptySet()
        project.getRootProject() >> project

        when:
        def resolver = new VersionResolverInternal(project, options,null,null)
        def ver = resolver.resolveVersionFromManifest(details)

        then:
        thrown(IllegalStateException)

    }

    def 'test no execution'(){

        given:
        def file = new File("src/test/resources/versions.yaml")

        def options = getOption(file.toURI().toString())

        def selector = Mock(ModuleVersionSelector)
        selector.getVersion() >> '1.2.3'
        selector.getGroup() >> 'com.coinfling'
        selector.getName() >> 'foobar'

        def details = Mock(DependencyResolveDetails)
        details.getRequested() >> selector

        def project = Mock(Project)
        project.file({ param -> param.endsWith('src/test/resources/versions.yaml')}) >>  file
        project.getAllprojects() >> Collections.emptySet()
        project.getRootProject() >> project

        when:
        def resolver = new VersionResolverInternal(project, options,null,null)
        def ver = resolver.resolveVersionFromManifest(details)

        then:
        ver == "1.2.3"

    }

    private VersionManifestOption getOption(def url, def username = null, def password = null) {
        [url: url, username: username, password: password] as VersionManifestOption
    }
}
