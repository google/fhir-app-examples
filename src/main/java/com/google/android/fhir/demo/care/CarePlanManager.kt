/*
 * Copyright 2022 Google LLC
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

package com.google.android.fhir.demo.care

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import java.io.File
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CarePlan.CarePlanActivityStatus
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.MetadataResource
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task

class CarePlanManager(
  private var fhirEngine: FhirEngine,
  private var knowledgeManager: KnowledgeManager,
  private var fhirOperator: FhirOperator,
  private val taskManager: TaskManager,
  private val context: Context
) {
  private var planDefinitionIdList = ArrayList<String>()
  private var cqlLibraryIdList = ArrayList<String>()
  private val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  private fun writeToFile(resource: Resource): File {
    val fileName =
      if (resource is MetadataResource && resource.name != null) {
        resource.name
      } else {
        resource.idElement.idPart
      }
    return File(context.filesDir, fileName).apply {
      writeText(jsonParser.encodeResourceToString(resource))
    }
  }
  suspend fun getPlanDefinitionDependentResources(planDefinition: PlanDefinition): Collection<Resource> {
    var bundleCollection: Collection<Resource> = mutableListOf()

    addPlanDefinition(IdType(planDefinition.id).idPart)

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

  private suspend fun loadCarePlanResourcesFromDb() {
    // Load Library resources
    val availableCqlLibraries = fhirEngine.search<Library> {}
    val availablePlanDefinitions = fhirEngine.search<PlanDefinition> {}
    for (cqlLibrary in availableCqlLibraries) {
      knowledgeManager.install(writeToFile(cqlLibrary))
      cqlLibraryIdList.add(IdType(cqlLibrary.id).idPart)
    }
    for (planDefinition in availablePlanDefinitions) {
      getPlanDefinitionDependentResources(planDefinition)
    }
  }

  private fun addPlanDefinition(planDefinitionId: String) {
    planDefinitionIdList.add(IdType(planDefinitionId).idPart)
  }

  suspend fun applyPlanDefinitionOnPatient(planDefinitionId: String, patient: Patient) {
    val patientId = IdType(patient.id).idPart

    if (cqlLibraryIdList.isEmpty()) {
      loadCarePlanResourcesFromDb()
    }

    val carePlanProposal =
      fhirOperator.generateCarePlan(planDefinitionId = planDefinitionId, patientId = patientId)
        as CarePlan

    // Fetch existing CarePlan of record for the Patient or create a new one if it does not exist
    val carePlanOfRecord = getCarePlanOfRecordForPatient(patient)

    // Accept the proposed (transient) CarePlan by default and add tasks to the CarePlan of record
    acceptCarePlan(carePlanProposal, carePlanOfRecord)
  }

  suspend fun applyAllPlanDefinitionsOnPatient(patient: Patient) {
    val patientId = IdType(patient.id).idPart

    if (cqlLibraryIdList.isEmpty()) {
      loadCarePlanResourcesFromDb()
    }
    for (planDefinitionId in planDefinitionIdList) {
      val carePlanProposal =
        fhirOperator.generateCarePlan(planDefinitionId = planDefinitionId, patientId = patientId)
          as CarePlan

      // Fetch existing CarePlan of record for the Patient or create a new one if it does not exist
      val carePlanOfRecord = getCarePlanOfRecordForPatient(patient)

      // Accept the proposed (transient) CarePlan by default and add tasks to the CarePlan of record
      acceptCarePlan(carePlanProposal, carePlanOfRecord)
    }
  }

  suspend fun applyPlanDefinitionOnMultiplePatients(
    planDefinitionId: String,
    patientList: List<Patient>
  ) {
    if (cqlLibraryIdList.isEmpty()) loadCarePlanResourcesFromDb()

    for (patient in patientList) {
      val patientId = IdType(patient.id).idPart

      val carePlanProposal =
        fhirOperator.generateCarePlan(planDefinitionId = planDefinitionId, patientId = patientId)
          as CarePlan

      // Fetch existing CarePlan of record for the Patient or create a new one if it does not exist
      val carePlanOfRecord = getCarePlanOfRecordForPatient(patient)

      // Accept the proposed (transient) CarePlan by default and add tasks to the CarePlan of record
      acceptCarePlan(carePlanProposal, carePlanOfRecord)
    }
  }

  suspend fun applyAllPlanDefinitionsOnMultiplePatients(patientList: List<Patient>) {

    if (cqlLibraryIdList.isEmpty()) loadCarePlanResourcesFromDb()

    for (patient in patientList) {
      val patientId = IdType(patient.id).idPart

      for (planDefinitionId in planDefinitionIdList) {
        val carePlanProposal =
          fhirOperator.generateCarePlan(planDefinitionId = planDefinitionId, patientId = patientId)
            as CarePlan

        // Fetch existing CarePlan of record for the Patient or create a new one if it does not
        // exist
        val carePlanOfRecord = getCarePlanOfRecordForPatient(patient)

        // Accept the proposed (transient) CarePlan by default and add tasks to the CarePlan of
        // record
        acceptCarePlan(carePlanProposal, carePlanOfRecord)
      }
    }
  }

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
      existingCarePlans.first() as CarePlan
    }
  }

  private fun updateCarePlanWithProtocol(carePlan: CarePlan, uris: List<CanonicalType>) {
    for (uri in uris) carePlan.addInstantiatesCanonical(uri.value)
  }

  private fun addRequestResourcesToCarePlanOfRecord(
    carePlan: CarePlan,
    requestResourceList: List<Resource>
  ) {
    for (resource in requestResourceList) {
      when (resource.fhirType()) {
        "Task" ->
          carePlan.addActivity().setReference(Reference(resource)).detail.status =
            taskManager.mapRequestResourceStatusToCarePlanStatus(resource as Task)
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

  private suspend fun linkRequestResourcesToCarePlan(
    carePlan: CarePlan,
    requestResourceList: List<Resource>
  ) {
    for (resource in requestResourceList) {
      when (resource.fhirType()) {
        "Task" -> taskManager.linkCarePlanToRequestResource(resource as Task, carePlan)
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

  private suspend fun createProposedRequestResources(resourceList: List<Resource>): List<Resource> {
    val createdRequestResources = ArrayList<Resource>()
    for (resource in resourceList) {
      when (resource.fhirType()) {
        "Task" -> {
          val task = taskManager.createRequestResource(resource as Task)
          createdRequestResources.add(task)
        }
        "ServiceRequest" -> TODO("Not supported yet")
        "MedicationRequest" -> TODO("Not supported yet")
        "SupplyRequest" -> TODO("Not supported yet")
        "Procedure" -> TODO("Not supported yet")
        "DiagnosticReport" -> TODO("Not supported yet")
        "Communication" -> TODO("Not supported yet")
        "CommunicationRequest" -> TODO("Not supported yet")
        "RequestGroup" -> {}
        else -> TODO("Not a valid request resource")
      }
    }
    return createdRequestResources
  }

  private suspend fun acceptCarePlan(proposedCarePlan: CarePlan, carePlanOfRecord: CarePlan) {
    val resourceList = createProposedRequestResources(proposedCarePlan.contained)
    updateCarePlanWithProtocol(carePlanOfRecord, proposedCarePlan.instantiatesCanonical)
    addRequestResourcesToCarePlanOfRecord(carePlanOfRecord, resourceList)

    fhirEngine.update(carePlanOfRecord)
    linkRequestResourcesToCarePlan(carePlanOfRecord, resourceList)
  }

  private suspend fun updateCarePlanStatus(
    carePlan: CarePlan,
    requestedActivityResource: Resource,
    carePlanActivityStatus: CarePlanActivityStatus,
    outcomeActivityReferences: List<Reference>
  ) {
    if (carePlan.isEmpty) return
    for (activity in carePlan.activity) {
      if (activity.reference.reference.equals(
          requestedActivityResource.fhirType() + "/" + IdType(requestedActivityResource.id).idPart
        )
      ) {
        activity.detail.status = carePlanActivityStatus
        activity.outcomeReference = outcomeActivityReferences
        fhirEngine.update(carePlan)
        break
      }
    }
  }

  suspend fun updateCarePlanActivity(
    requestResource: Resource,
    requestResourceStatus: String,
    outcomeActivityReferences: List<Reference>,
    updateCarePlan: Boolean = true
  ) {
    val carePlanActivityStatus: CarePlanActivityStatus
    val carePlan: CarePlan
    when (requestResource.fhirType()) {
      "Task" -> {
        taskManager.updateRequestResourceStatus(requestResource as Task, requestResourceStatus)
        if (updateCarePlan) {
          carePlanActivityStatus =
            taskManager.mapRequestResourceStatusToCarePlanStatus(requestResource)
          carePlan =
            if (requestResource.hasBasedOn())
              fhirEngine.get(
                ResourceType.CarePlan,
                IdType(requestResource.basedOnFirstRep.referenceElement.value).idPart
              ) as CarePlan
            else return
          updateCarePlanStatus(carePlan, requestResource, carePlanActivityStatus, outcomeActivityReferences)
        }
      }
      "ServiceRequest" -> TODO("Not supported yet")
      "MedicationRequest" -> TODO("Not supported yet")
      "SupplyRequest" -> TODO("Not supported yet")
      "Procedure" -> TODO("Not supported yet")
      "DiagnosticReport" -> TODO("Not supported yet")
      "Communication" -> TODO("Not supported yet")
      "CommunicationRequest" -> TODO("Not supported yet")
      "RequestGroup" -> {}
      else -> TODO("Not a valid request resource")
    }
  }
}
