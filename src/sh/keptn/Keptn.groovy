package sh.keptn

import org.jenkinsci.plugins.plaincredentials.StringCredentials
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.domains.DomainRequirement

/**
 * Downloads a file from the given url and stores it in the local workspace
 */
def downloadFile(url, file) {
    def downloadFileResponse = httpRequest httpMode: 'GET', 
            responseHandle: 'STRING', 
            url: url, 
            validResponseCodes: "100:404",
            ignoreSslErrors: true

    if (downloadFileResponse.status == 200) {
        echo "Successfully requested file from url: ${url}"
        writeFile file:file, text:downloadFileResponse.content
    } else {
        echo "Couldn't download file from url: ${url}. Response Code: " + createProjectResponse.status
    }
}

def getKeptnContextJsonFilename() {return "keptn.context.${BUILD_NUMBER}.json"}
def getKeptnInitJsonFilename() {return "keptn.init.${BUILD_NUMBER}.json"}

// added getNow() to easily switch between java.time.LocalDateTime.now() to Instant.now(). INstant.now() returns time in UTC where LocalDataTime returns local time without timezone. this leads to problems in case Jenkins Server and Keptn are in differnet timezones
def getNow() {
    // return java.time.LocalDateTime.now()
    return java.time.Instant.now()
}

String getKeptnApiToken() {
    StringCredentials keptn_api_token_credential = CredentialsProvider.findCredentialById(
        'KEPTN_API_TOKEN',
        StringCredentials,
        currentBuild.rawBuild,
        Collections.<DomainRequirement>emptyList()
    )
    String keptn_api_token = keptn_api_token_credential != null ? keptn_api_token_credential.secret : env.KEPTN_API_TOKEN
    return keptn_api_token
}

/** 
 * Loads the JSON from keptn.init and returns a json map - also merges the incoming parameters and fills in the keptn_api_token
 * Usage:
 * def keptnInit = keptnLoadFromInit(args)
 * echo keptnInit['project']
 */
def keptnLoadFromInit(Map args) {
    def keptnInitFileJson
    if (fileExists(file: getKeptnInitJsonFilename())) {
        def keptInitFileJsonContent = readFile getKeptnInitJsonFilename()
        echo keptInitFileJsonContent
        keptnInitFileJson = readJSON text: keptInitFileJsonContent
        /* for (keptnEntries in keptnInitFileJson) {
            println(keptnEntries.key + " = " +  keptnEntries.value)
        }*/
    } else {
        keptnInitFileJson = [:]
    }

    // now adding additional values from args to the map
    for ( argEntry in args ) {
        if (!keptnInitFileJson.containsKey(argEntry.key)) {
            keptnInitFileJson[argEntry.key] = argEntry.value
            // println("putting " + argEntry.key + "," + "${argEntry.value}")
        }
    }

    keptnInitFileJson['keptn_api_token'] = keptnApiToken

    // iterate over all arguments and print them
    /*println("final map entries")
    for (keptnEntries in keptnInitFileJson) {
        println(keptnEntries.key + " = " +  keptnEntries.value)
    }*/

    return keptnInitFileJson
}

/** 
 * keptnInit(project, stage, service, [shipyard], [monitoring])
 * Stores these values in keptn.init.json and makes sure that the Keptn project, stage and service exists
 * If shipyard is specified will create the project with the passed shipyard. If the project already exists it will just return the indicator that the project exists
 * If monitoring is specified, e.g: dynatrace, prometheus a configure-monitoring event is sent for this project!
 */
