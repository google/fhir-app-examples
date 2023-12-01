# Configurable Care

As an implementer it is easier to build and deploy configurable care delivery applications for different health programs that can leverage evidence based guidelines (e.g. SMART Guidelines). The Configurable Care application is a reference application that enables dynamic deployment of FHIR content to drive care delivery i.e. workflows. It uses the [Android FHIR SDK](https://github.com/google/android-fhir) ([Engine](https://github.com/google/android-fhir/wiki/FHIR-Engine-Library), [DataCapture](https://github.com/google/android-fhir/wiki/Structured-Data-Capture-Library) and [Workflow](https://github.com/google/android-fhir/wiki/Workflow-Library) libraries) to execute workflow logic and provide basic support for managing [CarePlan](https://www.hl7.org/fhir/careplan.html) and [Task](https://www.hl7.org/fhir/task.html) resources.

This application loads the [WHO SMART Guidelines for Immunization (Measles)](https://github.com/WorldHealthOrganization/smart-immunizations-measles/tree/main) and allows the user to run through several different outcomes as described below.


## Running the Application
### 1. Download the code
To download the code, clone the [FHIR App Examples](https://github.com/google/fhir-app-examples) repo: \
`git clone https://github.com/google/fhir-app-examples.git`

### 2. Import the `configurablecare` app into Android Studio

Open Android Studio, select `Open...` and choose the `configurablecare` folder from the source code that you downloaded earlier.

### 3. Run the `configurablecare` app
Now that you have imported the project into Android Studio and set up the FHIR server with test data, you are ready to run the `configurablecare` app for the first time.

Start the Android Studio emulator, and click Run in the Android Studio toolbar.

## Possible Outcomes for the WHO SMART Immunization Guidelines

### Outcome 1 - Successful administration of the Measles vaccine
1. Launch the `configurablecare` app in Android Studio with a connected Android device
2. Open logcat tab in Android Studio and wait until you see this message: `com.google.android.fhir.configurablecare  I  init`. _(You may use `system.out` as a filter to observe the logs better.)_
3. Click on "Register new patient" and complete the questionnaire to create new patient (infant) with basic information. [Suggested DOB: 02-02-2023]
4. Navigate to "Patient list" and choose your newly created patient
5. Click on "Careplan Activities" to see the list of pending and completed activities.
6. Begin the Immunization Review activity. You do not need to fill this form. Click on Submit on the top right.
7. Wait for "Updating Activities" to change to "Activities Updated". Click on the "Completed Activities" tab to see the recently completed task.
8. A new Measles Vaccine Medication Request activity proposal should appear in the "Pending Activities" tab. Click on this Activity to continue.
9. A questionnaire will open up that asks you whether you want to accept this proposal. Click on "Yes" and submit.
10. Notice that the Medication Request activity has now changed from "proposal" to "plan".
11. Click on the above activity. You will see a questionnaire with options for contraindications. Do NOT select anything and submit the empty questionnaire.
12. Notice that the Medication Request activity has now changed from "plan" to "order".
13. Click on the above activity. This will be the questionnaire for administering the measles vaccine.
14. Provide "yes" for "consent given" and "stock available" questions and answer the rest of the questions any way you like. Click on submit when you are done.
15. Now you will notice that all pending activities have been completed. Observe the completed activities as well and take note of the transition from proposal -> plan -> order -> completed
16. Go back to the Patient card and notice that the "Immunization Records" section now has a new item added with the details of this vaccine.

https://github.com/divyaramnath-13/fhir-app-examples/assets/112697704/0be5febc-e5c1-4822-9a2d-95626343582f


### Outcome 2 - Vaccine administration is blocked due to patient being contraindicated
1. Follow steps 1-10 from Exercise 1
2. Instead of submitting an empty questionnaire, select the following contraindications: "Severely immunosuppressed", "History of anaphylactic reactions" and "Severe allergic reactions" and click on submit.
3. You will notice that there are no more pending activities. If you take a look at the completed activities, you will notice a MedicationRequest with the status "Do Not Perform".
4. Go back to the Patient card and notice that the "Alerts" section now has a record that the measles vaccine should not be administered to this patient.

https://github.com/divyaramnath-13/fhir-app-examples/assets/112697704/52e99b28-a406-4b56-ba4c-c1c7187fcf77

### Outcome 3 - Vaccine administration is temporarily on hold (soft contraindication) until a clinical review has been completed [out-of-scope]
1. Follow steps 1-10 from Exercise 1
2. Instead of submitting an empty questionnaire, select the following contraindication: "History of anaphylactic reactions" and click on submit.
3. You will notice that the Medication Request activity is now on-hold until the patient's records have been reviewed

https://github.com/divyaramnath-13/fhir-app-examples/assets/112697704/c5cbb110-2bf3-4ff9-86f7-dedd4c4adcdd


### Outcome 4 - Rejection of the proposal to administer the Measles vaccine
1. Follow steps 1-8 from Exercise 1
2. A questionnaire will open up that asks you whether you want to accept this proposal. Click on "No", provide a reason and submit.
3. You will notice that all pending activities have been completed. In the completed activities section, the Medication Request activity is marked as cancelled due to rejection of the proposal.

https://github.com/divyaramnath-13/fhir-app-examples/assets/112697704/79a9b5ce-9ca2-4778-8438-4e87712211fb

## Disclaimer
This product is not intended to be a medical device.

The FHIR resources provided in this application are intended to be examples that illustrate the utility of Configurable Care. They must NOT be used for any medical purpose under any circumstances.

HL7®, and FHIR® are the registered trademarks of Health Level Seven International and their use of these trademarks does not constitute an endorsement by HL7.
