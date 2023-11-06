/*
 * Copyright 2022-2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.fhir.examples.configurablecare.care

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.knowledge.FhirNpmPackage
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator.Builder
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus
import org.hl7.fhir.r4.model.MetadataResource
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RequestGroup
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.Task

/** Responsible for creating and managing CarePlans */
class CarePlanManager(
  private var fhirEngine: FhirEngine,
  fhirContext: FhirContext,
  private val context: Context,
) {
  private var knowledgeManager = KnowledgeManager.create(context, inMemory = true)
  private var fhirOperator =
    Builder(context.applicationContext)
      .fhirContext(fhirContext)
      .fhirEngine(fhirEngine)
      .knowledgeManager(knowledgeManager)
      .build()

  private var requestManager: RequestManager =
    RequestManager(fhirEngine, fhirContext, TestRequestHandler())
  private var cqlLibraryIdList = ArrayList<String>()
  private val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  private fun writeToFile(resource: Resource): File {
    val fileName =
      if (resource is MetadataResource && resource.name != null) {
        resource.name
      } else {
        "${resource.fhirType()}_${resource.idElement.idPart}"
      }
    return File(context.filesDir, fileName).apply {
      writeText(jsonParser.encodeResourceToString(resource))
    }
  }

  private fun readFileFromAssets(context: Context, filename: String): String {
    return context.assets.open(filename).bufferedReader().use { it.readText() }
  }

  suspend fun fetchKnowledgeResources(path: String) {
    val rootDirectory = File(context.filesDir, path)
    if (rootDirectory.exists()) {
      initializeKnowledgeManager(rootDirectory)
      return
    }
    rootDirectory.mkdirs()

    val fileList = context.assets.list(path)
    if (fileList != null) {
      for (filename in fileList) {
        if (filename.contains(".json")) {
          val contents = readFileFromAssets(context, "$path/$filename")
          try {
            val resource = jsonParser.parseResource(contents)
            if (resource is Resource) {
              fhirEngine.create(resource)

              withContext(Dispatchers.IO) {
                val fis = FileOutputStream(File(context.filesDir, "$path/$filename"))
                fis.write(contents.toByteArray())
                println("Saved: ${context.filesDir}/$path/$filename")
              }
            }
          } catch (exception: Exception) {
            // do nothing
          }
        }
      }
    }
    initializeKnowledgeManager(rootDirectory)
  }

  /**
   * Extracts resources present in PlanDefinition.contained field
   *
   * We cannot use $data-requirements on the [PlanDefinition] yet. So, we assume that all knowledge
   * resources required to $apply a [PlanDefinition] are present within `PlanDefinition.contained`
   *
   * @param planDefinition PlanDefinition resource for which dependent resources are extracted
   */
  suspend fun getPlanDefinitionDependentResources(
    planDefinition: PlanDefinition,
  ): Collection<Resource> {
    var bundleCollection: Collection<Resource> = mutableListOf()

    knowledgeManager.install(writeToFile(planDefinition))
    for (resource in planDefinition.contained) {
      if (resource is Bundle) {
        for (entry in resource.entry) {
          entry.resource.meta.lastUpdated = planDefinition.meta.lastUpdated
          if (entry.resource is Library) {
            cqlLibraryIdList.add(IdType(entry.resource.id).idPart)
            knowledgeManager.install(writeToFile(entry.resource))
          }
          knowledgeManager.install(writeToFile(entry.resource))

          bundleCollection += entry.resource
        }
      }
    }
    return bundleCollection
  }

  private suspend fun initializeKnowledgeManager(rootDirectory: File) {
    knowledgeManager.install(
      FhirNpmPackage(
        "who.fhir.immunization",
        "1.0.0",
        "https://github.com/WorldHealthOrganization/smart-immunizations",
      ),
      rootDirectory,
    )
    println("KM has been initialized")
  }

  /**
   * Executes $apply on a [PlanDefinition] for a [Patient] and creates the request resources as per
   * the proposed [CarePlan]
   *
   * @param planDefinitionId PlanDefinition resource ID for which $apply is run
   * @param patient Patient resource for which the [PlanDefinition] $apply is run
   * @param requestResourceConfigs List of configurations that need to be applied to the request
   * resources as a result of the proposed [CarePlan]
   */
  suspend fun applyPlanDefinitionOnPatient(
    planDefinitionUri: String,
    patient: Patient,
    requestConfiguration: List<RequestConfiguration>,
  ) {
    val patientId = IdType(patient.id).idPart

    println("PlanDefinition: ${CanonicalType(planDefinitionUri)}")
    val carePlanProposal =
      fhirOperator.generateCarePlan(
        planDefinition = CanonicalType(planDefinitionUri),
        subject = "Patient/$patientId"
      ) as CarePlan
    println(jsonParser.encodeResourceToString(carePlanProposal))

    // Fetch existing CarePlan of record for the Patient or create a new one if it does not exist
    val carePlanOfRecord = getCarePlanOfRecordForPatient(patient)

    // Accept the proposed (transient) CarePlan by default and add tasks to the CarePlan of record
    val resourceList = acceptCarePlan(patientId, carePlanProposal, requestConfiguration)

    addRequestResourcesToCarePlanOfRecord(carePlanOfRecord, resourceList)
  }

  /** Fetch the [CarePlan] of record for a given [Patient], if it exists, otherwise create it */
  private suspend fun getCarePlanOfRecordForPatient(patient: Patient): CarePlan {
    val patientId = IdType(patient.id).idPart
    val existingCarePlans = fhirEngine.search("CarePlan?subject=$patientId")

    val carePlanOfRecord = CarePlan()
    return if (existingCarePlans.isEmpty()) {
      carePlanOfRecord.status = CarePlan.CarePlanStatus.ACTIVE
      carePlanOfRecord.subject = Reference(patient)
      carePlanOfRecord.description = "CarePlan of Record"
      fhirEngine.create(carePlanOfRecord)
      carePlanOfRecord
    } else {
      existingCarePlans.first().resource as CarePlan
    }
  }

  /** Update the [CarePlan] to include a reference to the FHIR-define protocol or guideline */
  private fun updateCarePlanWithProtocol(carePlan: CarePlan, uris: List<CanonicalType>) {
    for (uri in uris) carePlan.addInstantiatesCanonical(uri.value)
  }

  /** Link the request resources created for the [Patient] back to the [CarePlan] of record */
  private suspend fun addRequestResourcesToCarePlanOfRecord(
    carePlan: CarePlan,
    requestResourceList: List<Resource>,
  ) {
    for (resource in requestResourceList) {
      when (resource) {
        is Task ->
          carePlan.addActivity().setReference(Reference(resource)).detail.status =
            mapTaskStatusToCarePlanStatus(resource)
        is ServiceRequest ->
          carePlan.addActivity().setReference(Reference(resource)).detail.status =
            mapServiceRequestStatusToCarePlanStatus(resource)
        is MedicationRequest ->
          carePlan.addActivity().setReference(Reference(resource)).detail.status =
            mapMedicationRequestStatusToCarePlanStatus(resource)
        else -> TODO("Not a supported request resource")
      }
    }
    fhirEngine.update(carePlan)
  }

  /** Map [Task] status to [CarePlan] status */
  private fun mapTaskStatusToCarePlanStatus(resource: Task): CarePlan.CarePlanActivityStatus {
    // Refer: http://hl7.org/fhir/R4/valueset-care-plan-activity-status.html for some mapping
    // guidelines
    return when (resource.status) {
      Task.TaskStatus.ACCEPTED -> CarePlan.CarePlanActivityStatus.SCHEDULED
      Task.TaskStatus.DRAFT -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      Task.TaskStatus.REQUESTED -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      Task.TaskStatus.RECEIVED -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      Task.TaskStatus.REJECTED -> CarePlan.CarePlanActivityStatus.STOPPED
      Task.TaskStatus.READY -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      Task.TaskStatus.CANCELLED -> CarePlan.CarePlanActivityStatus.CANCELLED
      Task.TaskStatus.INPROGRESS -> CarePlan.CarePlanActivityStatus.INPROGRESS
      Task.TaskStatus.ONHOLD -> CarePlan.CarePlanActivityStatus.ONHOLD
      Task.TaskStatus.FAILED -> CarePlan.CarePlanActivityStatus.STOPPED
      Task.TaskStatus.COMPLETED -> CarePlan.CarePlanActivityStatus.COMPLETED
      Task.TaskStatus.ENTEREDINERROR -> CarePlan.CarePlanActivityStatus.ENTEREDINERROR
      Task.TaskStatus.NULL -> CarePlan.CarePlanActivityStatus.NULL
      else -> CarePlan.CarePlanActivityStatus.NULL
    }
  }

  private fun mapMedicationRequestStatusToCarePlanStatus(
    resource: MedicationRequest
  ): CarePlan.CarePlanActivityStatus {
    // Refer: http://hl7.org/fhir/R4/valueset-care-plan-activity-status.html for some mapping
    // guidelines
    return when (resource.status) {
      MedicationRequestStatus.ACTIVE -> CarePlan.CarePlanActivityStatus.INPROGRESS
      MedicationRequestStatus.DRAFT -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      else -> CarePlan.CarePlanActivityStatus.fromCode(resource.status.toCode())
    }
  }

  private fun mapServiceRequestStatusToCarePlanStatus(
    resource: ServiceRequest
  ): CarePlan.CarePlanActivityStatus {
    // Refer: http://hl7.org/fhir/R4/valueset-care-plan-activity-status.html for some mapping
    // guidelines
    return when (resource.status) {
      ServiceRequest.ServiceRequestStatus.ACTIVE -> CarePlan.CarePlanActivityStatus.INPROGRESS
      ServiceRequest.ServiceRequestStatus.REVOKED -> CarePlan.CarePlanActivityStatus.CANCELLED
      ServiceRequest.ServiceRequestStatus.DRAFT -> CarePlan.CarePlanActivityStatus.NOTSTARTED
      else -> CarePlan.CarePlanActivityStatus.fromCode(resource.status.toCode())
    }
  }

  /** Add the [CarePlan] reference to the request resources */
  private suspend fun linkRequestResourcesToCarePlan(
    carePlan: CarePlan,
    requestResourceList: List<Resource>,
  ) {
    for (resource in requestResourceList) {
      when (resource.fhirType()) {
        "Task" -> {} // requestManager.linkCarePlanToRequestResource(resource as Task, carePlan)
        "ServiceRequest" -> TODO("Not supported yet")
        "MedicationRequest" -> TODO("Not supported yet")
        "SupplyRequest" -> TODO("Not supported yet")
        "Procedure" -> TODO("Not supported yet")
        "DiagnosticReport" -> TODO("Not supported yet")
        "Communication" -> TODO("Not supported yet")
        "CommunicationRequest" -> TODO("Not supported yet")
        else -> TODO("Not a valid request resource")
      }
    }
  }

  private suspend fun acceptCarePlan(
    patientId: String,
    proposedCarePlan: CarePlan,
    requestConfiguration: List<RequestConfiguration>,
  ): List<Resource> {
    // modify this and use:
    val resourceList: MutableList<Resource> = mutableListOf()
    for (request in proposedCarePlan.contained) {
      if (request is RequestGroup) {
        resourceList.addAll(requestManager.createRequestFromRequestGroup(request))
      }
    }

    requestManager.evaluateNextStage(patientId, resourceList, requestConfiguration)
    return resourceList
  }
}