def keptnInit(Map args) {
    String keptn_endpoint = args.containsKey("keptn_endpoint") ? args.keptn_endpoint : env.KEPTN_ENDPOINT
    String keptn_bridge = args.containsKey("keptn_bridge") ? args.keptn_bridge : env.KEPTN_BRIDGE
    String keptn_api_token = keptnApiToken

    String project = args.containsKey("project") ? args.project : ""
    String stage = args.containsKey("stage") ? args.stage : ""
    String service = args.containsKey("service") ? args.service : ""
    String monitoring = args.containsKey("monitoring") ? args.monitoring : ""

    if ((project == "") || (stage == "") || (service == "") ||
        (keptn_endpoint == null) || (keptn_bridge == null) || (keptn_api_token == null)) {
        error("keptnInit requires project, stage, service, keptn_endpoint, keptn_bridge and keptn_api_token to be set. These values cant be empty!")
    }

    // write our key keptn params to keptn.init.json
    def initJson = [
        project: "${project}", service: "${service}", stage: "${stage}",
        keptn_endpoint: "${keptn_endpoint}", keptn_bridge: "${keptn_bridge}"
    ]
    writeJSON file: getKeptnInitJsonFilename(), json: initJson
    def keptnInit = keptnLoadFromInit(args)

    project = keptnInit['project']
    echo "Project: ${project}"

    // Step #0: Generate or use the shipyard file passed
    def shipyardFileContent = """apiVersion: "spec.keptn.sh/0.2.0"
    |kind: "Shipyard"
    |metadata:
    |  name: "shipyard-${project}"
    |spec:
    |  stages:
    |    - name: "${stage}"
    """.stripMargin()
    if (args.containsKey("shipyard")) {
        // lets see if a shipyard was passed - if so - we use that shipyard.yaml
        shipyardFileContent = readFile(args.shipyard)
    }
    echo "Shipyard: ${shipyardFileContent}"
    //TODO: add the shipyard.yaml to keptn project
    writeFile file:"keptn/shipyard.yaml", text:shipyardFileContent
    archiveArtifacts artifacts: "keptn/shipyard.yaml"
    keptnAddProjectResources("keptn/shipyard.yaml","shipyard.yaml")
    // Step #1: Create Project
    // TODO: will change this once we have a GET /project/{project} endpoint to query whether Project alread exists
    if (keptnProjectExists(args)) {
        if (keptnProjectStageExists(args)) {
            echo "Project ${project} with Stage ${stage} already exists on Keptn. Nothing to create!"
        } else {
            echo "Project ${project} exists on your Keptn server but doesnt contain Stage ${stage}! CAN'T add a new stage so stopping process"
            return false
        }
    } else {
        //perform base64 encoding on shipyard file
        echo "Project: ${project}"
        String shipyardBase64Encoded = shipyardFileContent.bytes.encodeBase64().toString()
        def createProjectBody = """{
            "name" : "${project}", 
            "shipyard" : "${shipyardBase64Encoded}"
        }"""
        def createProjectResponse = httpRequest contentType: 'APPLICATION_JSON', 
            customHeaders: [[maskValue: true, name: 'x-token', value: "${keptn_api_token}"]], 
            httpMode: 'POST', 
            requestBody: createProjectBody, 
            responseHandle: 'STRING', 
            url: "${keptn_endpoint}/controlPlane/v1/project", 
            validResponseCodes: "100:404",
            ignoreSslErrors: true

        if (createProjectResponse.status == 200) {
            echo "Created new Keptn Project: ${project}"
        } else {
            echo "Couldnt create Keptn Project bc it probably exists ${project}: " + createProjectResponse.content
            echo "shipyard: ${shipyardFileContent}"
            echo "project-body: ${createProjectBody}"
        }
    }

    // Step #2: Create Service
    if (keptnProjectServiceExists(args)) {
        echo "Service already available in Keptn. No further action required!"
    } else {
        def createServiceBody = """{
            "serviceName" : "${service}"
        }"""
        def createServiceResponse = httpRequest contentType: 'APPLICATION_JSON', 
            customHeaders: [[maskValue: true, name: 'x-token', value: "${keptn_api_token}"]], 
            httpMode: 'POST', 
            requestBody: createServiceBody, 
            responseHandle: 'STRING', 
            url: "${keptn_endpoint}/controlPlane/v1/project/${project}/service", 
            validResponseCodes: "100:404",
            ignoreSslErrors: true

        if (createServiceResponse.status == 200) {
            echo "Created new Keptn Service: ${service}"
        } else {
            echo "Couldnt create Keptn Service ${service}: " + createServiceResponse.content          
        }

        // Step #3: Configure Monitoring
        // This will ensure that the monitoring tool of choice is configured
        if(monitoring != "") {
            def configureMonitoringBody = """{
                |  "data": {
                |    "project": "${project}",
                |    "stage": "${stage}",
                |    "service": "${service}",
                |    "configureMonitoring": {
                |      "type": "${monitoring}"
                |    }
                |  },
                |  "datacontenttype": "application/json",
                |  "source": "Jenkins",
                |  "specversion": "1.0",
                |  "type": "sh.keptn.event.configure-monitoring.triggered"
                |}
            """.stripMargin()
            def configureMonitoringResponse = httpRequest contentType: 'APPLICATION_JSON', 
                customHeaders: [[maskValue: true, name: 'x-token', value: "${keptn_api_token}"]], 
                httpMode: 'POST', 
                requestBody: configureMonitoringBody, 
                responseHandle: 'STRING', 
                url: "${keptn_endpoint}/configuration-service/v1/event", 
                validResponseCodes: "100:404",
                ignoreSslErrors: true

            if (configureMonitoringResponse.status == 200) {
                echo "Successfully configured monitoring for: ${monitoring}"
                echo "body: ${configureMonitoringBody}"
            } else {
                echo "Couldnt configure monitoring for ${monitoring}: " + configureMonitoringResponse.content
                echo "body: ${configureMonitoringBody}"
            }    
        }
    }

    return true
}

