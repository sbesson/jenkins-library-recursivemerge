def call(Map pipelineParams) {

    def buildInfraRepo = pipelineParams.get('buildInfraRepo', 'joshmoore')
    def buildInfraBranch = pipelineParams.get('buildInfraBranch', 'single-repo')
    def buildInfraUrl = pipelineParams.get('buildInfraUrl', 'https://github.com/${buildInfraRepo}/build-infra/archive/${buildInfraBranch}.tar.gz | tar -zxf -')
    def buildInfraPath = pipelineParams.get('buildInfraPath', 'build-infra-${buildInfraBranch}')

    copyArtifacts(projectName: pipelineParams.parentVersions, flatten: false,
                    filter: pipelineParams.versionFile, target: '.')

    // build is in .gitignore so we can use it as a temp dir
    sh 'mkdir -p build'
    sh 'cd build && curl -sfL ${buildInfraUrl} | tar -zxf -'
    sh 'virtualenv build/venv && build/venv/bin/pip install scc'

    sh """
        export BASE_REPO=${pipelineParams.baseRepo}

        # Workaround for "unflattened" file, possibly due to matrix
        find . -path "${pipelineParams.versionFile}" -exec cp {} build/version.tsv \\;
        export VERSION_LOG=${pwd()}/build/version.tsv

        . build/venv/bin/activate
        if [ "${env.MERGE_PUSH_BRANCH}" != "null" ]; then
            export PUSH_BRANCH=${env.MERGE_PUSH_BRANCH}
        fi
        if [ "${env.MERGE_GIT_USER}" != "null" ]; then
            export GIT_USER=${env.MERGE_GIT_USER}
        fi
        export STATUS=${params.STATUS} MERGE_OPTIONS="${params.MERGE_OPTIONS}"
        bash build/${buildInfraPath}/recursive-merge
    """

    archiveArtifacts artifacts: 'build/version.tsv'
}
