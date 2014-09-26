package com.sarhanm.resolver

import groovy.transform.Memoized
import groovy.transform.Synchronized
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.BuildableModuleVersionSelectionResolveResult
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.DefaultBuildableModuleVersionSelectionResolveResult
import org.gradle.api.internal.artifacts.metadata.DefaultDependencyMetaData
import org.gradle.api.internal.artifacts.metadata.DependencyMetaData
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository
import org.gradle.api.logging.Logging
import org.gradle.internal.Transformers
import org.gradle.util.CollectionUtils
import org.yaml.snakeyaml.Yaml

/**
 *
 * @author mohammad sarhan
 */
class VersionResolver implements Action<DependencyResolveDetails>{

    def logger = Logging.getLogger(VersionResolver)


    private Project project
    private VersionResolverOptions options
    private List<ResolutionAwareRepository> repositories = null
    private File manifestFile

    VersionResolver(Project project,VersionResolverOptions options = null, File manifestFile = null) {
        this.project = project
        this.repositories = null;
        this.manifestFile = manifestFile
        this.options = options
    }

    @Synchronized
    List<ResolutionAwareRepository> getRepos()
    {
        if(repositories == null)
        {
            this.repositories = CollectionUtils.collect(project.repositories, Transformers.cast(ResolutionAwareRepository.class))
        }

        return this.repositories
    }

    @Override
    void execute(DependencyResolveDetails details) {


        def requested = details.requested
        def requestedVersion = resolveVersionFromManifest(details)
        def range = new VersionRange(requestedVersion)

        if(range.valid) {
            logger.debug "$requested is a valid range ($requestedVersion). Trying to resolve"
            getRepos().each { repo ->

                def result = new DefaultBuildableModuleVersionSelectionResolveResult()

                DependencyMetaData data = new DefaultDependencyMetaData(new DefaultModuleVersionIdentifier(
                        requested.getGroup(),
                        requested.getName(),
                        requestedVersion))

                repo.createResolver().remoteAccess.listModuleVersions(data, result)

                if (result.state == BuildableModuleVersionSelectionResolveResult.State.Listed) {
                    def versionsList = result.versions.versions.findAll({ range.contains(it.version) })
                            .collect({ new Version(it.version) }).sort()

                    if (versionsList) {
                        def versionToUse = versionsList.last().version
                        logger.info "$requested.group:$requested.name using version $versionToUse"
                        requestedVersion = versionToUse
                        return
                    }
                }
            }
        }
        else{
            logger.debug("$requested is not a valid range ($requestedVersion). Not trying to resolve")
        }

        if( requestedVersion != requested.version)
            details.useVersion requestedVersion

    }

    def String resolveVersionFromManifest(DependencyResolveDetails details)
    {
        def requested = details.requested
        def ver = requested.version

        if(ver == 'auto' && (manifestFile || options && options.manifest && options.manifest.url) )
        {
            def manifest = getManifest()
            if(manifest == null)
                throw new RuntimeException("Could not resolve manifest location $options.manifest.url")
            def name = "${requested.group}:${requested.name}"
            ver =   manifest.modules[name] ?: ver
            logger.debug("Resolved version of $name to $ver")
        }

        ver
    }

    @Memoized
    private getManifest()
    {

        def location = manifestFile?.toURI()?.toString() ?: options.manifest.url
        def text = null

        if(  location.startsWith("http")) {

            def builder = new HTTPBuilder(location)

            if(options.manifest.username != null)
            {
                builder.auth.basic options.manifest.username, options.manifest.password
            }

            if(options.manifest.ignoreSSL)
                builder.ignoreSSLIssues()

            builder.request(Method.GET) { rep ->
                // executed for all successful responses:
                response.success = { resp, reader ->
                    assert resp.statusLine.statusCode == 200
                    text = reader.text
                    logger.debug("Version Manifest: $location")
                }

                response.failure = { resp ->
                    logger.error "Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}"
                }
            }
        }
        else
        {
            def url = location.toURL()
            logger.info("Version Manifest: $url")
            text = url.text
        }

        if(text == null)
            return null

        logger.debug(text)

        Yaml yaml = new Yaml()
        def manifest =  yaml.load(text)
        manifest
    }
}