/**
 * Validates whether the project exists
 */
def keptnProjectExists(Map args) {
    def keptnInit = keptnLoadFromInit(args)

    def getProjectResponse = httpRequest customHeaders: [[maskValue: true, name: 'x-token', value: "${keptnInit['keptn_api_token']}"]], 
        httpMode: 'GET', 
        responseHandle: 'STRING', 
        url: "${keptnInit['keptn_endpoint']}/configuration-service/v1/project/${keptnInit['project']}", 
        validResponseCodes: "100:404",
        ignoreSslErrors: true

    echo "Response from get project: " + getProjectResponse.content

    return (getProjectResponse.status == 200)
}

/**
 * Validates whether the project stage exists
 */
def keptnProjectStageExists(Map args) {
    def keptnInit = keptnLoadFromInit(args)

    def getProjectStageResponse = httpRequest customHeaders: [[maskValue: true, name: 'x-token', value: "${keptnInit['keptn_api_token']}"]], 
        httpMode: 'GET', 
        responseHandle: 'STRING', 
        url: "${keptnInit['keptn_endpoint']}/configuration-service/v1/project/${keptnInit['project']}/stage/${keptnInit['stage']}", 
        validResponseCodes: "100:404",
        ignoreSslErrors: true

    echo "Response from get project stage: " + getProjectStageResponse.content

    return (getProjectStageResponse.status == 200)
}

/**
 * Validates whether the project service exists
 */
def keptnProjectServiceExists(Map args) {
    def keptnInit = keptnLoadFromInit(args)

    def getProjectServiceResponse = httpRequest customHeaders: [[maskValue: true, name: 'x-token', value: "${keptnInit['keptn_api_token']}"]], 
        httpMode: 'GET', 
        responseHandle: 'STRING', 
        url: "${keptnInit['keptn_endpoint']}/configuration-service/v1/project/${keptnInit['project']}/stage/${keptnInit['stage']}/service/${keptnInit['service']}", 
        validResponseCodes: "100:404",
        ignoreSslErrors: true

    echo "Response from get project service: " + getProjectServiceResponse.content

    return (getProjectServiceResponse.status == 200)
}

/**
 * Deletes a keptn project
 */
def keptnDeleteProject(Map args) {
    def keptnInit = keptnLoadFromInit(args)

    def deleteProjectResponse = httpRequest contentType: 'APPLICATION_JSON', 
        customHeaders: [[maskValue: true, name: 'x-token', value: "${keptnInit['keptn_api_token']}"]], 
        httpMode: 'DELETE', 
        responseHandle: 'STRING', 
        url: "${keptnInit['keptn_endpoint']}/configuration-service/v1/project/${keptnInit['project']}", 
        validResponseCodes: "100:404",
        ignoreSslErrors: true

    echo "Response from delete project: " + deleteProjectResponse.content
}


/**
 * keptnAddResources(['localfile1': remotelocation1,'localfile2': remotelocation, ...])
 * Allows you to upload a local files to the remote resource on keptn's Git repo on service level
 */
