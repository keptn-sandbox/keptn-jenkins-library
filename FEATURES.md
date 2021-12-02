# Features

This document describes a list of use-cases and features that this library provides.

## Use Cases

The following are the core use cases that this integration supports. If you have another use case, please let us know by creating an Issue.

* UC 1: As a user, I want to [perform a release validation](https://keptn.sh/docs/concepts/performance_validation/#test-automation-process) by triggering a [Keptn quality gate](https://keptn.sh/docs/concepts/quality_gates/) evaluation from within Jenkins
* UC 2: As a user, I want to trigger a delivery with Keptn from within Jenkins
* UC 3: As a user, I want to create a project, a service, and push files to the Keptn configuration repo
* UC 4: As a user, I want to trigger an existing CI/CD Jenkins Pipeline from Keptn (via webhook-service) 


## Stories

Each use case consists of multiple stories, however, a story can be part of many use cases.

### User Story 1: Install keptn-jenkins-library in Jenkins

**Goal**: A user should be able to install the library within their Jenkins installation.

**DoD**:
* README contains installation instructions with screenshots
* README contains compatibility matrix


### User Story 2: Connect to the Keptn API

**Goal**: A user should be able to connect to their existing Keptn installation.

**DoD**:
* README contains instructions on how to configure secrets / environment variables
* Shared library code contains helper functions to read secrets / environment variables


### User Story 3: Create a project with shipyard

**Goal**: A user should be able to create a project (defined by a shipyard file) and a service.

The current implementation allows to call keptnInit with a project, service and stage, as well as a shipyard file.
If the project already exists, skip creation of the project and uploading the shipyard file.
If the service already exists, skip creation of the service.

**DoD**:
* Shared library code contains function `initProject` (or similar) that can be called from within a Jenkinsfile to create a project, service, and upload shipyard file
* README contains an example of how to call this function within a Jenkinsfile


### User Story 4: Add files / resources

**Goal**: A user should be able to add files to the Keptn configuration repo (project resource, stage resource, service resource)

**Notes**: In Keptn, some files are added as "global" resources for the project, while some others are added per stage or per service.

**DoD**:
* Shared library code contains functions `addResource`, `addProjectResource`, `addStageResource`, `addServiceResource` (or similar) that can be used to upload a file to Keptn (e.g., SLI, SLO files)
* README contains an example of how to call these functions within a Jenkinsfile

### User Story 5: Mark start-time of testing

**Goal**: A user should be able to mark start-time of a test within their Jenkinsfile, in order to use the datetime for an evaluation afterwards.

**DoD**:
* Shared library code contains function `markEvaluationStartTime` which takes the current datetime and stores it
* README contains an example of how to call this function within a Jenkinsfile (e.g., before tests start)


### User Story 6: Trigger an evaluation

**Goal**: A user should be able to trigger an evaluation within their Jenkinsfile.

**DoD**:
* Shared library code contains a function to trigger an evaluation in Keptn
* Start-time as stored by `markEvaluationStartTime` is used; end-time = now()
* README contains an example of how to call this function within a Jenkinsfile (e.g., after tests)
* Evaluation is triggered within Keptn (verifiable in Keptn Bridge)


### User Story 7: Labels to the evaluation event

**Goal**: A user should be able to add labels to the evaluation event that is sent for triggering an evaluation. Also, certain default labels should be added, e.g.:

```
        |    "labels": {
        |      "jobname" : "${JOB_NAME}",
        |      "buildNumber": "${BUILD_NUMBER}",
        |      "joburl" : "${BUILD_URL}"
        |    },
```

**DoD**:
* Shared library code for triggering an evaluation contains default labels
* Shared library code for triggering an evaluation takes labels as parameter, and merges it with default labels
* README contains an example of how to call this function within a Jenkinsfile
* Labels appear in Keptn Bridge (verifiable in Keptn Bridge)


### User Story 8: Wait for evaluation.finished

**Goal**: A user should be able to wait for Keptn to generate the evaluation.finished event, and put the result as a Jenkins pipeline result.

**Example**:

```
    stage('Wait for Result') {
        echo "Waiting until Keptn is done and returns the results"
        def result = keptn.waitForEvaluationFinishedEvent setBuildResult:true, waitTime:5
        echo "${result}"
    }
```


**DoD**:
* Shared library code contains a function to wait for an evaluation.finished event (based on the shkeptncontext of evaluation.triggered)
* Function can be configured with a waitTime
* Function can be configured to set a build result for the Jenkins Pipeline
* README contains an example of how to call this function within a Jenkinsfile


### User Story 9: Trigger a delivery

**Goal**: A user should be able to trigger a delivery within their Jenkinsfile.

**Example**:
```
    stage('Docker build/push') {
      docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
        def app = docker.build("${image_name}:${commit_id}", '.').push()
      }
    }

    stage('Trigger Delivery') {
        def keptnContext = keptn.sendDeliveryTriggeredEvent image:"${image_name}:${commit_id}"
    }
```

**DoD**:
* Shared library code contains a function to trigger a delivery in Keptn
* Shared library code for triggering a delivery takes a container/image as a parameter
* README contains an example of how to call this function within a Jenkinsfile
* Delivery is triggered within Keptn (verifiable in Keptn Bridge)


### User Story 10: Call Jenkins pipeline from Keptn

**Goal**: A user should be able to call a Jenkins pipeline from Keptn.

**Notes**: This is handled via [Keptn webhook-service](https://keptn.sh/docs/0.10.x/integrations/webhooks/).

**DoD**:
* Example/documentation provided (e.g., in Keptn docs or on Keptn artifacthub) how to call Jenkins from Keptn.


### User Story 11: Send an arbitrary .finished event to Keptn

**Goal**: A user should be able send an arbitrary .finished event back to Keptn

**Example**: When a user calls a Jenkins pipeline from within Keptn, they need to be able to reply with a `.finished` event eventually:

```
    stage('Test') {
        nodejs(nodeJSInstallationName: 'nodejs') {
        sh 'npm install --only=dev'
        sh 'npm test'
        }
    }

    stage('Send Finished Event Back to Keptn') {
        // Send Finished Event back
        def keptnContext = keptn.sendFinishedEvent eventType: "test", keptnContext: "${params.shkeptncontext}", triggeredId: "${params.triggeredid}", result:"pass", status:"succeeded", message:"jenkins tests succeeded"
    }
```

**DoD**:
* Shared library code contains a function to send a finished event to Keptn
* README contains an example of how to call this function within a Jenkinsfile
* Finished event appears in Keptn Bridge (check for source="jenkins-library")

