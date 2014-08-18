
# Groovey Versioner

### What is it?

For gradle projects, this groovy project can generate a version based on git tags and other pieces of information from the git repo. 

Example of a gradle.build file:

    import com.coinfling.versioner.CoinflingVersioner
    def versioner = new CoinflingVersioner()

    group = 'com.foobar.example'
    version = versioner.getVersion()

    #We can also keep things at snapshot versions
    version = versioner.getSnapshotVersion()

Because the version is derived from the state of your git repo, you don't have to worry about maintaining the version value in your gradle.build file as you merge and/or branch. It'll always output the correct value.

### Scheme

The version scheme:

#### For branch names master, release/.* and hotfix/.*

    {major}.{minor}.{#-of-commits}.{hotfix-number}.{branch-name}.{short-commit-hash}
 
#### All other branches

    0.0.{#-of-commits}.{hotfix-number}.{branch-name}.{short-commit-hash}

NOTE: in this case, we namespace out the version string and prepend with 0.0

#### Snapshot versions:

master, release/.* and hotfix/.*
    
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
 
This can be configured by passing in the regex that matches solid versions to the Versioner constructor.
 
### {#-of-commits}:
 
The is the number of commits since the inception of the git repo.
  
### {hotfix-number}:

This is the number of commits in the current branch that do not yet exist in the master branch. This assumes that hotfix branches are forked from master. 

This is configurable by passing in the "common" branch in the Versioner constructor.

This number ONLY exists if the branch name contains the word "hotfix"

### {branch-name}:

This is the branch name of the repo. 

Branch name is cleansed to remove 'origin/' and 'remote/' and converts '/' and '.' to '-'

On build boxes, the branch name is derived from environment variables.
Jenkins --> $GIT_BRANCH
Travis --> $TRAVIS_BRANCH

### {short-commit-hash}

This is the short commit hash. Super useful to look at an artifact and know how to get to it to start a hotfix or developmet.