def keptnAddResources(file, remoteUri) {
    def keptnInit = keptnLoadFromInit([:])

    if (fileExists(file: file)) {
        def localFile = readFile(file)
        print "loaded file ${file}"
        //perform base64 encoding
        String localFileBase64Encoded = localFile.bytes.encodeBase64().toString()

        //Update SLO in keptn
        def requestBody = """{
            "resources" : [{"resourceURI": "${remoteUri}","resourceContent": "${localFileBase64Encoded}"}]
        }"""

        def addResourceResponse = httpRequest contentType: 'APPLICATION_JSON', 
            customHeaders: [[maskValue: true, name: 'x-token', value: "${keptnInit['keptn_api_token']}"]], 
            httpMode: 'POST', 
            requestBody: requestBody, 
            responseHandle: 'STRING', 
            url: "${keptnInit['keptn_endpoint']}/configuration-service/v1/project/${keptnInit['project']}/stage/${keptnInit['stage']}/service/${keptnInit['service']}/resource", 
            validResponseCodes: "100:404",
            ignoreSslErrors: true

        echo "Response from upload resource ${file} to ${remoteUri}: " + addResourceResponse.content

    } else {
        echo "File ${file} does not exist"
    }
}

/**
 * keptnAddProjectResources(['localfile1': remotelocation1,'localfile2': remotelocation, ...])
 * Allows you to upload one local file to the Keptn's internal repo on Project level
 */
def keptnAddProjectResources(file, remoteUri) {
    def keptnInit = keptnLoadFromInit([:])

    if (fileExists(file: file)) {
        def localFile = readFile(file)
        print "loaded file ${file}"
        //perform base64 encoding
        String localFileBase64Encoded = localFile.bytes.encodeBase64().toString()

        //Update SLO in keptn
        def requestBody = """{
            "resources" : [{"resourceURI": "${remoteUri}","resourceContent": "${localFileBase64Encoded}"}]
        }"""

        def addResourceResponse = httpRequest contentType: 'APPLICATION_JSON', 
            customHeaders: [[maskValue: true, name: 'x-token', value: "${keptnInit['keptn_api_token']}"]], 
            httpMode: 'POST', 
            requestBody: requestBody, 
            responseHandle: 'STRING', 
            url: "${keptnInit['keptn_endpoint']}/configuration-service/v1/project/${keptnInit['project']}/resource", 
            validResponseCodes: "100:404",
            ignoreSslErrors: true

        echo "Response from upload resource ${file} to ${remoteUri}: " + addResourceResponse.content

    } else {
        echo "File ${file} does not exist"
    }
}

/**
 * keptnAddStageResources(['localfile1': remotelocation1,'localfile2': remotelocation, ...])
 * Allows you to upload one local file to the Keptn's internal repo on Stage level
 */
def keptnAddStageResources(file, remoteUri) {
    def keptnInit = keptnLoadFromInit([:])

    if (fileExists(file: file)) {
        def localFile = readFile(file)
        print "loaded file ${file}"
        //perform base64 encoding
        String localFileBase64Encoded = localFile.bytes.encodeBase64().toString()

        //Update SLO in keptn
        def requestBody = """{
            "resources" : [{"resourceURI": "${remoteUri}","resourceContent": "${localFileBase64Encoded}"}]
        }"""

        def addResourceResponse = httpRequest contentType: 'APPLICATION_JSON', 
            customHeaders: [[maskValue: true, name: 'x-token', value: "${keptnInit['keptn_api_token']}"]], 
            httpMode: 'POST', 
            requestBody: requestBody, 
            responseHandle: 'STRING', 
            url: "${keptnInit['keptn_endpoint']}/configuration-service/v1/project/${keptnInit['project']}/stage/${keptnInit['stage']}/resource", 
            validResponseCodes: "100:404",
            ignoreSslErrors: true

        echo "Response from upload resource ${file} to ${remoteUri}: " + addResourceResponse.content

    } else {
        echo "File ${file} does not exist"
    }
}

/** 
 * Stores the current local time in keptn.input.json
 */
def markEvaluationStartTime() {
    def startTime = getNow().toString()

    def keptnContextFileJson
    if (fileExists(file: getKeptnInitJsonFilename())) {
        def keptnContextFileContent = readFile getKeptnInitJsonFilename()
        keptnContextFileJson = readJSON text: keptnContextFileContent
        keptnContextFileJson["starttime"] = startTime
    } else {
        keptnContextFileJson = [starttime:startTime]
    }

    writeJSON file: getKeptnInitJsonFilename(), json: keptnContextFileJson

    return startTime
}

