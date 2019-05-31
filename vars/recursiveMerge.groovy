def call(Map pipelineParams) {

    if (pipelineParams == null) {
        pipelineParams = ["new": true]
    }

    def buildInfraRepo = pipelineParams.buildInfraRepo ?: "ome"
    def buildInfraBranch = pipelineParams.buildInfraBranch ?: "master"
    def buildInfraUrl = pipelineParams.buildInfraUrl ?: "https://github.com/${buildInfraRepo}/build-infra/archive/${buildInfraBranch}.tar.gz"
    def buildInfraPath = pipelineParams.buildInfraPath ?: "build-infra-${buildInfraBranch}"

    def baseRepo = pipelineParams.baseRepo ?: "unknown.git"
    def versionFile = pipelineParams.versionFile ?: "build/version.tsv"

    // environment
    def currentDir = pwd()
    def pushBranch = env.MERGE_PUSH_BRANCH
    def gitUser = env.MERGE_GIT_USER
    def mergeOptions = params.MERGE_OPTIONS
    def status = params.STATUS

    // build is in .gitignore so we can use it as a temp dir
    copyArtifacts(projectName: pipelineParams.parentVersions, flatten: false,
                    filter: versionFile, target: 'build')

    sh "cd build && curl -sfL ${buildInfraUrl} | tar -zxf -"
    sh "virtualenv build/venv && build/venv/bin/pip install scc"

    sh """
        export BASE_REPO=${baseRepo}

        # Workaround for "unflattened" file, possibly due to matrix
        find . -path "build/${versionFile}" -exec cp {} build/version.tsv \\;
        export VERSION_LOG=${currentDir}/build/version.tsv

        . build/venv/bin/activate
        if [ "${pushBranch}" != "null" ]; then
            export PUSH_BRANCH=${pushBranch}
        fi
        if [ "${gitUser}" != "null" ]; then
            export GIT_USER=${gitUser}
        fi
        export STATUS=${status} MERGE_OPTIONS="${mergeOptions}"
        bash build/${buildInfraPath}/recursive-merge
    """

    archiveArtifacts artifacts: 'build/version.tsv'
}
