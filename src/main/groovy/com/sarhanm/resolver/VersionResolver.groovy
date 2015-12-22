package com.sarhanm.resolver

import groovy.transform.Memoized
import groovy.transform.Synchronized
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository
import org.gradle.api.logging.Logging
import org.gradle.internal.Transformers
import org.gradle.internal.component.model.DefaultDependencyMetaData
import org.gradle.internal.component.model.DependencyMetaData
import org.gradle.internal.resolve.result.DefaultBuildableModuleVersionListingResolveResult
import org.gradle.util.CollectionUtils

//import org.gradle.internal.resolve.result.DefaultBuildableModuleComponentVersionSelectionResolveResult
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
    private usedVersions
    private computedManifest
    private siblingProject = new HashSet();

    VersionResolver(Project project,VersionResolverOptions options = null, File manifestFile = null) {
        this.project = project
        this.repositories = null;
        this.manifestFile = manifestFile
        this.options = options

        this.usedVersions = [:]
        this.computedManifest = ['modules': usedVersions ]

        project?.getParent()?.subprojects?.each { siblingProject.add("${it.group}:${it.name}"); }

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
            requestedVersion = resolveVersion(requested.group,requested.name,requestedVersion,range)
        }
        else{
            logger.debug("$requested is not a valid range ($requestedVersion). Not trying to resolve")
        }

        if( requestedVersion != requested.version)
        {
            details.useVersion requestedVersion
            usedVersions["${details.requested.group}:${details.requested.name}"] = requestedVersion
        }

    }

    def resolveVersion(String group,String name, String version, VersionRange range)
    {
        logger.debug "$group:$name:$version is a valid range. Trying to resolve"
        def versionToReturn = version
        getRepos().each { repo ->
            if(versionToReturn != version)
                return

            def resolver = repo.createResolver()

            //We don't want to include the local repo if we are trying to re-fresh dependencies
            if(this.project.gradle.startParameter.refreshDependencies && resolver.local)
            {
                return
            }


            def result = new DefaultBuildableModuleVersionListingResolveResult()

            DependencyMetaData data = new DefaultDependencyMetaData(new DefaultModuleVersionIdentifier(
                    group,
                    name,
                    version))

            if( resolver.local)
                resolver.localAccess.listModuleVersions(data, result)
            else
                resolver.remoteAccess.listModuleVersions(data, result)

            if (result.hasResult()) {
                def latestVersion = null
                result.versions.each{

                    //NOTE: we could use the sort and other cool grooyish methods,
                    // but this should be a lot more efficient as a single pass
                    if(range.contains(it)) {
                        def current= new Version(it)
                        if (latestVersion == null)
                            latestVersion = current
                        else if (latestVersion.compareTo(current) < 0)
                            latestVersion = current
                    }
                }

                if (latestVersion) {
                    logger.info "$group:$name using version $latestVersion.version"
                    versionToReturn = latestVersion.version
                }
            }
        }

        return versionToReturn
    }

    def String resolveVersionFromManifest(DependencyResolveDetails details)
    {
        def requested = details.requested
        def ver = requested.version
        def group = requested.group
        def rname = requested.name
        def isExplicit = isExplicitlyVersioned(group,rname)

        // Siblings are always versioned together.
        if(siblingProject.contains("${group}:${rname}"))
            return ver


        //We don't want to set versions for dependencies explicitly set in this project.
        if(ver != 'auto' && isExplicit)
            return ver

        if( manifestFile || options && options.manifest && options.manifest.url)
        {
            def manifest = getManifest()
            if(manifest == null)
                throw new RuntimeException("Could not resolve manifest location $options.manifest.url")
            def name = "${group}:${rname}"
            ver =   manifest.modules[name] ?: ver
            logger.debug("Resolved version of $name to $ver")
        }

        ver
    }

    @Memoized
    private boolean isExplicitlyVersioned(String group, String name)
    {
        def explicit = false
        project?.configurations?.all{

            dependencies.each{
                if(it.name == name && it.group == group)
                {
                    explicit = true
                    return explicit
                }
            }
        }

        return explicit
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

    def getComputedVersionManifest()
    {
        return computedManifest
    }

}