/** 
 * reads the starttime from keptn.input.json or ""
 */
def getEvaluationStartTime() {
    if (fileExists(file: getKeptnInitJsonFilename())) {
        def keptnContextFileContent = readFile getKeptnInitJsonFilename()
        def keptnContextFileJson = readJSON text: keptnContextFileContent
        if (keptnContextFileJson.containsKey("starttime")) {
            return keptnContextFileJson["starttime"]
        }
    }
    
    return ""
}

/**
 * Writes the keptn.context.json and keptn.html file including a link to the bridge
 */
def writeKeptnContextFiles(response) {
    def keptnInit = keptnLoadFromInit([:])
    /* 
      println("Status: "+response.status)
      println("Content: "+response.content)      
    */

    def keptnResponseJson = readJSON text: response.content
    def keptnContext = keptnResponseJson['keptnContext']
    echo "Retrieved KeptnContext: ${keptnContext}"

    // First we write the actual keptn context
    writeFile file: getKeptnContextJsonFilename(), text: response.content
    archiveArtifacts artifacts: getKeptnContextJsonFilename()

    // now we generate the HTML File that contains a clickable link
    String keptn_bridge = keptnInit['keptn_bridge']
    def htmlContent = """<html>
    <head>
        <meta http-equiv="Refresh" content="0; url='${keptn_bridge}/trace/${keptnContext}'" />
    </head>
    <body>
        <p>Click <a href="${keptn_bridge}/trace/${keptnContext}">this link</a> to open the Keptn's Bridge.</p>        
    </body>
    </html>"""

    writeFile file:"keptn.html", text:htmlContent
    archiveArtifacts artifacts: "keptn.html"

    return keptnContext
}

/**
 * takes the request JSON body, adds custom labels and reports back that JSON as string
 */
def addCustomLabels(requestBody, labels) {
    def requestBodyAsJSON = readJSON text: requestBody
    if (labels != null) {
      for (label in labels) {
          requestBodyAsJSON['data']['labels'][label.key.toString()] = label.value.toString()
      }
    }

    writeJSON file: "helper.json", json: requestBodyAsJSON
    requestBody = readFile "helper.json"

    return requestBody
}

/**
 * sendStartEvaluationEvent(project, stage, service, starttime, endtime, [labels, keptn_endpoint, keptn_api_token])
 * will start an evaluation and stores the keptn context in keptn.context.json
 * if starttime == "" --> it will first look it up in keptn.context.json as it may have been set with markEvaluationStartTime()
 * if starttime == number in seconds -> will calculate the starttime based on starttime = Now()-number in seconds
 * if endtime == number in seconds -> will calculate the time based on endtime = Now()-number in seconds
 * if endtime == "" --> it will default to Now()
 * Here are a couple of usage options 
 * Last 10 minutes: starttime=600, endtime=
 * Timeframe from Now()-11minutes to Now()-1min: starttime=660, endtime=60
 * From starttime untile now: starttime="2020-04-17T11:30:00.000Z", endtime=""
 */
