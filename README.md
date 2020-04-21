# Keptn Jenkins Shared Library
Jenkins shared library for integrating Keptn Use Cases with your Jenkins Pipelines

| Author | Library Version |
| ------ | ------------- |
| grabnerandi | master |

## Usage
In order to use this Jenkins Shared Library simply configure it in your Global Jenkins Configuration. Here is one way of doing this by pulling it from this GitHub repo:
![](./images/jenkinsglobalconfig.png)

Once you have this global library configured you can use it in your Jenkins Pipeline like this

```
@library('keptn-library')
import sh.keptn.Keptn
def keptn = new sh.keptn.Keptn()

// initialize keptn
keptn.keptnInit project:"yourkeptnproject", service:"yourkeptnservice", stage:"yourkeptnstage", shipyard:'shipyard.yaml'

// upload a file to your keptn initialized project
keptn.keptnAddResources('keptn/sli.yaml','dynatrace/sli.yaml')

// start a quality gate evaluation for the last 10 minutes shifted by 1 minute
def keptnContext = keptn.sendStartEvaluationEvent starttime:"660", endtime:"60" 
echo "Open Keptns Bridge: ${keptn_bridge}/trace/${keptnContext}"

// inform keptn about a new deploymed version to let Keptn execute tests and enforce quality gates
def keptnContext = keptn.sendDeploymentFinishedEvent testStrategy:"performance", deploymentURI:"http://yourapp.yourdomain.local"
echo "Open Keptns Bridge: ${keptn_bridge}/trace/${keptnContext}"

// wait for quality gave evaluation to be done
def result = keptn.waitForEvaluationDoneEvent setBuildResult:true, waitTime:waitTime
echo "${result}"
```