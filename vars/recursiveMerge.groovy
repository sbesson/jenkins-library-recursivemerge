#!/usr/bin/env groovy
/**
 * Copyright 2019 University of Dundee
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

/**
 * Call the generic script running the recursive merge on one repository and
 * submodules.
 *
 * This script will create a version TSV file under build/version.tsv and
 * archive it as an artifact of the Jenkins job. It can also consume a
 * version.tsv generated and archived by a single parent (non-matrix) Jenkins
 * job.  The name of the location of the parent file can be
 * specified using {@code pipelineParams.parentVersions} and
 * {@code pipelineParams.versionFile}.
 *
 * @param pipelineParams a map containing parameters that can be passed to the
 *                       recursive merge call
 */
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
    def status = params.STATUS ?: "success-only"

    if (pipelineParams.parentVersions != null) {
        // build is in .gitignore so we can use it as a temp dir
        copyArtifacts(projectName: pipelineParams.parentVersions, flatten: true,
                        filter: versionFile, target: 'build')
    } else {
        sh "mkdir -p build"
    }

    sh "cd build && curl -sfL ${buildInfraUrl} | tar -zxf -"
    sh "virtualenv build/venv && build/venv/bin/pip install scc"

    sh """
        export BASE_REPO=${baseRepo}

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