def sendStartEvaluationEvent(Map args) {
    def keptnInit = keptnLoadFromInit(args)
    
    /* String project, String stage, String service, String deploymentURI, String testStrategy */
    String keptn_endpoint = keptnInit['keptn_endpoint']
    String keptn_api_token = keptnInit['keptn_api_token']

    def labels = args.containsKey('labels') ? args.labels : [:]

    String project = keptnInit['project']
    String stage = keptnInit['stage']
    String service = keptnInit['service']
    
    String starttime = args.containsKey("starttime") ? args.starttime : ""
    String endtime = args.containsKey("endtime") ? args.endtime : ""

    // Allow image & tag to be passed as parameters - or default to JOB_NAME & BUILD_NUMBER
    String image = args.containsKey("image") ? args.image : "${JOB_NAME}"
    String tag = args.containsKey("tag") ? args.tag : "${BUILD_NUMBER}"

    echo "${starttime} - ${endtime}"

    // lets check on timeframe based on the usage scenarios we support
    if (starttime == "") {
        starttime = getEvaluationStartTime()
        echo "#1 - ${starttime} - ${endtime}"
    }
    if (starttime == "") {
        echo "No starttime specified. Either specify exact time or seconds counting back from Now()"
        return false;
    }
    if (starttime?.isInteger()) {
        seconds = starttime.toInteger()
        if (seconds > 0) {
            starttime = getNow().minusSeconds((int)starttime.toInteger()).toString()
            echo "Setting starttime to ${starttime}"
        } else {
            echo "No negative numbers allowed for starttime!"
            return false;
        }
    }
    if (endtime?.isInteger()) {
        seconds = endtime.toInteger()
        if (seconds > 0) {
            endtime = getNow().minusSeconds((int)endtime.toInteger()).toString()
            echo "Setting endtime to ${endtime}"
        } else {
            echo "No negative numbers allowed for endtime!"
            return false;
        }
    }
    if (endtime == "") {
        endtime = getNow().toString()
        echo "Endttime empty. Setting endtime to Now: ${endtime}"
    }

    if ((starttime == "") || (endtime == "")) {
        echo "Startime (${starttime}) and endtime (${endtime}) not correctly passed!"
        return false;
    }

    if ((project == "") || (stage == "") || (service == "")) {
        echo "Method requires project, stage and service to be set. These values cant be empty!"
        return false;
    }

    echo "Sending a Start-Evaluation event to Keptn for ${project}.${stage}.${service} for ${starttime} - ${endtime}"
    
    def requestBody = """{
        |  "data": {
        |    "teststrategy" : "manual",
        |    "project": "${project}",
        |    "stage": "${stage}",        
        |    "service": "${service}",
        |    "labels": {
        |      "buildId" : "${tag}",
        |      "jobname" : "${JOB_NAME}",
        |      "buildNumber": "${BUILD_NUMBER}",
        |      "joburl" : "${BUILD_URL}"
        |    },
        |    "status": "succeeded",
        |    "result": "pass",
        |    "test": { 
        |      "start": "${starttime}",
        |      "end" : "${endtime}"
        |    },
        |     "evaluation": {
        |       "start": "${starttime}",
        |       "end": "${endtime}"
        |    },
        |    "image" : "${image}",
        |    "tag" : "${tag}",
        |  },
        |  "datacontenttype": "application/json",        
        |  "source": "Jenkins",
        |  "specversion": "1.0",
        |  "type": "sh.keptn.event.evaluation.triggered"
        |}
    """.stripMargin()

    // lets add our custom labels
    requestBody = addCustomLabels(requestBody, labels)

    //debug feature
    //echo requestBody  
  
    def response = httpRequest contentType: 'APPLICATION_JSON', 
      customHeaders: [[maskValue: true, name: 'x-token', value: "${keptn_api_token}"]], 
      httpMode: 'POST', 
      requestBody: requestBody, 
      responseHandle: 'STRING', 
      url: "${keptn_endpoint}/v1/event", 
      validResponseCodes: "100:404", 
      ignoreSslErrors: true

    // write response to keptn.context.json & add to artifacts
    def keptnContext = writeKeptnContextFiles(response)
    
    return keptnContext
}

/**
 * waitForEvaluationDoneEvent(setBuildResult, [keptn_context, keptn_endpoint, keptn_api_token])
 */
