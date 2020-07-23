# Keptn Jenkins Shared Library
Jenkins shared library for integrating Keptn Use Cases with your Jenkins Pipelines

| Authors | Library Version | Keptn Version | Comment |
| ------ | ------------- | --------------| -------- |
| [@grabnerandi](https://github.com/grabnerandi), [@kristofre](https://github.com/kristofre) | 1.0 | 0.6.x | Initial Release |
| [@grabnerandi](https://github.com/grabnerandi), [@kristofre](https://github.com/kristofre) | 2.0 | 0.6.x | Better Pipeline Result Handling |
| [@grabnerandi](https://github.com/grabnerandi), [@kristofre](https://github.com/kristofre) | 2.1 | 0.6.x | Validate existing project in keptnInit |

## Watch the tutorial webinar on YouTube

As part of a Keptn Community Webinar we walked through all use cases supported by this Jenkins Shared Library (click image to play video):

[![Level Up your Jenkins with Keptn Video Tutorial](https://img.youtube.com/vi/VYRdirdjOAg/0.jpg)](https://www.youtube.com/watch?v=VYRdirdjOAg "Level Up your Jenkins with Keptn Video Tutorial")

## Pre-Requisits on Jenkins
This Jenkins Shared Library requires the following Jenkins Plugins to be installed on your Jenkins
| Jenkins Plugin | Comment | Tested Version |
| -------------- | -------- | ------------ |
| [httpRequest Plugin](https://plugins.jenkins.io/http_request/) | Uses httpRequest to make REST Calls to Keptn | Tested with 1.8.26 | 
| [Pipeline Utility Step Plugin](https://plugins.jenkins.io/pipeline-utility-steps/) | Uses readJSON, writeJSON | Tested with 2.5.0 |

## Usage
In order to use this Jenkins Shared Library simply configure it in your Global Jenkins Configuration. Here is one way of doing this by pulling it from this GitHub repo:
![](./images/jenkinsglobalconfig.png)

The library also assumes the following global variables being configured in Jenkins: KEPTN_API_TOKEN, KEPTN_BRIDGE, KEPTN_ENDPOINT
![](./images/jenkinsglobalenvs.png)
You can obtain Keptn API Token and Endpoint as explained in the Keptn doc:
```
KEPTN_ENDPOINT=https://api.keptn.$(kubectl get cm keptn-domain -n keptn -ojsonpath={.data.app_domain})
KEPTN_API_TOKEN=$(kubectl get secret keptn-api-token -n keptn -ojsonpath={.data.keptn-api-token} | base64 --decode)
```
The KEPTN_BRIDGE is the link to your keptn bridge so that the Library can generate some deep links to the bridge to give you easy access to quality gate results!

Once you have everything configured use it in your Jenkins Pipeline like this

```groovy
@Library('keptn-library@1.0')
import sh.keptn.Keptn
def keptn = new sh.keptn.Keptn()


// Initialize Keptn: "Link" it to your Jenkins Pipeline
// -------------------------------------------
// initialize keptn: will store project, service and stage in a local context file so you dont have to pass it to all other functions
keptn.keptnInit project:"yourkeptnproject", service:"yourkeptnservice", stage:"yourkeptnstage"

// initialize keptn with Shipyard: if a shipyard file is passed keptnInit will also make sure this project is created in Keptn
// This allows you to automatically create a Keptn project for your Jenkins pipeline w/o having to do anything with Keptn directly
keptn.keptnInit project:"yourkeptnproject", service:"yourkeptnservice", stage:"yourkeptnstage", shipyard:'shipyard.yaml'


// Upload your SLIs, SLOs, Test Scripts ... to Keptn
// --------------------------------------------
// If you want to fully automate the Keptn configuration you shoudl upload your sli.yaml, slo.yaml and optionally files such as your tests
// First parameter defines the file in your local Jenkins Workspace, the second one the location Keptn will use to store it in its own Git
keptn.keptnAddResources('keptn/sli.yaml','dynatrace/sli.yaml')
keptn.keptnAddResources('keptn/slo.yaml','slo.yaml')
keptn.keptnAddResources('keptn/load.jmx','jmeter/load.jmx')

// Quality Gate Evaluation Use Case
// ------------------------------------------
// Start a quality gate evaluation. There are multiple timeframe options, e.g: using timestamps or number minutes from Now()
// Example #1: Evaluate the last 10 minutes
def keptnContext = keptn.sendStartEvaluationEvent starttime:"600", endtime:"0" 

// Example #2: Evaluate the previous hour. End=Now()-3600, Start=Now()-7200
def keptnContext = keptn.sendStartEvaluationEvent starttime:"7200", endtime:"3600" 

// Example #3: Evaluate a specific timeframe
def keptnContext = keptn.sendStartEvaluationEvent starttime:"2019-06-07T07:00:00.0000Z", endtime:"2019-06-07T08:00:00.0000Z" 

// Example #4: Mark a starting timestamp before executing your tests
// Following example will fill starttime with the time when you called markEvaluationStartTime and as end is empty will default to Now()
keptn.markEvaluationStartTime
... here is where you would execute any existing tests
def keptnContext = keptn.sendStartEvaluationEvent starttime:"", endtime:"" 
echo "Open Keptns Bridge: ${keptn_bridge}/trace/${keptnContext}"


// Performance Testing as a Service Use Case
// -------------------------------------------
// If we want Keptn to execute a test against a specific URL we simply inform Keptn about a new deployment
// After Keptn executes the tests it will also evaluate the quality gate for the timeframe the test took to execute
def keptnContext = keptn.sendDeploymentFinishedEvent testStrategy:"performance", deploymentURI:"http://yourapp.yourdomain.local"
echo "Open Keptns Bridge: ${keptn_bridge}/trace/${keptnContext}"


// Progressive Delivery Use Case
// -------------------------------------------
// If you want Keptn to deploy, test and evaluate then we can simply inform Keptn about a new configuration (=container image) you have
// Typially you would use your Jenkins to build and push a container to your container registry. After that you notify Keptn about it
def keptnContext = keptn.sendConfigurationChangedEvent image:"docker.io/grabnerandi/simplenodeservice:3.0.0"
echo "Open Keptns Bridge: ${keptn_bridge}/trace/${keptnContext}"


// Waiting for Quality Gate Result
// --------------------------------------------
def result = keptn.waitForEvaluationDoneEvent setBuildResult:true, waitTime:waitTime
echo "${result}"
```

## Tutorials

If you want to see more examples go here: [Keptn Jenkins Tutorials](https://github.com/keptn-sandbox/jenkins-tutorial)
