

def call(project, version=null, server=null) {
    def artiServer = Artifactory.server (server==null ? 'configuredArtiServer' : server)

    def libraryProps = readJSON text: libraryResource('uploadArtifactory.json')
    def localProps = readJSON file: 'devops/artifactory.json'
    
    def libraryMap
    def localMap
    
    def packageName = localProps."${project}"
    boolean useBranch = true

    libraryMap = [uploadPath:libraryProps."$project"["uploadPath"]]
    localMap = [patterns:localProps."$project"["patterns"], download:localProps."$project"["download"], treehash:localProps."$project"["treehash"], branch:localProps."$project"["branch"]]

    if (localMap.branch[0] == null) {
        useBranch = true
    } else {
        useBranch = localMap.branch[0]
    }

    String path = libraryMap.uploadPath[0]
    path += path?.endsWith('/') ? '' : '/'
    if (useBranch) {
        path += "${GIT_BRANCH}"
    }
    path += path?.endsWith('/') ? '' : '/'

    String patterns = localMap.patterns[0]

    String download = localMap.download[0]
    download += download?.endsWith('/') ? '' : '/'

    String treeHashPath = localMap.treehash[0]

    downloadPackages = "${env.WORKSPACE}/${download}"

    String uploadPackages = "${env.WORKSPACE}/$patterns"

    String uploadSpec = ""
    if (treeHashPath != null) {
        String treeHash = sh(script: "git rev-parse HEAD:${treeHashPath}", returnStdout: true).trim()
        String properties = "treeHash=$treeHash"
        path += "${treeHash}/"
        uploadSpec = returnUploadSpec(uploadPackages, path, properties)
    } else {
        properties = null
        uploadSpec = returnUploadSpec(uploadPackages, path, properties)
    }
    println uploadSpec
    uploadToArtifctory(artiServer, uploadSpec)
}

def returnUploadSpec(uploadPackages, uploadTarget, downloadProperties, version=null) {
    def uploadSpec = ""
    if (properties != null) {
    uploadSpec = """{
            "files": [{
            "pattern": "$uploadPackages",
            "target": "$uploadTarget",
            "props": "$downloadProperties"
            }]
        }"""
    } else {
        uploadSpec = """{
            "files": [{
            "pattern": "$uploadPackages",
            "target": "$uploadTarget"
            }]
        }"""
    }
    return uploadSpec
}

def uploadToArtifctory(artiServer, uploadSpec) {
    didIUpload = artiServer.upload spec: uploadSpec
    println didIUpload
    println didIUpload.artifacts.size()
    didIUpload.getProperties().each { 
        println it
    }
}