def waitForEvaluationDoneEvent(Map args) {
    def keptnInit = keptnLoadFromInit(args)
    
    Boolean setBuildResult = args.containsKey("setBuildResult") ? args.setBuildResult : false 
    int waitTime = args.containsKey("waitTime") ? args.waitTime : 3 // default is 3 minute wait 
    String keptn_endpoint = keptnInit['keptn_endpoint']
    String keptn_api_token = keptnInit['keptn_api_token']
    String keptn_context = args.containsKey("keptnContext") ? args.keptnContext : ""

    if ((keptn_context == "" || keptn_context == null) && fileExists(file: getKeptnContextJsonFilename())) {
        def keptnContextFileContent = readFile getKeptnContextJsonFilename()
        def keptnContextFileJson = readJSON text: keptnContextFileContent
        keptn_context = keptnContextFileJson['keptnContext']
    }

    if (keptn_context == "" || keptn_context == null) {
        echo "Couldnt find a current keptnContext. Not getting evaluation results"
        if (setBuildResult) {

            // currentBuild.result = 'FAILURE' 
            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                error('No keptn context')
                // sh "exit 1"
            }
        }
        return false;
    }

    echo "Wait for Evaluation Done for keptnContext: ${keptn_context}"
    sleep 10 //added delay for keptn that is too fast
    
    def evalResponse = ""
    timeout(time: waitTime, unit: 'MINUTES') {
        script {
            waitUntil {
                // Post the Keptn Context to the Keptn api to get the Evaluation-done event
                def response = httpRequest contentType: 'APPLICATION_JSON', 
                    customHeaders: [[maskValue: true, name: 'x-token', value: "${keptn_api_token}"]], 
                    httpMode: 'GET', 
                    responseHandle: 'STRING', 
                    url: "${keptn_endpoint}/mongodb-datastore/event?keptnContext=${keptn_context}&type=sh.keptn.event.evaluation.finished", 
                    validResponseCodes: "100:404", 
                    ignoreSslErrors: true

                //The API returns a response code 404 error if the evalution done event does not exist
                if (response.status == 404 || response.content.contains("No Keptn sh.keptn.event.evaluation.finished event found for context") || response.content.contains("[]")  ) {
                    sleep 10
                    return false
                } else {
                    evalResponse = response.content
                    return true
                } 
            }
        }
    }
    
    if (evalResponse == "") {
        echo "Didnt receive any successful keptn evaluation results"
        if (setBuildResult) {
            // currentBuild.result = 'FAILURE'
            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                error("Didnt receive any successful keptn evaluation results")
                // sh "exit 1"
            }
        }
        return false;
    }

    // write result to file and archive it
    writeFile file: "keptn.evaluationresult.${keptn_context}.json", text: evalResponse
    archiveArtifacts artifacts: "keptn.evaluationresult.${keptn_context}.json"
    println("Archived Keptn Evaluation Done Result details in keptn.evaluationresult.${keptn_context}.json")

    def keptnResponseJson = readJSON text: evalResponse
    // check whether we really retrieve a valid response - otherwise this woudl lead to an NPE
    def score = 0
    def result = ""
    if ((keptnResponseJson != null) && (keptnResponseJson['events'] != null) && (keptnResponseJson['events']['data']['evaluation'] != null)) {
      score = keptnResponseJson['events']['data']['evaluation']['score']
      result = keptnResponseJson['events']['data']['evaluation']['result']
    }
    
    echo "Retrieved Score: ${score}, Result: ${result}"

    // set build result depending on score
    if (setBuildResult) {
        switch(result) {
            case "[pass]":
                // currentBuild.result = 'SUCCESS' 
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    error("Keptn Score: ${score}, Result: ${result}")
                    echo "SUCCESS"
                    // sh "exit 1"
                }
                break;
            case "[warning]":
                // currentBuild.result = 'UNSTABLE' 
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    error("Keptn Score: ${score}, Result: ${result}")
                    echo "UNSTABLE"
                    // sh "exit 1"
                }
                break;
            default:
                // currentBuild.result = 'FAILURE' 
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    // sh "exit 1"
                    error("Keptn Score: ${score}, Result: ${result}")
                    echo "FAILURE"
                }
                break;
        }
    }

    return score
}

/**
 * sendDeploymentFinishedEvent(project, stage, service, deploymentURI, testStrategy [labels, keptn_endpoint, keptn_api_token])
 * Example: sendDeploymentFinishedEvent deploymentURI:"http://mysampleapp.mydomain.local" testStrategy:"performance"
 * Will trigger a Continuous Performance Evaluation workflow in Keptn where Keptn will 
    -> first: trigger a test execution against that URI with the specified testStrategy
    -> second: trigger an SLI/SLO evaluation!
 */
