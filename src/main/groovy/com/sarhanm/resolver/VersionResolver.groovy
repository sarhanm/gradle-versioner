package com.sarhanm.resolver

import groovy.transform.Memoized
import groovy.transform.Synchronized
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

    VersionResolver(Project project, VersionResolverOptions options = null) {
        this.project = project
        this.repositories = null;
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
                        details.useVersion versionToUse
                        return
                    }
                }
            }
        }

        if( requestedVersion != requested.version)
            details.useVersion requestedVersion

    }

    def String resolveVersionFromManifest(DependencyResolveDetails details)
    {
        def requested = details.requested
        def ver = requested.version

        if(ver == 'auto' && options && options.versionManifest)
        {
            def manifest = getManifest(options.versionManifest)
            def name = "${requested.group}:${requested.name}"
            ver =   manifest.modules[name] ?: ver
        }

        ver
    }

    @Memoized
    private getManifest(def location)
    {
        //TODO: be able to handle multiple URL Schemas.
        //For now, this handles file:// and http:// and https://
        //Need it to also handle mvn://groupId:artifactId[:classifier][:type]
        // and other type of formats that make it easier to specify where the yaml file lives.

        def url = location.toURL()
        Yaml yaml = new Yaml()
        def manifest =  yaml.load(url.text)
        manifest
    }
}

