def call(Map pipelineParams) {

    pipeline {

        agent {
            label 'testintegration'
        }

        parameters {
            choice(
                name: 'STATUS',
                choices: ['success-only', 'no-error', 'none'],
                description: 'PR check status'
            )
            string(
                name: 'MERGE_OPTIONS',
                defaultValue: '-vvv --no-ask --reset --comment',
                description: 'scc merge options'
            )
        }

        stages {
            stage('Merge') {
                steps {

                    // build is in .gitignore so we can use it as a temp dir
                    sh 'mkdir build'
                    sh 'cd build && curl -sfL https://github.com/ome/build-infra/archive/master.tar.gz | tar -zxf -'
                    sh 'virtualenv build/venv && build/venv/bin/pip install scc'

                    copyArtifacts(projectName: pipelineParams.parentVersions, flatten: true,
                                  filter: pipelineParams.versionFile, target: 'build')

                    sh """
                        export BASE_REPO=${pipelineParams.baseRepo}

                        # Workaround for "unflattened" file, possibly due to matrix
                        find build/ -path "*/${pipelineParams.versionFile}" -exec cp {} build/version.tsv \\;
                        test -e build/version.tsv
                        export VERSION_LOG=${env.WORKSPACE}/build/version.tsv

                        . build/venv/bin/activate
                        if [ "${env.MERGE_PUSH_BRANCH}" != "null" ]; then
                            export PUSH_BRANCH=${env.MERGE_PUSH_BRANCH}
                        fi
                        if [ "${env.MERGE_GIT_USER}" != "null" ]; then
                            export GIT_USER=${env.MERGE_GIT_USER}
                        fi
                        export STATUS=${params.STATUS} MERGE_OPTIONS="${params.MERGE_OPTIONS}"
                        bash build/build-infra-master/recursive-merge
                    """

                    archiveArtifacts artifacts: 'build/version.tsv'
                }
            }
        }
    }
}