def sendDeploymentFinishedEvent(Map args) {
    def keptnInit = keptnLoadFromInit(args)

    /* String project, String stage, String service, String deploymentURI, String testStrategy */
    String keptn_endpoint = keptnInit['keptn_endpoint']
    String keptn_api_token = keptnInit['keptn_api_token']

    def labels = args.containsKey('labels') ? args.labels : [:]

    String project = keptnInit['project']
    String stage = keptnInit['stage']
    String service = keptnInit['service']
    String deploymentURI = args.containsKey("deploymentURI") ? args.deploymentURI : ""
    String testStrategy = args.containsKey("testStrategy") ? args.testStrategy : ""

    // Allow image & tag to be passed as parameters - or default to JOB_NAME & BUILD_NUMBER
    String image = args.containsKey("image") ? args.image : "${JOB_NAME}"
    String tag = args.containsKey("tag") ? args.tag : "${BUILD_NUMBER}"

    echo "Sending a Deployment Finished event to Keptn for ${project}.${stage}.${service} on ${deploymentURI} with testStrategy ${testStrategy}"
    
    def requestBody = """{
        |  "datacontenttype": "application/json",
        |  "data": {
        |    "deploymentURIPublic": "${deploymentURI}",
        |    "teststrategy" : "${testStrategy}",
        |    "project": "${project}",
        |    "service": "${service}",
        |    "stage": "${stage}",
        |    "image" : "${image}",
        |    "tag" : "${tag}",
        |    "labels": {
        |      "buildId" : "${tag}",
        |      "jobname" : "${JOB_NAME}",
        |      "buildNumber": "${BUILD_NUMBER}",
        |      "joburl" : "${BUILD_URL}"
        |    }
        |  },
        |  "source": "jenkins-library",
        |  "specversion": "1.0",
        |  "type": "sh.keptn.event.deployment.finished"
        |}
    """.stripMargin()

    // lets add our custom labels
    requestBody = addCustomLabels(requestBody, labels)
    
    //debug feature
    //echo requestBody  
  
    def response = httpRequest contentType: 'APPLICATION_JSON', 
      customHeaders: [[maskValue: true, name: 'x-token', value: "${keptn_api_token}"]], 
      httpMode: 'POST', 
      requestBody: requestBody, 
      responseHandle: 'STRING', 
      url: "${keptn_endpoint}/v1/event", 
      validResponseCodes: "100:404", 
      ignoreSslErrors: true


    // write response to keptn.context.json & add to artifacts
    def keptnContext = writeKeptnContextFiles(response)

    return keptnContext
}

/**
 * sendConfigurationChangedEvent(project, stage, service, image, [labels, keptn_endpoint, keptn_api_token])
 * Example: sendConfigurationChangedEvent image:"docker.io/grabnerandi/simplenodeservice:3.0.0"
 * Will trigger a full delivery workflow in keptn!
 */
def sendConfigurationChangedEvent(Map args) {
    def keptnInit = keptnLoadFromInit(args)

    /* String project, String stage, String service, String image, String tag */
    String keptn_endpoint = keptnInit['keptn_endpoint']
    String keptn_api_token = keptnInit['keptn_api_token']

    def labels = args.containsKey('labels') ? args.labels : [:]

    String project = keptnInit['project']
    String stage = keptnInit['stage']
    String service = keptnInit['service']
    String image = args.containsKey("image") ? args.image : ""
    String tag = args.containsKey("tag") ? args.tag : "${BUILD_NUMBER}"

    echo "Sending a Configuration Change event to Keptn for ${project}.${stage}.${service} for image ${image}"
    
    def requestBody = """{
        |  "data": {
        |    "project": "${project}",
        |    "service": "${service}",
        |    "stage": "${stage}",
        |    "configurationChange": {
        |      "values": {
        |      "image": "${image}"
        |      }
        |    },
        |    "deployment": {
        |      "deploymentstrategy": "direct"
        |    },
        |    "labels": {
        |      "buildId" : "${tag}",
        |      "jobname" : "${JOB_NAME}",
        |      "buildNumber": "${BUILD_NUMBER}",
        |      "joburl" : "${BUILD_URL}"
        |    }
        |  },
        |  "datacontenttype": "application/json",
        |  "source": "jenkins-library",
        |  "specversion": "1.0",
        |  "type": "sh.keptn.event.${stage}.delivery.triggered"
        |}
    """.stripMargin()

    // lets add our custom labels
    requestBody = addCustomLabels(requestBody, labels)
    
    //Debug feature
    //echo requestBody   
  
    def response = httpRequest contentType: 'APPLICATION_JSON', 
      customHeaders: [[maskValue: true, name: 'x-token', value: "${keptn_api_token}"]], 
      httpMode: 'POST', 
      requestBody: requestBody, 
      responseHandle: 'STRING', 
      url: "${keptn_endpoint}/v1/event", 
      validResponseCodes: "100:404", 
      ignoreSslErrors: true


    // write response to keptn.context.json & add to artifacts
    def keptnContext = writeKeptnContextFiles(response)
    
    return keptnContext
}

return this
