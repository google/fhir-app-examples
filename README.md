# FHIR App Examples

A repository containing example apps using [Open Health
Stack](https://developers.google.com/open-health-stack) components.

Follow the guide below to see a demo of several components working together.

## Before you begin

### What you'll see

The demo Android app uses the [Android FHIR
SDK](https://github.com/google/android-fhir) to show a patient registration
questionnaire and save them locally. It also syncs data with a FHIR server through
the [FHIR Info Gateway](https://github.com/google/fhir-gateway), which provides
access control to FHIR resources based on the authenticated user. Finally,
[FHIR Data Pipes](https://github.com/google/fhir-data-pipes) periodically
transforms the data to Parquet files that you can query to perform analytics.


### What you'll learn

*   How to integrate the Android FHIR SDK with the FHIR Info Gateway
*   How the FHIR Info Gateway works
*   How to implement interfaces for syncing FHIR resources
*   How to transform and export FHIR data to query with Spark SQL

The diagram below shows the different components that are involved:

[![Setup Overview](https://github.com/google/fhir-gateway/blob/main/doc/summary.png?raw=true "Setup Overview")](https://github.com/google/fhir-gateway/blob/main/doc/summary.png?raw=true)

### What you'll need

*   Java 8 or higher
*   [Android Studio](https://developer.android.com/studio) set up with an
    [Android emulator](https://developer.android.com/studio/run/managing-avds)
*   [Docker](https://docs.docker.com/get-docker/)
*   [Docker-Compose](https://docs.docker.com/compose/install/) v2 or later

## Demo app setup

1.  Clone the FHIR App Examples repo:

    ```shell
    git clone https://github.com/google/fhir-app-examples.git
    ```

2.  Open Android Studio, select **Import Project (Gradle, Eclipse ADT, etc.)**
    and choose the `fhir-app-examples` folder downloaded in the previous step.
    If this is your first time opening the project, the Gradle Build process
    should start (and take some time).

## FHIR server and Data Pipes setup

1.  Clone the [FHIR Data Pipes repo](https://github.com/google/fhir-data-pipes):

    ```shell
    git clone https://github.com/google/fhir-data-pipes.git
    ```

2.  Follow the instructions to [set up a local test
    server](https://github.com/google/fhir-data-pipes/wiki/Try-the-pipelines-using-local-test-servers).
    At step 2, follow the instructions for a "HAPI source server with Postgres".
    You can omit the optional step 4.

    You now have a HAPI FHIR server that you loaded with synthetic patient
    data; exactly 79 patients, if it only has the test data.

3.  From a terminal, run:

    ```shell
    PATIENT_ID1=4765
    PATIENT_ID2=4767

    curl -X PUT -H "Content-Type: application/json" \
      "http://localhost:8091/fhir/List/patient-list-example" \
      -d '{
          "resourceType": "List",
          "id": "patient-list-example",
          "status": "current",
          "mode": "working",
          "entry": [
             {
                "item": {
                "reference": "Patient/'"${PATIENT_ID1}"'"
                }
             },
             {
                "item": {
                "reference": "Patient/'"${PATIENT_ID2}"'"
                }
             }
          ]
       }'
    ```
    Note: If you are copy and pasting from OSX, you may need to first paste
    into a text editor to put all of that in a single line (and removing
    trailing "\\"), before pasting that into shell to run.

    This creates a [FHIR List](https://www.hl7.org/fhir/list.html) on the
    server with the id `patient-list-example`, which we will use as an
    access control list. It contains references to `Patient/4765` and
    `Patient/4767` which the user is allowed to access.

4.  Follow the instructions to set up a [single-machine analytics
    pipeline](https://github.com/google/fhir-data-pipes/wiki/Analytics-on-a-single-machine-using-Docker).
    This Docker image includes the [FHIR Pipelines
    Controller](https://github.com/google/fhir-data-pipes/wiki/Try-out-the-FHIR-Pipelines-Controller)
    plus a Spark Thrift server where data is ultimately loaded for querying.

5.  In a web browser, visit [http://localhost:8090](http://localhost:8090)
    to see the FHIR Pipelines Controller UI.

## IDP and Info Gateway setup

1.  Clone the FHIR Info Gateway repo:

    ```shell
    git clone https://github.com/google/fhir-gateway.git
    ```

2.  Start the Keycloak Identity Provider Server. From the fhir-gateway
    directory, run:

    ```shell
    docker-compose -f docker/keycloak/config-compose.yaml \
      up --force-recreate --remove-orphans -d --quiet-pull
    ```

    The
    [config-compose.yaml](https://github.com/google/fhir-gateway/blob/main/docker/keycloak/config-compose.yaml)
    sets up a Keycloak instance that can support both a list-based access
    control and a single-patient based SMART-on-FHIR app (in two separate
    realms).

    The `keycloak-config` image is built using the Dockerfile
    [here](https://github.com/google/fhir-gateway/blob/main/docker/keycloak/Dockerfile).
    A key component of the Dockerfile is the
    [keycloak_setup.sh](https://github.com/google/fhir-gateway/blob/main/docker/keycloak/keycloak_setup.sh).
    There are two points of interest in this script: the first is
    [this](https://github.com/google/fhir-gateway/blob/main/docker/keycloak/keycloak_setup.sh#L78-L83),
    which creates a client that authenticated users can act as, and
    [here](https://github.com/google/fhir-gateway/blob/main/docker/keycloak/keycloak_setup.sh#L101-L105)
    where we create a user that binds the `patient-list-example` value to the
    `patient_list` claim field that is part of the JWT access token. The default
    username and password used for the user are from the 
    [env file](https://github.com/google/fhir-gateway/blob/main/docker/keycloak/.env).

3.  Start the FHIR Info Gateway. From the fhir-gateway directory, run:

    ```shell
    docker run --rm --network host \
      -e TOKEN_ISSUER="http://localhost:9080/auth/realms/test" \
      -e PROXY_TO="http://localhost:8091/fhir" \
      -e BACKEND_TYPE="HAPI" \
      -e RUN_MODE="DEV" \
      -e ACCESS_CHECKER=list \
      -e ALLOWED_QUERIES_FILE="resources/hapi_page_url_allowed_queries.json" \
      us-docker.pkg.dev/fhir-proxy-build/stable/fhir-access-proxy:latest
    ```

    This brings up a FHIR Info Gateway, connected to the HAPI FHIR server.
    The `TOKEN_ISSUER` variable is the IP of the Keycloak IDP from the
    previous step, and the `PROXY_TO` variable is the IP of the FHIR server. As
    we are running the `TOKEN_ISSUER` and FHIR Info Gateway on the same machine
    (but on different ports), we need to bypass the Proxy's token issuer check
    by setting the environment variable `RUN_MODE` to `DEV`.

    **WARNING**: Never use `RUN_MODE=DEV` in a production environment.

    Part of setting up the FHIR Info Gateway is choosing the type of
    [Access Checker](https://github.com/google/fhir-gateway/wiki/Understanding-access-checker-plugins)
    to use. This is set using the `ACCESS_CHECKER` environment variable (See
    [here](https://github.com/google/fhir-gateway#proxy-setup) for more
    detail). In this demo, we will use the default value of `list`, which will
    use the
    [`ListAccessChecker`](https://github.com/google/fhir-gateway/wiki/Understanding-access-checker-plugins#explore-the-list-access-checker-plugin)
    to manage incoming requests. This access-checker uses the `patient_list` ID
    in the JWT access token to fetch the "List" of patient IDs that the given
    user has access to. There are some URL requests that we want to bypass the
    access checker (e.g. URLs with `_getpages` in them) and we declare these
    rules in
    [`hapi_page_url_allowed_queries.json`](https://github.com/google/fhir-gateway/blob/main/resources/hapi_page_url_allowed_queries.json).
    To make the server use this file, we set the environment variable
    [`ALLOWED_QUERIES_FILE`](https://github.com/google/fhir-gateway#proxy-setup).

## See the components in action

This example demonstrates several components of Open Health Stack.

### Android FHIR SDK in the demo app

The Demo app uses the [Structured Data Capture
library](https://developers.google.com/open-health-stack/android-fhir/data-capture) 
to render the patient registration and survey forms, and to extract FHIR
resources based on the responses. You can see a form by clicking the **Add
Patient** button in the bottom-right of the main screen.

The Demo app also uses the [FHIR Engine library](https://developers.google.com/open-health-stack/android-fhir/fhir-engine)
to save FHIR resources in the app and sync them with a FHIR server. You can
see this when resources sync from the server the first time, or when you register
new patients.

1.  In Android Studio, with an Android Emulator installed, run the `demo` app by
    pressing on the **Play** button on the top bar. This will build the app and
    open the emulator.

1.  Once the app finishes building it will launch in the emulator and its logs
    will be available in the bottom **Run** tab of Android Studio.

1.  In the Emulator, press the **Log In** button, which will take you to the IDP
    login screen. Type **testuser** as the username and **testpass** as the
    password.

1.  The app will then start the syncing process. You can see this in the logs
    displayed in the Run tab.

### Info Gateway

When the Demo app syncs resources with the FHIR server, it is actually communicating
with a [FHIR Info Gateway](https://developers.google.com/open-health-stack/fhir-info-gateway).
It uses the [List Access Checker](https://github.com/google/fhir-gateway/wiki/Understanding-access-checker-plugins#explore-the-list-access-checker-plugin)
to determine which Patient resources `testuser` has access to, and then fetches the
resources from the actual FHIR server when allowed. The demo app is designed to only
send requests that are expected to succeed, but you can follow the guide to [try out the Info
Gateway](https://github.com/google/fhir-gateway/wiki/Try-out-FHIR-Information-Gateway)
for more information.

### FHIR Data Pipes

The FHIR Data Pipes Pipelines Controller facilitates the transformation of data from
a FHIR server to Parquet files. In this guide, you use the [single machine
configuration](https://github.com/google/fhir-data-pipes/wiki/Analytics-on-a-single-machine-using-Docker)
which also loads the Parquet files into a Spark Thrift server for you.

1.  Visit the Pipeline Controller UI at [http://localhost:8090](http://localhost:8090).

1.  Click on **Run Full** to generate the Parquet files.

1.  Connect to jdbc:hive2://localhost:10001 using a Hive/Spark client.

1.  Count the number of patients:
    ```sql
    SELECT COUNT(0) FROM default.patient;
    ```

1.  From the demo app running in the Android Emulator, register a new patient by selecting
    the **New Patient** (+) button and complete the registration form.

1.  Force the app to sync with the server by tapping the menu button and selecting **Sync**.

1.  Update the Parquet files by visiting the Pipeline Controller UI and clicking **Run Incremental**.

1.  Query the number of patients again:
    ```sql
    SELECT COUNT(0) FROM default.patient;
    ```

If you have any errors when running the incremental pipeline or it fails to work, try using
`sudo chmod -R 755` on the Parquet file directory, default located at
`fhir-data-pipes/tree/master/docker/dwh`. 

## Implementation details

### Initial Launch

When the app is launched, the first class launched is
[`FhirApplication`](demo/src/main/java/com/google/fhir/examples/demo/FhirApplication.kt),
as it is a subclass of
[`Application`](https://developer.android.com/reference/android/app/Application)
and specified in the `"android:name"` field in
[`AndroidManifest.xml`](demo/src/main/AndroidManifest.xml). Part of the
`FhirApplication` class instantiates a
[`ServerConfiguration`](https://github.com/google/android-fhir/blob/master/engine/src/main/java/com/google/fhir/examples/FhirEngineProvider.kt#L129).
We pass into the `ServerConfiguration` the URL of the FHIR Access Proxy. As we
are running the Proxy and the App from the same machine, we use
[`10.0.2.2`](https://developer.android.com/studio/run/emulator-networking) as a
special alias to the host loopback interface (i.e., 127.0.0.1 on the same
machine). We also pass into the `ServerConfiguration` an instance of
[`Authenticator`](https://github.com/google/android-fhir/blob/master/engine/src/main/java/com/google/fhir/examples/sync/Authenticator.kt)
for supplying the Proxy the JWT access token;
[`LoginRepository`](demo/src/main/java/com/google/fhir/examples/demo/LoginRepository.kt)
is the implementation of `Authenticator` we wrote.

### Fetching Access Token

Our end-to-end setup uses
[OAuth 2.0 authorization code flow](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow)
to retrieve an access token.

<!-- TODO(omarismail): Add diagram showing OAuth Flow -->

After initializing the `FhirApplication` class, the next class launched is the
[`LoginActivity`](demo/src/main/java/com/google/fhir/examples/demo/LoginActivity.kt)
class, as specified by the intent filters in the `AndroidManifest.xml` file. The
`LoginActivity` class initializes the
[`LoginActivityViewModel`](demo/src/main/java/com/google/fhir/examples/demo/LoginActivityViewModel.kt)
class; the `LoginActivityViewModel` contains two methods that are called by
`LoginActivity`: `createIntent` and `handleLoginResponse`. The first method
returns an
[`Intent`](https://developer.android.com/reference/android/content/Intent) that
is bound to the Log In button. The intent is built by first fetching the
[Discovery Document](https://openid.net/specs/openid-connect-discovery-1_0.html)
from the Proxy. The URL to the Proxy discovery endpoint is loaded from the
[`auth_config.json`](demo/src/main/res/raw/auth_config.json). When a request to the
Proxy is made to this endpoint, it returns a response that includes the value of
`TOKEN_ISSUER`, which is needed to create the login Intent.

When the Log In button is pressed, the Intent opens a webpage to the login
screen with the value of `TOKEN_ISSUER` as the base URL, where the user is
prompted to type in their credentials. Once the user logs in, the callback
defined in the `getContent` variable in `LoginActivity` runs, which takes the
response from the IDP containing an
[authorization code](https://www.oauth.com/oauth2-servers/server-side-apps/authorization-code/),
and passes it to the `handleLoginResponse` method. This method abstracts the
exchange of the authorization code for an access token, which is stored in the
App. Any call in the app now made to `LoginRepository.getAccessToken` fetches
the stored JWT, and if expired, refreshes the token.

### Download Resources

Once the user logs in, the
[`MainActivity`](demo/src/main/java/com/google/fhir/examples/demo/MainActivity.kt)
class is launched, which instantiates the
[`MainActivityViewModel`](demo/src/main/java/com/google/fhir/examples/demo/MainActivityViewModel.kt)
class. When `MainActivityViewModel` is initialized, it launches an instance of
[`SyncJob`](https://github.com/google/android-fhir/blob/master/engine/src/main/java/com/google/fhir/examples/sync/SyncJob.kt).
One of the parameters we need to pass in to the `SyncJob.poll` method is an
implementation of the
[`FhirSyncWorker`](https://github.com/google/android-fhir/blob/master/engine/src/main/java/com/google/fhir/examples/sync/FhirSyncWorker.kt)
abstract class, which we provide via the
[`FhirPeriodicSyncWorker`](demo/src/main/java/com/google/fhir/examples/demo/data/FhirPeriodicSyncWorker.kt)
class.

`FhirPeriodicSyncWorker` implements two methods, one of which is
`getDownloadWorkManager`. The implementation of that method requires a
[`DownloadWorkManager`](https://github.com/google/android-fhir/blob/master/engine/src/main/java/com/google/fhir/examples/sync/DownloadWorkManager.kt)
returned, a class that we also have to implement. We have to provide a way for
the SDK to generate the FHIR download requests and handle the FHIR responses
returned, and we do that via the
[`DownloadWorkManagerImpl`](demo/src/main/java/com/google/fhir/examples/demo/data/DownloadWorkManagerImpl.kt)
class. This class takes in an initial resource ID to seed the first download
request; this resource ID comes from the value of the `patient_list` claim that
is part of the JWT access token now stored on the App.

As we logged in as `testuser`, the value of `patient_list` will be
`patient-list-example`, which we defined in Keycloak. `patient-list-example` is
the ID of a List resource on the FHIR server that we first want to fetch. With
`FhirPeriodicSyncWorker` and `DownloadWorkManagerImpl` instantiated, the
`SyncJob.poll` method runs and downloads all resources as specified in the
classes we created.
