def call(Map pipelineParams) {

    if (pipelineParams == null) {
        pipelineParams = ["new": true]
    }

    def fork = pipelineParams.fork ?: "ome"
    def repo = pipelineParams.repo ?: "unknown"
    def url = pipelineParams.url ?: "git://github.com/${fork}/${repo}"

    checkout poll: false,
           scm: [$class: 'GitSCM',
                 branches: [[name: '*/master']],
                 doGenerateSubmoduleConfigurations: false,
                 extensions: [[$class: 'CleanCheckout'],
                              [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: false, recursiveSubmodules: true, reference: '', trackingSubmodules: false]],
                 submoduleCfg: [],
                 userRemoteConfigs: [[url: url]]]

}
