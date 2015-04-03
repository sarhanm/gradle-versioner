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

    /**
     * @return Hotfix version number as integer
     */
    public int getHotfixNumber()
    {
        def branchName = getBranchNameRaw()
        def commitList = executeGit("log --oneline $branchName...$options.commonHotfixBranch")

        return commitList ? commitList.split('\n').length : 0
    }

    /**
     * @return Point version number as String.  May contain both point
     * and hotfix numbers for a hotfix branch (such as "5.2")
     */
    public String getVersionPoint()
    {
        if(isHotfix())
        {
            // In the hotfix case, the point value is not the number of commits in the current branch
            //but the number of commits from when we split off.
            def branchName = getBranchNameRaw();

            def commit_hash = executeGit("merge-base $branchName $options.commonHotfixBranch")
            def commitList = executeGit("log --oneline $commit_hash")

            def point = commitList ? commitList.split('\n').length : 0
            point += "." + getHotfixNumber()
            return point
        }
        else{
            //Number of commits since start of time.
            String output = executeGit(CMD_POINT)

            //Default to zero if we are not able to get the point version
            return output == null ? "0" : output.trim()
        }
    }

    /**
     * @return Major and minor version numbers as String (such as "3.4")
     */
    public String getMajorMinor()
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
    public int getMajorNumber()
    {
        def majorMinorSplit = getMajorMinor().split('\\.')
        return Integer.parseInt( majorMinorSplit[0] )
    }

    /**
     * @return Minor version number as integer
     */
    public int getMinorNumber()
    {
        def majorMinorSplit = getMajorMinor().split('\\.')
        return Integer.parseInt( majorMinorSplit[1] )
    }

    /**
     * @return Point version number as integer
     */
    public int getPointNumber()
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