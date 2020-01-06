# Artifactory

#### To use the shared library function for uploading and downloading artifacts there are a few per-requisists

1.    Have the artifactory plugin installed and configured
1.    Have an unique name configured in resources/uploadArtifactory.json in jenkins-shared-library repository, with an upload root path
1.    Have an artifactory.json in the devops folder in your repository.

It is simple to use the upload and download functions like this in a pipeline

There are 2 separate functions uploadToArtifactory and downloadFromArtifactory, both for now takes the unique name as a parameter, and will be made to work with version numbering soon.

The functionality is, read the shared library file for the root path to where to find find or upload the files to, then add the information from the file in your local repository.

#### There are currently 4 different parameters to set in the local json file. 

1.    patterns: This the pattern of files to upload, this can be a file or a path /*
1.    download: This is the path where files are being downloaded to when using the downloadFromArtifactory function
1.    treehash: If treehash is configured the file / files will be uploaded with an extra property the git tree hash of the folder configured for treehash
1.    branch: Use branch in the upload path, this will append the GIT_BRANCH to the upload / download path. if set to true or left out from configuration