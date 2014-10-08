package com.sarhanm.versioner

/**
 * Generates a solid and snapshot version using information
 * derived from the git repository
 */
class Versioner
{
    static final DEFAULT_SOLID_BRANCH_REGEX = "master|.*release.*|.*hotfix.*"
    static final DEFAULT_HOTFIX_COMMON_BRANCH = "origin/master"

    //Git commands also used in unit tests
    static final CMD_BRANCH = "rev-parse --abbrev-ref HEAD"
    static final CMD_MAJOR_MINOR = "describe --match v* --abbrev=0"
    static final CMD_POINT = "rev-list HEAD --count"
    static final CMD_COMMIT_HASH =  "rev-parse --short HEAD"

    private cache = [:]
    private GitExecutor gitExecutor
    private EnvReader envReader
    def VersionerOptions options

    public Versioner()
    {
        this.gitExecutor = new GitExecutor()
        this.envReader = new EnvReader()
        this.options = new VersionerOptions()
    }

    public Versioner(versionerOptions, File rootDir)
    {
        this.gitExecutor = new GitExecutor(rootDir)
        this.envReader = new EnvReader()
        this.options = versionerOptions
    }

    def getHotfixNumber()
    {
        def branchName = getBranchNameRaw()
        def commitList = executeGit("log --oneline $branchName...$options.commonHotfixBranch")

        return commitList ? commitList.split('\n').length : 0
    }

    def getVersionPoint()
    {
        //Number of commits since start of time.
        String output = executeGit(CMD_POINT)

        //Default to zero if we are not able to get the point version
        if(output == null)
            return 0

        def point = output.trim()
        if(isHotfix())
            point += "." + getHotfixNumber()

        return point
    }

    def getMajorMinor()
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

    def String getBranchName()
    {
        def branchName = getBranchNameRaw()

        def prefixes = ['remote/','origin/']
        for(String prefix : prefixes)
        {
            if(branchName.startsWith(prefix))
                branchName = branchName.substring(prefix.length())
        }

        return branchName.replaceAll('/', '-').replaceAll('\\.','-')
    }

    def String getBranchNameRaw()
    {
        //Either from jenkins or travis
        def name = envReader.getBranchNameFromEnv(options.branchEnvName)

        //Dev box. Execute the actual git cmd
        if(!name)
            name = executeGit(CMD_BRANCH)

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

        if(!options.omitBranchMetadata)
            version += ".$branchName"

        if(options.snapshot)
            version += "-SNAPSHOT"
        else if(!options.omitBranchMetadata)
            version += ".$commitHash"

        return version
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
        def branchName = getBranchName()
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
        def branchName = getBranchName()
        return branchName.contains("hotfix")
    }
}