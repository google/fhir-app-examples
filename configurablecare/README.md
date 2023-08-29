# Configurable Care

As an implementer it is easier to build and deploy configurable care delivery applications for different health programs that can leverage evidence based guidelines (e.g. SMART Guidelines). The Configurable Care application is a reference application that enables dynamic deployment of FHIR content to drive care delivery i.e. workflows. It uses the [Android FHIR SDK](https://github.com/google/android-fhir) ([Engine](https://github.com/google/android-fhir/wiki/FHIR-Engine-Library), [DataCapture](https://github.com/google/android-fhir/wiki/Structured-Data-Capture-Library) and [Workflow](https://github.com/google/android-fhir/wiki/Workflow-Library) libraries) to execute workflow logic and provide basic support for managing [CarePlan](https://www.hl7.org/fhir/careplan.html) and [Task](https://www.hl7.org/fhir/task.html) resources.


## Running the Application
### 1. Download the code
To download the code, clone the [FHIR App Examples](https://github.com/google/fhir-app-examples) repo: \
`git clone https://github.com/google/fhir-app-examples.git`

### 2. Import the `configurablecare` app into Android Studio

Open Android Studio, select `Open...` and choose the `configurablecare` folder from the source code that you downloaded earlier.

### 3. Set up a local HAPI FHIR server with test data
Run the following command in a terminal to get the latest image of HAPI FHIR: \
`docker pull hapiproject/hapi:latest`

Create a HAPI FHIR container by either using Docker Desktop to run the previously downloaded image `hapiproject/hapi`, or by running the following command: \
`docker run -p 8080:8080 hapiproject/hapi:latest` \
*Learn [more](https://github.com/hapifhir/hapi-fhir-jpaserver-starter#running-via-docker-hub).*

You should see the HAPI FHIR web interface when you navigate to http://localhost:8080/ in a browser.

#### Test data
*You may skip this step and use your own data instead of the sample FHIR resources provided in this application.*

The `configurablecare/app/src/main/assets` directory contains a FHIR Bundle `fhir-resources-example.json` with some example resources for running the Configurable Care application:
- Knowledge resources for conducting screenings ([PlanDefinitions](https://www.hl7.org/fhir/plandefinition.html), [ActivityDefinitions](https://www.hl7.org/fhir/activitydefinition.html), [Questionnaires](https://www.hl7.org/fhir/questionnaire.html))
- Practitioner related resources ([Practitioners](https://www.hl7.org/fhir/practitioner.html) [PractitionerRoles](https://www.hl7.org/fhir/practitionerrole.html), [Organizations](https://www.hl7.org/fhir/organization.html), [Locations](https://www.hl7.org/fhir/location.html))
- Patient related resources ([Patient](https://www.hl7.org/fhir/patient.html), [Tasks](https://www.hl7.org/fhir/task.html), [CarePlans](https://www.hl7.org/fhir/careplan.html))

To upload these FHIR resources to the local HAPI FHIR server, run the following command from the `configurablecare` directory:

```
curl -X POST -H "Content-Type: application/json" -d \
@./app/src/main/assets/fhir-resources-example.json \
http:/localhost:8080/fhir/
```

#### Care Config
*You may skip this step if you are using the example resources provided in this application. If you are using your own data, you will need to modify `assets/care-config.json`.*

`care-confg.json` is an application level JSON config that allows the user to configure:
- PlanDefinition resources that the application is authorized to access
- Request resources (e.g. Task) that may be created as a result of PlanDefinition `$apply` operation. An example of this is setting the default due date for any Task created as a result of running `$apply` on a particular PlanDefinition.

*This config may be left unaltered when running the application using the sample FHIR resources provided.*

### 4. Run the `configurablecare` app
Now that you have imported the project into Android Studio and set up the FHIR server with test data, you are ready to run the `configurablecare` app for the first time.

Start the Android Studio emulator, and click Run in the Android Studio toolbar.

### Demo
Watch the video below for a demo of the Configurable Care application using the sample FHIR data provided:

https://github.com/MJ1998/fhir-app-examples/assets/112697704/1aa9e3ea-3a5e-4a25-8bb1-f62334a2c164
