/**
 * Downloads a file from the given url and stores it in the local workspace
 */
def call(url, file) {
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