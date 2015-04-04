# Gradle Versioner

### What is it?

For gradle projects, this gradle plugin can generate a version based on git tags and other pieces of information from the git repo. Because the version is derived from the state of your git repo, you don't have to worry about maintaining the version value in your gradle.build file as you merge and/or branch; it'll always output the correct value.

Simple Example:
    
    # gradle.build
    apply plugin: 'com.sarhanm.versioner'
    
Example using versioner options:

    #gradle.build

     // Note: order is important.
    //Options must be specified before plugin is applied
    import com.sarhanm.versioner.VersionerOptions
    project.extensions.create('versioner', VersionerOptions)
    versioner{
        snapshot = true
    }
    apply plugin: 'com.sarhanm.versioner'

Look at [com.sarhanm.versioner.VersionerOptions](../src/main/groovy/com/sarhanm/versioner/VersionerOptions.groovy) for a complete list of options.

NOTE: Because of [GRADLE-2407](https://issues.gradle.org/browse/GRADLE-2407) you cannot apply this plugin in any initializing scripts (scripts found in init.d).
This is a common case for most companies. If this is something you need, please vote up the issue.

### Scheme

The version scheme:

#### For branch names master, release/.* and hotfix/.*

    {major}.{minor}.{#-of-commits}.{hotfix-number}.{branch-name}.{short-commit-hash}
 
#### All other branches

    0.0.{#-of-commits}.{hotfix-number}.{branch-name}.{short-commit-hash}

NOTE: in this case, we namespace out the version string and prepend with 0.0. More on this and how to configure it below. 

#### Snapshot versions for master, release/.* and hotfix/.*
    
    {major}.{minor}.{branch-name}-SNAPSHOT

#### Snapshot versions for all other branches

    0.0.{branch-name}-SNAPSHOT    

### {major}.{minor}:

The major and minor parts of the version are derived from the newest git tag that starts with a 'v'. 
 
Example tags:
    v1.0
    v2.3

This value will be used only for branch names "master", "hotfix/.*" and "release/.*"

All other branches get a major.minor of '0.0'. This helps keep the version scheme uploaded to nexus clean (or any other artifact repository ).
 
This can be configured via [com.sarhanm.versioner.VersionerOptions:solidBranchRegex](../src/main/groovy/com/sarhanm/versioner/VersionerOptions.groovy), so you could configure the plugin to always use the git tag for all branches. 
 
### {#-of-commits}:
 
The is the number of commits since the inception of the git repo.
  
### {hotfix-number}:

This is the number of commits in the current branch that do not yet exist in the master branch. This assumes that hotfix branches are forked from master. 

This is configurable via [com.sarhanm.versioner.VersionerOptions:commonHotfixBranch](../src/main/groovy/com/sarhanm/versioner/VersionerOptions.groovy)

This number ONLY exists if the branch name contains the word "hotfix"

### {branch-name}:

This is the branch name of the repo. 

Branch name is cleansed to remove 'origin/' and 'remote/' and converts '/' and '.' to '-'

On build boxes, the branch name is derived from environment variables.
Jenkins --> $GIT_BRANCH
Travis --> $TRAVIS_BRANCH

For other build systems, you'll need to configure the env key that contains the branchName. Use the [com.sarhanm.versioner.VersionerOptions:branchEnvName](../src/main/groovy/com/sarhanm/versioner/VersionerOptions.groovy)

NOTE: The reason we use environment variables to determine the branch name is that most build systems checkout a repo at a commit, which is a detached head. In this case, we have no way to determine what the branch name is without the help of the build system.

### {short-commit-hash}

This is the short commit hash. Super useful to look at an artifact and know how to get to it to start a hotfix or developmet.

## NOTE:
Travis CI has a default git depth of 50. This means that your {#-of-commits} will stay at 50 unless you change the depth of your clone to something higher.

```
git:
  depth: 100
```
