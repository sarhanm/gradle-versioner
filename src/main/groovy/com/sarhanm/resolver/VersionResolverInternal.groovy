package com.sarhanm.resolver

import groovy.transform.Memoized
import groovy.transform.Synchronized
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 *
 * @author mohammad sarhan
 */
class VersionResolverInternal implements Action<DependencyResolveDetails> {

    private static final Logger logger = Logging.getLogger(VersionResolverInternal)

    private Project project
    private ConfigurationContainer configurations
    private VersionManifestOption options

    private usedVersions
    private computedManifest
    private Map<String, String> explicitVersion = [:]
    private VersionManifestResolver manifestResolver

    VersionResolverInternal(Project project,
                            VersionManifestOption manifestOptions,
                            ConfigurationContainer configurations,
                            RepositoryHandler repositoryHandler
    ) {
        this(project, manifestOptions, configurations, repositoryHandler, null);
    }


    VersionResolverInternal(Project project,
                            VersionManifestOption manifestOptions,
                            ConfigurationContainer configurations,
                            RepositoryHandler repositoryHandler,
                            Set<Project> subprojects
    ) {

        this.project = project
        this.configurations = configurations
        this.options = manifestOptions

        this.usedVersions = [:]
        this.computedManifest = ['modules': usedVersions]

        manifestResolver = new VersionManifestResolver(options)

        this.configurations?.all { Configuration c ->
            c.dependencies.all { Dependency d ->
                if (d.version != 'auto') {
                    String key = "${d.group}:${d.name}"
                    explicitVersion[key] = d.version
                    logger.debug "Adding {} to explicit versions with version {}", key, d.version
                }
            }
        }
    }

    @Override
    void execute(DependencyResolveDetails details) {

        def requested = details.requested
        def requestedVersion = resolveVersion(requested.group, requested.name, requested.version)
        def range = new VersionRange(requestedVersion)

        if (range.valid) {
            //replace with a range query so the componentselection logic kicks in.
            //TODO: restrict range to so we don't have to validate every possible version.
            requestedVersion = '+'
        } else {
            logger.debug("$requested is not a valid range ($requestedVersion). Not trying to resolve")
        }

        if (requestedVersion != requested.version) {
            details.useVersion requestedVersion
            usedVersions["${details.requested.group}:${details.requested.name}"] = requestedVersion
            logger.debug("Resolved version of ${details.requested.group}:${details.requested.name} to $requestedVersion")
        }
    }

    String resolveVersion(String group, String name, String requestedVersion) {

        // Local projects are always versioned together.
        if (isLocalProject(group, name)) {
            logger.debug("$group:$name is a sibling project. skipping version resolution")
            return requestedVersion
        }

        //it actually doesn't matter if the version is 'auto' or otherwise
        // as long as its a version found in our version manifest
        //we always pin the version based on two rules in the following order:
        // 1. if pinned in the build.gradle, then that version wins
        // 2. if pinned in the version manifest, then that version wins.

        //Only do our logic of overriding pins if we are defining them in the version manifest
        //otherwise we should depend on gradle to resolve multiple versions for the same dependency.
        if (isVersionInManifest(group, name)) {
            if (isExplicitlyVersioned(group, name)) {
                //use the version in the build.gradle
                return getExplicitVersionFromProject(group, name)
            } else {
                //resolve via the manifest. We don't care that auto is being used or not,
                //we always resolve to what is in the manifest
                requestedVersion = getVersionFromManifest(group, name)
                if (!requestedVersion)
                    throw new RuntimeException("Could not resolve manifest locations (${manifestLocations})")

            }
        }

        if (requestedVersion == "auto")
            throw new IllegalStateException("[com.sarhanm.vesion-resolver]$group:$name:$requestedVersion is " +
                    "marked for resolution but no version is defined in version manifest")

        //return the default version if not resolved by above
        return requestedVersion
    }

    String getVersionFromManifest(group, name) {
        VersionManifest manifest = resolveManifest()
        return manifest.getVersion(group, name)
    }

    boolean isVersionInManifest(group, name) {
        return getVersionFromManifest(group, name) != null
    }

    boolean isExplicitlyVersioned(String group, String name) {
        return getExplicitVersionFromProject(group, name) != null
    }

    String getExplicitVersionFromProject(String group, String name) {
        return explicitVersion.get("$group:$name" as String)
    }

    def getComputedVersionManifest() {
        return computedManifest
    }

    boolean isLocalProject(group, name) {
        project.getRootProject().getAllprojects().find { p -> p.group == group && p.name == name }
    }

    @Memoized
    private VersionManifest resolveManifest(){
        return manifestResolver.getVersionManifest(getManifestLocations())
    }

    /**
     * Gets the locations of the defined manifests.
     * Locations can be defined in a configuration dependency
     * or via the VersionResolverOptions
     * @return
     */
    @Memoized
    private List<URI> getManifestLocations() {
        List<URI> locations = []

        resolveManifestConfiguration()?.each {
            locations.add it.toURI()
        }

        if (options?.url) {
            logger.info("Resolving ${options.url}")

            if (options.url.startsWith("http")) {
                locations.add options.url.toURI()
            } else {
                def manifestFile = project.file(options.url)
                if (!manifestFile.exists()) {
                    logger.info("Not loading ${options.url} because it does not exist")
                } else {
                    logger.info("Loading ${manifestFile.toURI()}")
                    locations.add manifestFile.toURI()
                }
            }
        }

        locations
    }

    /**
     * Resolves the versionManifest into a list of files.
     * @return List of files that contain versioning information
     */
    private List<File> resolveManifestConfiguration() {
        def versionManifest = configurations?.findByName(VersionResolutionPlugin.VERSION_MANIFEST_CONFIGURATION)
        def resolved = versionManifest?.resolve()
        resolved && !resolved.empty ? new ArrayList<>(resolved) : null
    }
}

