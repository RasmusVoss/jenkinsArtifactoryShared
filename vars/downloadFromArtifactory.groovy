// Will read from the library resource and devops/artifactory.json file in the workspace
// will fail if the file is missing.


def call(project, version=null, server=null) {
    def artiServer = Artifactory.server (server==null ? 'cphShureArtifactory' : server)

    def libraryProps = readJSON text: libraryResource('uploadArtifactory.json')
    def localProps = readJSON file: 'devops/artifactory.json'

    def libraryMap
    def localMap

    def packageName = localProps."${project}"

    boolean useBranch = true

    libraryMap = [uploadPath:libraryProps."$project"["uploadPath"], subPath:libraryProps."$project"["subPath"], buildInfo:libraryProps."$project"["buildInfo"]]
    localMap = [patterns:localProps."$project"["patterns"], download:localProps."$project"["download"], treehash:localProps."$project"["treehash"], branch:localProps."$project"["branch"]]

    if (localMap.branch[0] == null) {
        useBranch = true
    } else {
        useBranch = localMap.branch[0]
    }

    String path = libraryMap.uploadPath[0]
    String patterns = localMap.patterns[0]

    String download = localMap.download[0]
    download += download?.endsWith('/') ? '' : '/'

    String treeHashPath = localMap.treehash[0]

    if (useBranch) {
        repoPath = "${path}${GIT_BRANCH}"
    } else {
        repoPath = "${path}"
    }
    repoPath += repoPath?.endsWith('/') ? '' : '/'

    if (libraryMap.subPath[0] != null) {
         repoPath += libraryMap.subPath[0]
    }

    repoPath += repoPath?.endsWith('/') ? '' : '/'
    downloadPackages = "${env.WORKSPACE}/${download}"
    String downloadSpec = ""
    if (treeHashPath != null) {
        String treeHash = sh(script: "git rev-parse HEAD:${treeHashPath}", returnStdout: true).trim()
        String properties = "treeHash=$treeHash"
        repoPath += "${treeHash}/"
        downloadSpec = returnDownloadSpec(repoPath, downloadPackages, properties)
    } else {
        properties = null
        downloadSpec = returnDownloadSpec(repoPath, downloadPackages, properties)
    }
    println downloadSpec
    downloadFromArtifactory(artiServer, downloadSpec)
}

def returnDownloadSpec(repoPath, downloadPackages, downloadProperties) {
    def downloadSpec = ""
    if (properties != null){
        downloadSpec = """{
            "files": [{
            "pattern": "$repoPath",
            "target": "$downloadPackages",
            "props": "$downloadProperties",
            "flat": "true"
            }]
        }"""
    } else {
        downloadSpec = """{
            "files": [{
            "pattern": "$repoPath",
            "target": "$downloadPackages",
            "flat": "true"
            }]
        }"""
    }
    return downloadSpec
}

def downloadFromArtifactory(artiServer, downloadSpec) {
    didIDownload = artiServer.download spec: downloadSpec
    if (didIDownload.dependencies.size() > 0) {
      return true
    } else {
      return false
    }
    //  Use this for finding properties on a groovy object
    // println didIDownload.getProperties().toString()
}
