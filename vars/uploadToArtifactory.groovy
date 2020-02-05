

def call(project, version=null, server=null) {
    def artiServer = Artifactory.server (server==null ? 'cphShureArtifactory' : server)

    def libraryProps = readJSON text: libraryResource('uploadArtifactory.json')
    def localProps = readJSON file: 'devops/artifactory.json'

    def libraryMap
    def localMap

    def packageName = localProps."${project}"
    boolean useBranch = true
    boolean includeBuild = false

    libraryMap = [uploadPath:libraryProps."$project"["uploadPath"], subPath:libraryProps."$project"["subPath"], buildInfo:libraryProps."$project"["buildInfo"]]
    localMap = [patterns:localProps."$project"["patterns"], download:localProps."$project"["download"], treehash:localProps."$project"["treehash"], branch:localProps."$project"["branch"]]

    if (localMap.branch[0] == null) {
        useBranch = true
    } else {
        useBranch = localMap.branch[0]
    }

    if (libraryMap.buildInfo[0] == true) {
        includeBuild = true
    }

    String path = libraryMap.uploadPath[0]
    path += path?.endsWith('/') ? '' : '/'
    if (useBranch) {
        path += "${GIT_BRANCH}"
    }
    path += path?.endsWith('/') ? '' : '/'

    if (libraryMap.subPath[0] != null) {
        path += libraryMap.subPath[0]
    }
    path += path?.endsWith('/') ? '' : '/'

    String patterns = localMap.patterns[0]

    String download = localMap.download[0]
    download += download?.endsWith('/') ? '' : '/'

    String treeHashPath = localMap.treehash[0]

    downloadPackages = "${env.WORKSPACE}/${download}"

    String uploadSpec = ""
    if (treeHashPath != null) {
        String treeHash = sh(script: "git rev-parse HEAD:${treeHashPath}", returnStdout: true).trim()
        String properties = "treeHash=$treeHash"
        path += "${treeHash}/"
        uploadSpec = returnUploadSpec(patterns, path, properties)
    } else {
        properties = null
        uploadSpec = returnUploadSpec(patterns, path, properties)
    }
    println uploadSpec
    uploadToArtifctory(artiServer, uploadSpec, includeBuild)
}


def returnUploadSpec(uploadPackages, uploadTarget, downloadProperties, version=null) {
    def uploadSpec = ""
    uploadSpecStart = """{"files": [{"""
    uploadSpecEnd = "]}"
    uploadPackages.split(',').each { filePattern_ ->
        filePattern = filePattern_.replaceAll("\\s","")
        if (properties != null) {
            uploadSpec = """${uploadSpec}
                "pattern": "${env.WORKSPACE}/${filePattern}",
                "target": "$uploadTarget",
                "props": "$downloadProperties"
            },{"""
        } else {
            uploadSpec = """${uploadSpec}
                "pattern": "${env.WORKSPACE}/${filePattern}",
                "target": "$uploadTarget"
            },{"""
        }
    }
    if (uploadSpec?.endsWith('{')) {
        uploadSpec = uploadSpec.subSequence(0, uploadSpec.length() - 2)
    }
    finalUploadSpec = """${uploadSpecStart}${uploadSpec}${uploadSpecEnd}"""

    return finalUploadSpec
}

def uploadToArtifctory(artiServer, uploadSpec, includeBuild) {
    def buildInfo = Artifactory.newBuildInfo()
    if (includeBuild) {
        didIUpload = artiServer.upload spec: uploadSpec, buildInfo: buildInfo
        artiServer.publishBuildInfo didIUpload
    } else {
        didIUpload = artiServer.upload spec: uploadSpec
    }
    didIUpload.getProperties().each {
        println it
    }
}

