/*
 * Copyright 2023 Google LLC
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

import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import java.lang.StringBuilder
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.CommunicationRequest
import org.hl7.fhir.r4.model.Enumerations.RequestResourceType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RequestGroup
import org.hl7.fhir.r4.model.RequestGroup.RequestStatus
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestIntent
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskIntent
import org.hl7.fhir.r4.model.Task.TaskStatus

class RequestManager(
  private var fhirEngine: FhirEngine,
  fhirContext: FhirContext,
  private val requestHandler: RequestHandler,
) {
  private val jsonParser = fhirContext.newJsonParser()

  /** Ensure the Task has a status and intent defined */
  private fun validateTask(task: Task) {
    task.id = UUID.randomUUID().toString()
    if (task.status == null) task.status = TaskStatus.DRAFT
    if (task.intent == null) task.intent = TaskIntent.PROPOSAL
  }

  private fun validateServiceRequest(serviceRequest: ServiceRequest) {
    serviceRequest.id = UUID.randomUUID().toString()
    if (serviceRequest.status == null) serviceRequest.status = ServiceRequestStatus.DRAFT
    serviceRequest.intent = ServiceRequestIntent.PROPOSAL
  }

  private fun validateMedicationRequest(medicationRequest: MedicationRequest) {
    medicationRequest.id = UUID.randomUUID().toString()
    if (medicationRequest.status == null) medicationRequest.status = MedicationRequestStatus.DRAFT
    medicationRequest.intent = MedicationRequestIntent.PROPOSAL
  }

  /** Creates the request given a RequestGroup or RequestOrchestration? */
  suspend fun createRequestFromRequestGroup(requestGroup: RequestGroup): List<Resource> {
    val resourceList: MutableList<Resource> = mutableListOf()
    for (request in requestGroup.contained) {
      when (request) {
        is Task -> validateTask(request)
        is MedicationRequest -> validateMedicationRequest(request)
        is ServiceRequest -> validateServiceRequest(request)
        is CommunicationRequest -> {}
        else -> {}
      }
      fhirEngine.create(request)
      println(jsonParser.encodeResourceToString(request))
      resourceList.add(request)
    }
    return resourceList
  }

  suspend fun getRequestsForPatient(
    patientId: String,
    requestType: ResourceType,
    status: String = "",
    intent: String = ""
  ): List<Resource> {
    val requestList: MutableList<Resource> = mutableListOf()
    if (enumValues<SupportedRequestResources>().any { it.value.toCode() == requestType.name }) {
      val xFhirQueryBuilder = StringBuilder()
      xFhirQueryBuilder.append("${requestType.name}?subject=Patient/$patientId")
      if (status.isNotEmpty()) {
        xFhirQueryBuilder.append("&status=$status")
      }
      if (intent.isNotEmpty()) {
        xFhirQueryBuilder.append("&intent=$intent")
      }
      val searchList = fhirEngine.search(xFhirQueryBuilder.toString())
      for (item in searchList) {
        requestList.add(item.resource)
      }
    }
    return requestList // not a valid or supported request
  }

  private fun getMappedStatus(status: String, resourceType: SupportedRequestResources): String {
    when (resourceType) {
      SupportedRequestResources.TASK -> {
        return when (status) {
          "draft" -> TaskStatus.DRAFT.toCode().lowercase()
          "active" -> TaskStatus.INPROGRESS.toCode().lowercase()
          "on-hold" -> TaskStatus.ONHOLD.toCode().lowercase()
          "completed" -> TaskStatus.COMPLETED.toCode().lowercase()
          "cancelled" -> TaskStatus.CANCELLED.toCode().lowercase()
          "stopped" -> TaskStatus.REJECTED.toCode().lowercase()
          else -> ""
        }
      }
      SupportedRequestResources.MEDICATIONREQUEST -> {
        return when (status) {
          "draft" -> MedicationRequestStatus.DRAFT.toCode().lowercase()
          "active" -> MedicationRequestStatus.ACTIVE.toCode().lowercase()
          "completed" -> MedicationRequestStatus.COMPLETED.toCode().lowercase()
          "cancelled" -> MedicationRequestStatus.CANCELLED.toCode().lowercase()
          "stopped" -> MedicationRequestStatus.STOPPED.toCode().lowercase()
          "on-hold" -> MedicationRequestStatus.ONHOLD.toCode().lowercase()
          else -> ""
        }
      }
      SupportedRequestResources.SERVICEREQUEST -> {
        return when (status) {
          "draft" -> ServiceRequestStatus.DRAFT.toCode().lowercase()
          "active" -> ServiceRequestStatus.ACTIVE.toCode().lowercase()
          "completed" -> ServiceRequestStatus.COMPLETED.toCode().lowercase()
          "cancelled" -> ServiceRequestStatus.REVOKED.toCode().lowercase()
          "stopped" -> ServiceRequestStatus.REVOKED.toCode().lowercase()
          "on-hold" -> ServiceRequestStatus.ONHOLD.toCode().lowercase()
          else -> ""
        }
      }
      else -> return ""
    }
  }

  suspend fun getAllRequestsForPatient(
    patientId: String,
    status: String = "",
    intent: String = ""
  ): List<Resource> {
    val requestList: MutableList<Resource> = mutableListOf()
    for (requestResourceType in SupportedRequestResources.values()) {
      val xFhirQueryBuilder = StringBuilder()
      xFhirQueryBuilder.append("${requestResourceType.value.toCode()}?subject=Patient/$patientId")
      if (status.isNotEmpty()) {
        val mappedStatus = getMappedStatus(status, requestResourceType)
        if (mappedStatus.isNotEmpty()) {
          xFhirQueryBuilder.append("&status=$status")
        }
      }
      if (intent.isNotEmpty()) {
        xFhirQueryBuilder.append("&intent=$intent")
      }
      val searchList = fhirEngine.search(xFhirQueryBuilder.toString())
      for (item in searchList) {
        requestList.add(item.resource)
      }
    }
    return requestList
  }

  private fun isValidStatusTransition(
    currentStatus: RequestStatus?,
    newStatus: RequestStatus,
  ): Boolean {
    if (newStatus == RequestStatus.DRAFT) return currentStatus == RequestStatus.NULL

    if (newStatus == RequestStatus.ACTIVE)
      return (currentStatus == RequestStatus.DRAFT || currentStatus == RequestStatus.ONHOLD)

    if (newStatus == RequestStatus.ONHOLD) return currentStatus == RequestStatus.ACTIVE

    if (newStatus == RequestStatus.COMPLETED) return currentStatus == RequestStatus.ACTIVE

    if (newStatus == RequestStatus.REVOKED) return currentStatus == RequestStatus.ACTIVE

    return newStatus == RequestStatus.ENTEREDINERROR
  }

  private suspend fun handleDoNotPerform(
    medicationRequest: MedicationRequest,
    requestConfiguration: List<RequestConfiguration> = emptyList()
  ) {
    println("Do not perform handler")
    if (medicationRequest.intent == MedicationRequestIntent.PROPOSAL) {
      println(medicationRequest.subject.reference)
      val patientReference = medicationRequest.subject.reference

      val medicationRequestPlan =
        fhirEngine
          .search("MedicationRequest?subject=$patientReference&intent=plan")
          .first()
          .resource as MedicationRequest

      println("order to be cancelled")
      endPlan(
        medicationRequestPlan,
        MedicationRequestStatus.STOPPED,
        "Do Not Perform MedicationRequest received"
      )

      val newMedicationRequestPlan = beginPlan(medicationRequest, emptyList())
      val newMedicationRequestOrder = beginOrder(newMedicationRequestPlan, emptyList())
      endOrder(newMedicationRequestOrder, MedicationRequestStatus.COMPLETED, "Do Not Perform")
    }
  }

  suspend fun evaluateNextStage(
    patientId: String,
    requestList: List<Resource>,
    requestConfiguration: List<RequestConfiguration>
  ) {
    // Workaround to handle CI questionnaire & PD --> this should be represented in the care config
    // If no Do-Not-Perform resources are created when the CI PD is applied, begin order
    if (requestList.isEmpty()) {
      evaluateNextStepsForEmptyResourceList(patientId, requestConfiguration)
    }

    for (request in requestList) {
      when (request) {
        is MedicationRequest -> {
          if (request.doNotPerform) handleDoNotPerform(request, emptyList())
          if (request.intent == MedicationRequestIntent.PROPOSAL) {
            beginProposal(request, requestConfiguration)
          }
        }
        is ServiceRequest -> {
          val patientReference = request.subject.reference
          val medicationRequestPlan =
            fhirEngine
              .search("MedicationRequest?subject=$patientReference&intent=plan")
              .first()
              .resource as MedicationRequest
          medicationRequestPlan.status = MedicationRequestStatus.ONHOLD
          println(jsonParser.encodeResourceToString(medicationRequestPlan))
          request.meta.lastUpdated = Date.from(Instant.now())
          fhirEngine.update(medicationRequestPlan)
        }
        else -> {}
      }
    }
  }

  private suspend fun evaluateNextStepsForEmptyResourceList(
    patientId: String,
    requestConfiguration: List<RequestConfiguration>
  ) {
    // begin order and end plan
    val medicationRequestPlans =
      getRequestsForPatient(patientId, ResourceType.MedicationRequest, intent = "plan")

    for (medicationRequestPlan in medicationRequestPlans) {
      println(
        "moving to order for ${jsonParser.encodeResourceToString(medicationRequestPlans.first())}"
      )
      beginOrder(
        medicationRequestPlan as MedicationRequest,
        requestConfiguration,
        "No Contraindications detected so proceeding with order"
      )
    }
  }

  suspend fun beginProposal(
    medicationRequest: MedicationRequest,
    requestConfiguration: List<RequestConfiguration>
  ) {
    if (medicationRequest.status == MedicationRequestStatus.DRAFT) {
      medicationRequest.status = MedicationRequestStatus.ACTIVE

      if (requestConfiguration.isNotEmpty()) {
        val intentConfig =
          getNextActionForMedicationRequest(medicationRequest, requestConfiguration)

        if (intentConfig != null) {
          if (intentConfig.action == "begin-plan") {
            if (intentConfig.condition != "automatic") {
              medicationRequest.addSupportingInformation(Reference(intentConfig.condition))
            } else { // automatic transition from proposal to plan
              beginPlan(medicationRequest, requestConfiguration, "Auto-acceptance of proposal")
            }
          } else {
            // do nothing
          }
        }
      }
      medicationRequest.meta.lastUpdated = Date.from(Instant.now())
      fhirEngine.update(medicationRequest)
    }
  }

  suspend fun endProposal(
    medicationRequest: MedicationRequest,
    status: MedicationRequestStatus,
    reason: String = ""
  ) {
    if (medicationRequest.status == MedicationRequestStatus.ACTIVE) {
      medicationRequest.status = status
      medicationRequest.statusReason =
        CodeableConcept().addCoding(Coding().apply { display = reason })
      medicationRequest.meta.lastUpdated = Date.from(Instant.now())
      fhirEngine.update(medicationRequest)
    }
  }

  suspend fun beginPlan(
    medicationRequest: MedicationRequest,
    requestConfiguration: List<RequestConfiguration>,
    endProposalMessage: String = ""
  ): MedicationRequest {
    if (medicationRequest.status == MedicationRequestStatus.DRAFT) {
      medicationRequest.status = MedicationRequestStatus.ACTIVE
    }
    if (medicationRequest.status == MedicationRequestStatus.ACTIVE) {
      val newMedicationRequest: MedicationRequest = medicationRequest.copy()
      newMedicationRequest.id = UUID.randomUUID().toString()
      newMedicationRequest.status = MedicationRequestStatus.DRAFT
      newMedicationRequest.intent = MedicationRequestIntent.PLAN
      newMedicationRequest.basedOn.add(Reference(medicationRequest))
      newMedicationRequest.supportingInformation = null

      endProposal(medicationRequest, MedicationRequestStatus.COMPLETED, endProposalMessage)

      if (requestConfiguration.isNotEmpty()) {
        val intentConfig =
          getNextActionForMedicationRequest(newMedicationRequest, requestConfiguration)

        if (intentConfig != null) {
          if (intentConfig.action == "begin-order") {
            if (intentConfig.condition != "automatic") {
              newMedicationRequest.addSupportingInformation(Reference(intentConfig.condition))
            } else { // automatic transition from plan to order
              beginOrder(newMedicationRequest, requestConfiguration, "Auto-acceptance of proposal")
            }
          } else {
            // do nothing
          }
        } else {
          // do nothing
        }
      }

      medicationRequest.meta.lastUpdated = Date.from(Instant.now())
      newMedicationRequest.meta.lastUpdated = Date.from(Instant.now())

      fhirEngine.create(newMedicationRequest)
      fhirEngine.update(medicationRequest)

      return newMedicationRequest
    }
    return MedicationRequest()
  }

  suspend fun endPlan(
    medicationRequest: MedicationRequest,
    status: MedicationRequestStatus,
    reason: String = ""
  ) {
    if (medicationRequest.status == MedicationRequestStatus.ACTIVE ||
        medicationRequest.status == MedicationRequestStatus.DRAFT
    ) {
      medicationRequest.status = status
      medicationRequest.statusReason =
        CodeableConcept().addCoding(Coding().apply { display = reason })
      medicationRequest.meta.lastUpdated = Date.from(Instant.now())
      fhirEngine.update(medicationRequest)
    }
  }

  suspend fun beginOrder(
    medicationRequest: MedicationRequest,
    requestConfiguration: List<RequestConfiguration>,
    endPlanMessage: String = ""
  ): MedicationRequest {
    if (medicationRequest.status == MedicationRequestStatus.DRAFT) {
      medicationRequest.status = MedicationRequestStatus.ACTIVE
    }
    if (medicationRequest.status == MedicationRequestStatus.ACTIVE) {
      val newMedicationRequest: MedicationRequest = medicationRequest.copy()
      newMedicationRequest.id = UUID.randomUUID().toString()
      newMedicationRequest.status = MedicationRequestStatus.DRAFT
      newMedicationRequest.intent = MedicationRequestIntent.ORDER
      newMedicationRequest.basedOn.add(Reference(medicationRequest))
      newMedicationRequest.supportingInformation = null

      endPlan(medicationRequest, MedicationRequestStatus.COMPLETED, endPlanMessage)

      if (requestConfiguration.isNotEmpty()) {
        val intentConfig =
          getNextActionForMedicationRequest(newMedicationRequest, requestConfiguration)

        if (intentConfig != null) {
          if (intentConfig.action == "complete-order") {
            if (intentConfig.condition != "automatic") {
              newMedicationRequest.addSupportingInformation(Reference(intentConfig.condition))
            } else { // automatic completion of order
              endOrder(
                newMedicationRequest,
                MedicationRequestStatus.COMPLETED,
                "Auto-completion of order"
              )
            }
          } else {
            // do nothing
          }
        } else {
          // do nothing
        }
      }

      medicationRequest.meta.lastUpdated = Date.from(Instant.now())
      newMedicationRequest.meta.lastUpdated = Date.from(Instant.now())
      fhirEngine.create(newMedicationRequest)
      fhirEngine.update(medicationRequest)

      return newMedicationRequest
    }
    return MedicationRequest()
  }

  suspend fun endOrder(
    medicationRequest: MedicationRequest,
    status: MedicationRequestStatus,
    reason: String = ""
  ) {
    if (medicationRequest.status == MedicationRequestStatus.ACTIVE ||
        medicationRequest.status == MedicationRequestStatus.DRAFT
    ) {
      medicationRequest.status = status
      medicationRequest.statusReason =
        CodeableConcept().addCoding(Coding().apply { display = reason })
      medicationRequest.meta.lastUpdated = Date.from(Instant.now())
      fhirEngine.update(medicationRequest)
    }
  }

  suspend fun getRequestsCount(patientId: String, status: String = "", intent: String = ""): Int {
    return let { getAllRequestsForPatient(patientId, status = status, intent = intent).count() }
  }

  suspend fun fetchQuestionnaire(questionnaireId: String): Questionnaire {
    return fhirEngine.get(IdType(questionnaireId).idPart)
  }

  companion object {
    enum class SupportedRequestResources(val value: RequestResourceType) {
      TASK(RequestResourceType.TASK),
      MEDICATIONREQUEST(RequestResourceType.MEDICATIONREQUEST),
      SERVICEREQUEST(RequestResourceType.SERVICEREQUEST)
    }

    fun getNextActionForMedicationRequest(
      medicationRequest: MedicationRequest,
      requestConfiguration: List<RequestConfiguration>
    ): RequestConfiguration.IntentCondition? {
      return requestConfiguration
        .firstOrNull { it.requestType == "MedicationRequest" }
        ?.intentConditions?.firstOrNull { it.intent == medicationRequest.intent.toCode() }
    }
  }
}
