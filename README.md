# Keptn Jenkins Shared Library
![GitHub release (latest by date)](https://img.shields.io/github/v/release/keptn-sandbox/keptn-jenkins-library)

Jenkins shared library for integrating Keptn Use Cases within your Jenkins Pipelines.


## Compatibility matrix

You can find out the latest release on the [GitHub releases](https://github.com/keptn-sandbox/keptn-jenkins-library/releases) page.

| Library Version | Keptn Version\* | Comment                                                           |
|-----------------|-----------------|-------------------------------------------------------------------|
| 1.0             | 0.6.x           | Initial Release                                                   |
| 2.0             | 0.6.x           | Better Pipeline Result Handling                                   |
| 2.1             | 0.6.x           | Validate existing project in keptnInit                            |
| 2.2             | 0.6.x           | Adding custom label support for Keptn 0.6.x                       |
| 3.0             | 0.7.x           | Supporting 0.7.0 API Endpoints                                    |
| 3.1             | 0.7.x           | Sending *buildId* label to Keptn                                  |
| 3.2             | 0.7.x           | Adding custom label support for Keptn 0.7.x                       |
| 3.3             | 0.7.x           | Improved Evaluation done event handling in Keptn                  |
| 3.4             | 0.7.x           | Implementing #9 to customize image and tag                        |
| 3.5             | 0.7.x           | Keptn API Token now configurable via Jenkins Credentials          |
| 4.0             | 0.8.0           | Now supporting Keptn 0.8.0                                        |
| 4.1             | 0.8.x, 0.9.x    | Supporting Keptn 0.9.x, bug fixes                                 |
| 5.0             | 0.9.x, 0.10.0   | Supporting Keptn 0.10.0, bug fixes, Cleanups                      |
| 5.1             | 0.9.x, 0.10.0   | Bug fixes                                                         |
| 6.0.0           | 0.10.0 - 0.12.0 | Supporting Keptn 0.12.0, added `keptnConfigureMonitoring` command |

\* **Note**: This integration has been implemented and tested with the mentioned Keptn Versions in mind. It might be compatible with newer versions, but some aspects might not work as expected.

Please make sure to always specify a version when including the library in your Jenkinsfile, e.g.
```groovy
@Library('keptn-library@6.0.0')
import sh.keptn.Keptn
def keptn = new sh.keptn.Keptn()
```

## Watch the tutorial webinar on YouTube

As part of a Keptn Community Webinar we walked through all use cases supported by this Jenkins Shared Library (click image to play video):

[![Level Up your Jenkins with Keptn Video Tutorial](https://img.youtube.com/vi/VYRdirdjOAg/0.jpg)](https://www.youtube.com/watch?v=VYRdirdjOAg "Level Up your Jenkins with Keptn Video Tutorial")

## Pre-Requisites on Jenkins
This Jenkins Shared Library requires the following Jenkins Plugins to be installed on your Jenkins
| Jenkins Plugin | Comment | Tested Version |
| -------------- | -------- | ------------ |
| [httpRequest Plugin](https://plugins.jenkins.io/http_request/) | Uses httpRequest to make REST Calls to Keptn | Tested with 1.8.26 | 
| [Pipeline Utility Step Plugin](https://plugins.jenkins.io/pipeline-utility-steps/) | Uses readJSON, writeJSON | Tested with 2.5.0 |
| [Credentials Plugin](https://plugins.jenkins.io/credentials/) | Uses CredentialsProvider (installed by default) | Tested with 2.3.15 |
| [Plain Credentials Plugin](https://plugins.jenkins.io/plain-credentials/) | Uses StringCredentials (installed by default) |Tested with 1.7 |

## Usage
In order to use this Jenkins Shared Library simply configure it in your Global Jenkins Configuration. Here is one way of doing this by pulling it from this GitHub repo:
![](./images/jenkinsglobalconfig.png)

The library also needs the following variables to be set. They can be configured in multiple ways. Order of precedence is the order they are listed in (e.g. KEPTN_ENDPOINT configured as argument and as global variable, argument takes precedence)
* KEPTN_ENDPOINT (argument in initKeptn or as global variable)
* KEPTN_BRIDGE (argument in initKeptn or as global variable)
* KEPTN_API_TOKEN (global variable or as 'Secret Text' credential)

Configuration as additional arguments in `keptnInit`:
```groovy
keptn.keptnInit keptn_endpoint:"https://api.keptn...", keptn_bridge:"https://bridge.keptn...", ...
```

Configuration as 'Secret Text' credential:

![](./images/jenkinssecrettextcredential.png)

Configuration as global variable:

![](./images/jenkinsglobalenvs.png)

You can obtain Keptn API Token and Endpoint as explained in the Keptn doc:
```
KEPTN_ENDPOINT=https://api.keptn.$(kubectl get cm keptn-domain -n keptn -ojsonpath={.data.app_domain})
KEPTN_API_TOKEN=$(kubectl get secret keptn-api-token -n keptn -ojsonpath={.data.keptn-api-token} | base64 --decode)
```
The KEPTN_BRIDGE is the link to your Keptn bridge so that the Library can generate some deep links to the bridge to give you easy access to quality gate results!

Once you have everything configured use it in your Jenkins Pipeline like this

```groovy
@Library('keptn-library@6.0.0')
import sh.keptn.Keptn
def keptn = new sh.keptn.Keptn()


// Initialize Keptn: "Link" it to your Jenkins Pipeline
// -------------------------------------------
// initialize keptn: will store project, service and stage in a local context file so you don't have to pass it to all other functions
keptn.keptnInit project:"yourproject", service:"yourservice", stage:"yourstage"

// initialize keptn with Shipyard: if a shipyard file is passed keptnInit will also make sure this project is created in Keptn
// This allows you to automatically create a Keptn project for your Jenkins pipeline w/o having to do anything with Keptn directly
keptn.keptnInit project:"yourproject", service:"yourservice", stage:"yourstage", shipyard:'shipyard.yaml'


// Upload your Monitoring Configuration, SLIs and SLOs to Keptn
// --------------------------------------------
// If you want to fully automate the Keptn configuration you should upload your sli.yaml, slo.yaml and optionally files such as your tests
// First parameter defines the file in your local Jenkins Workspace, the second one the location Keptn will use to store it in its own Git
keptn.keptnAddResources('keptn/sli.yaml','dynatrace/sli.yaml')
keptn.keptnAddResources('keptn/slo.yaml','slo.yaml')
keptn.keptnAddResources('keptn/dynatrace.conf.yaml','dynatrace/dynatrace.conf.yaml')
// OR in case you use prometheus:
// keptn.keptnAddResources('keptn/sli.yaml','prometheus/sli.yaml')

// Upload Test Scripts
keptn.keptnAddResources('keptn/load.jmx','jmeter/load.jmx')

// Configure monitoring for your keptn project (using dynatrace or prometheus)
keptn.keptnConfigureMonitoring monitoring:"dynatrace"
// OR in case you use prometheus:
// keptn.keptnConfigureMonitoring monitoring:"prometheus"

// Custom Labels
// all keptn.send** functions have an optional parameter called labels. It is a way to pass custom labels to the sent event
def labels=[:]
labels.put('TriggeredBy', 'Andi')


// Send Finished Event Use Case
// ------------------------------------------
// Send back a finished event to keptn for any triggered task which was handled by Jenkins
// keptn.sendFinishedEvent functions have optional event type payload, depending on the type of an event

// Example #1: Send a finished Event for a test task
def eventTypePayload=[:]
eventTypePayload.put('start', '2019-06-07T07:00:00.0000Z')
eventTypePayload.put('end', '2019-06-07T08:00:00.0000Z')
def keptnContext = keptn.sendFinishedEvent eventType: "test", keptnContext: "${params.shkeptncontext}", triggeredId: "${params.triggeredid}", result:"pass", status:"succeeded", eventTypePayload: eventTypePayload, lables: lables



// Example #2: Send a finished Event for a deployment task
// keptn.sendFinishedEvent functions have optional event type payload - example payload for keptn.sendFinishedEvent eventType: "deployment"
def eventTypePayload=[:]
eventTypePayload.put('deploymentstrategy', 'direct')
eventTypePayload.put('deploymentURIsLocal', ['carts.sockshop-staging.svc.cluster.local','another.cartsUri.local'])
def keptnContext = keptn.sendFinishedEvent eventType: "deployment", keptnContext: "${params.shkeptncontext}", triggeredId: "${params.triggeredid}", result:"pass", status:"succeeded", eventTypePayload: eventTypePayload, lables: lables



// Quality Gate Evaluation Use Case
// ------------------------------------------
// Start a quality gate evaluation. There are multiple timeframe options, e.g: using timestamps or number minutes from Now()
// Example #1: Evaluate the last 10 minutes
def keptnContext = keptn.sendStartEvaluationEvent starttime:"600", endtime:"0" 

// Example #2: Evaluate the previous hour. End=Now()-3600, Start=Now()-7200
def keptnContext = keptn.sendStartEvaluationEvent starttime:"7200", endtime:"3600" 

// Example #3: Evaluate a specific timeframe
def keptnContext = keptn.sendStartEvaluationEvent starttime:"2019-06-07T07:00:00.0000Z", endtime:"2019-06-07T08:00:00.0000Z", labels: labels

// Example #4: Mark a starting timestamp before executing your tests
// Following example will fill starttime with the time when you called markEvaluationStartTime and as end is empty will default to Now()
keptn.markEvaluationStartTime()

// ... 
// ^^^ here is where you would execute any existing tests

// Send a test.finished event
def keptnContext = keptn.sendFinishedEvent eventType: "test", keptnContext: "${params.shkeptncontext}", triggeredId: "${params.triggeredid}", result:"pass", status:"succeeded"
echo "Open Keptns Bridge: ${keptn_bridge}/trace/${keptnContext}"


// Example #5: Progressive Delivery Use Case
// If you want Keptn to deploy, test and evaluate then we can simply inform Keptn that we want to 
// trigger a delivery with a container image.
// Typically you would use your Jenkins to build and push a container to your container registry. 
// After that you notify Keptn about it
def keptnContext = keptn.sendDeliveryTriggeredEvent image:"docker.io/myorg/my-image:1.2.3"
String keptn_bridge = env.KEPTN_BRIDGE
echo "Open Keptns Bridge: ${keptn_bridge}/trace/${keptnContext}"

// --------------------------------------------
// Waiting for Quality Gate Result (5 Minutes)
// --------------------------------------------
def result = keptn.waitForEvaluationDoneEvent setBuildResult:true, waitTime:5
echo "${result}"
```


## Features

A list of use-cases and features that this library supports is provided in [FEATURES.md](FEATURES.md).

## Tutorials

If you want to see more examples go here: [Keptn Jenkins Tutorials](https://github.com/keptn-sandbox/jenkins-tutorial)

## Contributors

A big thanks to all [maintainers](CODEOWNERS) and [contributors](https://github.com/keptn-sandbox/keptn-jenkins-library/graphs/contributors)!

## Contributing guidelines

Please refer to [Keptn contributing
guidelines](https://github.com/keptn/keptn/blob/master/CONTRIBUTING.md) for a
general overview of how to contribute to this repository with special attention
to our [bug first policy](https://github.com/keptn/keptn/blob/master/CONTRIBUTING.md#bug-first-policy)

## Release

Please refer to our [Release documentation](RELEASE.md)

## License

See [LICENSE](LICENSE).
