
This library runs git commands and builds a version of the following scheme

    {major}.{minor}.{#-of-commits}.{hotfix-number}.{branch-name}.{short-commit-hash}
 
 {major}.{minor}: 
 The major and minor parts of the version are derived from the newest git tag that starts with a 'v'. 
 
 Example tags:
 v1.0
 v2.3
 
 
 The major.minor version is used for the master branch, any branch with the word 'release' or 'hotfix'. 
 All other branches get a major.minor of '0.0'. This helps keep the version scheme uploaded to nexus clean.
 
 This can be re-configured by passing in the regex that matches solid versions to the Versioner constructor
 
 {#-of-commits}:
 
 The is the number of commits since the inception of the git repo.
  
{hotfix-number}:

This is the number of commits in the current branch that do not yet exist in the master branch. This assumes that hotfix branches are branches from master. 
This is configurable by passing in the "common" branch in the Versioner constructor.


{branch-name}:

This is the branch name of the repo. 

Branch name is cleansed to remove 'origin/' and 'remote/' and Converts '/' and '.' to '-'

On th
