package com.sarhanm.versioner

/**
 * Generates a solid and snapshot version using information
 * derived from the git repository
 */
class Versioner
{
    private static final DEFAULT_SOLID_BRANCH_REGEX = "master|.*release.*|.*hotfix.*"
    private static final DEFAULT_HOTFIX_COMMON_BRANCH = "origin/master"

    //Git commands also used in unit tests
    static final CMD_BRANCH = "rev-parse --abbrev-ref HEAD"
    static final CMD_MAJOR_MINOR = "describe --match v* --abbrev=0"
    static final CMD_POINT = "rev-list HEAD --count"
    static final CMD_COMMIT_HASH =  "rev-parse --short HEAD"

    private cache = [:]
    private GitExecutor gitExecutor
    private EnvReader envReader
    private solidBranchRegex;
    private commonHotfixBranch

    public Versioner()
    {
        this(DEFAULT_SOLID_BRANCH_REGEX,DEFAULT_HOTFIX_COMMON_BRANCH)
    }

    public Versioner(String solidBranchRegex, String commonHotfixBranch)
    {
        this.solidBranchRegex = solidBranchRegex
        this.commonHotfixBranch = commonHotfixBranch
        this.gitExecutor = new GitExecutor()
        this.envReader = new EnvReader()
    }

    def getHotfixNumber()
    {
        def branchName = getBranchNameRaw()
        def commitList = executeGit("log --oneline $branchName...$commonHotfixBranch")

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
        if(!useSolidVersion())
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
        def name = envReader.getBranchNameFromEnv()

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
        return getMajorMinor() + '.' + getVersionPoint()+'.'+getBranchName() + '.' + getCommitHash()
    }

    /**
     *
     * @return Snapshot version. This removes the point part of the version string
     * as it can not be used with a snapshot version
     */
    def String getSnapshotVersion()
    {
        return getMajorMinor() + '.'+getBranchName()+'-SNAPSHOT'
    }

    /******************/
    /**** Private *****/
    /******************/
    private useSolidVersion()
    {
        def branchName = getBranchName()

        return branchName.matches(solidBranchRegex)
//        return  branchName == "master"  ||
//                branchName.startsWith("release") ||
//                branchName.startsWith("hotfix")
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