package com.sarhanm.versioner

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import groovy.transform.Memoized

/**
 * Generates a solid and snapshot version using information
 * derived from the git repository
 */
class Versioner
{
    static final DEFAULT_SOLID_BRANCH_REGEX = "master|.*release.*|.*hotfix.*"
    static final DEFAULT_HOTFIX_COMMON_BRANCH = "master"

    //Git commands also used in unit tests
    static final CMD_BRANCH = "rev-parse --abbrev-ref HEAD"
    static final CMD_MAJOR_MINOR = "describe --match v* --abbrev=0"
    static final CMD_POINT = "rev-list HEAD --count"
    static final CMD_POINT_SOLID_BRANCH = "describe --long --match v*"
    static final CMD_COMMIT_HASH =  "rev-parse --short HEAD"

    private cache = [:]
    private GitExecutor gitExecutor
    private EnvReader envReader
    private String projectName
    private String initialVersion
    private Logger logger
    def VersionerOptions options

    public Versioner()
    {
        this.gitExecutor = new GitExecutor()
        this.envReader = new EnvReader()
        this.options = new VersionerOptions()
        // for unit testing we mock these two
        this.projectName = "testProject"
        this.initialVersion = "1.0"
        this.logger = null
    }

    public Versioner(versionerOptions, Project project)
    {
        this.gitExecutor = new GitExecutor(project)
        this.envReader = new EnvReader()
        this.options = versionerOptions
        this.projectName = project.name
        this.initialVersion = project.version
        this.logger = project.logger
    }

    @Memoized
    public String toString()
    {
        if (options.disabled)
        {
            return initialVersion
        }

        logger?.info "Initial project $projectName version: $initialVersion"

        def computedVersion = getVersion()

        //Trying to make this log line machine readable and findable in long/huge logs
        logger?.quiet "versioner:${projectName}=$computedVersion"

        computedVersion
    }

    def plus( other )
    {
        return toString() + other.toString()
    }
    /**
     * @return Hotfix version number as integer
     */
    def int getHotfixNumber()
    {
        def branchName = getBranchNameRaw()
        def commitList = executeGit("log --oneline $branchName...$options.commonHotfixBranch")

        return commitList ? commitList.split('\n').length : 0
    }

    /**
     * @return Point version number as String.  May contain both point
     * and hotfix numbers for a hotfix branch (such as "5.2")
     */
    def String getVersionPoint()
    {
        if(isHotfix())
        {
            // In the hotfix case, the point value is not the number of commits in the current branch
            //but the number of commits from when we split off.
            def branchName = getBranchNameRaw();

            def commitHash = executeGit("merge-base $branchName $options.commonHotfixBranch")
            def commitList = executeGit("log --oneline $commitHash")
            println "Commit list: $commitList"
            def point = commitList ? commitList.split('\n').length : 0
            point += "." + getHotfixNumber()
            return point
        }
        else{

            if( useSolidMajorMinorVersion() )
            {
                //The point number is the number of commits since the last v{major}.{minor} tag.
                //If no tag has been set yet, we fallback to the number of commits from the beginning of time.
                String output = executeGit(CMD_POINT_SOLID_BRANCH)

                if(output)
                {
                    def parts = output.trim().split("-")
                    if(parts.length > 1)
                        return parts[1]
                }
            }

            return totalCommits
        }
    }

    /**
     * @return Major and minor version numbers as String (such as "3.4")
     */
    def String getMajorMinor()
    {
        //We only want certain branches to have solid versions
        if(!useSolidMajorMinorVersion())
            return "0.0"

        def output = executeGit(CMD_MAJOR_MINOR)
        if(output == null)
            return "1.0"

        //Remove the v prefix
        return output.substring(1).trim()
    }

    /**
     * @return Major version number as integer
     */
    def int getMajorNumber()
    {
        def majorMinorSplit = getMajorMinor().split('\\.')
        return Integer.parseInt( majorMinorSplit[0] )
    }

    /**
     * @return Minor version number as integer
     */
    def int getMinorNumber()
    {
        def majorMinorSplit = getMajorMinor().split('\\.')
        return Integer.parseInt( majorMinorSplit[1] )
    }

    /**
     * @return Point version number as integer
     */
    def int getPointNumber()
    {
        def pointSplit = getVersionPoint().split('\\.')
        return Integer.parseInt( pointSplit[0] )
    }

    /**
     * @return Branch name without slashes or dots
     */
    def String getCleansedBranchName()
    {
        return branch.replaceAll('/', '-').replaceAll('\\.','-')
    }

    /**
     * @return Branch name without the remote and/or origin prefix
     */
    def String getBranch()
    {
        def branchName = getBranchNameRaw()

        def prefixes = ['remote/','origin/']
        for(String prefix : prefixes)
        {
            if(branchName.startsWith(prefix))
                branchName = branchName.substring(prefix.length())
        }

        branchName
    }

    @Memoized
    def String getBranchNameRaw()
    {

        //Try from git repo first, then fall back on env names
        // This is important so tests execute successfully.
        def name = executeGit(CMD_BRANCH)

        //Fallback to either from jenkins or travis
        if(!name || name == 'HEAD')
            name = envReader.getBranchNameFromEnv(options.branchEnvName)

        if(!name)
            return "unknown"

        return name.trim()
    }

    def String getCommitHash()
    {
        def output = executeGit(CMD_COMMIT_HASH)
        if(output == null)
            return "unkown-commit-hash"
        return output.trim()
    }

    /**
     *
     * @return The full version
     * major.minor.point.branch-name
     */
    def String getVersion()
    {
        def version = "$majorMinor"

        if(!options.snapshot)
            version += ".$versionPoint"

        //We now have the digit parts of the version.
        //The rest is adding git meta-data depending on user-options

        if(!omitBranchMetadata())
            version += ".$cleansedBranchName"

        if(options.snapshot)
            version += "-SNAPSHOT"
        else if(!omitBranchMetadata())
            version += ".$commitHash"

        return version
    }

    def omitBranchMetadata()
    {
        if(options.omitBranchMetadata)
            return true

        if(options.omitBranchMetadataOnSolidBranch && useSolidMajorMinorVersion())
            return true

        return false
    }

    /**
     *
     * @return total commits since the inception of the repo
     */
    def String getTotalCommits()
    {
        String output = executeGit(CMD_POINT)

        //Default to zero
        return output == null ? "0" : output.trim()
    }

    /******************/
    /**** Private *****/
    /******************/

    /**
     *
     * @return True if we are in a branch which should use a solid Major and minor numbers.
     * Otherwise we use 0.0
     */
    private useSolidMajorMinorVersion()
    {
        def branchName = getCleansedBranchName()
        return branchName.matches(options.solidBranchRegex)
    }

    /**
     * Executes a git command with the passed in args
     * @param cmdArgs
     * @return The result of the execution
     */
    private String executeGit(cmdArgs)
    {
        if (cache.containsKey(cmdArgs))
            return cache.get(cmdArgs)

        def val = gitExecutor.execute(cmdArgs)
        cache.put(cmdArgs,val)
        return val
    }

    private isHotfix()
    {
        if(options.disableHotfixVersioning)
            return false

        def branchName = getCleansedBranchName()
        return branchName.contains("hotfix")
    }
}
