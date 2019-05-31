import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

public class RecursiveMergeSpec extends JenkinsPipelineSpecification {

    def recursiveMerge = null

    def setup() {
        recursiveMerge = loadPipelineScriptForTest("vars/recursiveMerge.groovy")
        explicitlyMockPipelineStep('copyArtifacts')
        explicitlyMockPipelineStep('pwd')
    }

    def "default environment variables are set" () {
        when:
            recursiveMerge()
        then:
            1 * getPipelineMock("pwd")()
            1 * getPipelineMock("copyArtifacts")({it =~ /version.tsv/})
            1 * getPipelineMock("sh")({it =~ /mkdir/})
            1 * getPipelineMock("sh")({it =~ /joshmoore/}) // real test
            1 * getPipelineMock("sh")({it =~ /venv/})
            1 * getPipelineMock("sh")({it =~ /BASE_REPO/})
            1 * getPipelineMock("env.getProperty").call('MERGE_GIT_USER')
            1 * getPipelineMock("env.getProperty").call('MERGE_PUSH_BRANCH')
            1 * getPipelineMock("params.getProperty").call('MERGE_OPTIONS')
            1 * getPipelineMock("params.getProperty").call('STATUS')
    }

}
