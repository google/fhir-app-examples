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
package com.google.fhir.examples.configurablecare

import android.app.Application
import android.content.res.Resources
import android.icu.text.DateFormat
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Patient

/**
 * The ViewModel helper class for PatientItemRecyclerViewAdapter, that is responsible for preparing
 * data for UI.
 */
class PatientDetailsViewModel(
  application: Application,
  private val fhirEngine: FhirEngine,
  private val patientId: String
) : AndroidViewModel(application) {
  val livePatientData = MutableLiveData<List<PatientDetailData>>()

  /** Emits list of [PatientDetailData]. */
  fun getPatientDetailData() {
    viewModelScope.launch { livePatientData.value = getPatientDetailDataModel() }
  }

  private suspend fun getPatient(): PatientListViewModel.PatientItem {
    val patient = fhirEngine.get<Patient>(patientId)
    return patient.toPatientItem(0)
  }

  private suspend fun getPatientImmunizationRecords(): List<PatientListViewModel.ImmunizationItem> {
    val immunizations: MutableList<PatientListViewModel.ImmunizationItem> = mutableListOf()
    fhirEngine
      .search<Immunization> { filter(Immunization.PATIENT, { value = "Patient/$patientId" }) }
      .take(MAX_RESOURCE_COUNT)
      .map { createImmunizationItem(it.resource, getApplication<Application>().resources) }
      .let { immunizations.addAll(it) }
    return immunizations
  }

  private suspend fun getPatientMedicalAlerts(): List<PatientListViewModel.MedicalAlertItem> {
    val medicationRequests: MutableList<PatientListViewModel.MedicalAlertItem> = mutableListOf()
    fhirEngine
      .search<MedicationRequest> {
        filter(MedicationRequest.SUBJECT, { value = "Patient/$patientId" })
      }
      .filter {
        it.resource.doNotPerform &&
          it.resource.intent == MedicationRequest.MedicationRequestIntent.ORDER
      }
      .map { createMedicalAlertItem(it.resource, getApplication<Application>().resources) }
      .let { medicationRequests.addAll(it) }
    return medicationRequests
  }

  private suspend fun getPatientDetailDataModel(): List<PatientDetailData> {
    val data = mutableListOf<PatientDetailData>()
    val patient = getPatient()
    val immunizationRecords = getPatientImmunizationRecords()
    val medicalAlerts = getPatientMedicalAlerts()

    patient.let { patientItem ->
      data.add(PatientDetailOverview(patientItem, firstInGroup = true))
      data.add(
        PatientDetailProperty(
          PatientProperty(getString(R.string.patient_property_id), patientItem.resourceId)
        )
      )
      data.add(
        PatientDetailProperty(
          PatientProperty(
            getString(R.string.patient_property_dob),
            patientItem.dob?.localizedString ?: ""
          )
        )
      )
      data.add(
        PatientDetailProperty(
          PatientProperty(
            getString(R.string.patient_property_gender),
            patientItem.gender.replaceFirstChar {
              if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
          ),
          lastInGroup = true
        )
      )
    }

    if (immunizationRecords.isNotEmpty()) {
      data.add(PatientDetailHeader(getString(R.string.header_immunization)))

      val immunizationDataModel =
        immunizationRecords.mapIndexed { index, immunizationItem ->
          PatientDetailObservation(
            immunizationItem,
            firstInGroup = index == 0,
            lastInGroup = index == immunizationRecords.size - 1
          )
        }
      data.addAll(immunizationDataModel)
    }

    if (medicalAlerts.isNotEmpty()) {
      data.add(PatientDetailHeader(getString(R.string.header_conditions)))
      val conditionDataModel =
        medicalAlerts.mapIndexed { index, conditionItem ->
          PatientDetailCondition(
            conditionItem,
            firstInGroup = index == 0,
            lastInGroup = index == medicalAlerts.size - 1
          )
        }
      data.addAll(conditionDataModel)
    }

    return data
  }

  private val LocalDate.localizedString: String
    get() {
      val date = Date.from(atStartOfDay(ZoneId.systemDefault())?.toInstant())
      return if (isAndroidIcuSupported())
        DateFormat.getDateInstance(DateFormat.DEFAULT).format(date)
      else SimpleDateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault()).format(date)
    }

  // Android ICU is supported API level 24 onwards.
  private fun isAndroidIcuSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

  private fun getString(resId: Int) = getApplication<Application>().resources.getString(resId)

  companion object {
    /**
     * Creates ObservationItem objects with displayable values from the Fhir Observation objects.
     */
    private fun createImmunizationItem(
      immunization: Immunization,
      resources: Resources
    ): PatientListViewModel.ImmunizationItem {
      var immunizationCode = ""
      var immunizationDisplay = ""

      if (immunization.hasVaccineCode() && immunization.vaccineCode.hasCoding()) {
        immunizationCode = immunization.vaccineCode.codingFirstRep.code
        immunizationDisplay = immunization.vaccineCode.codingFirstRep.display
      }

      return PatientListViewModel.ImmunizationItem(
        immunization.logicalId,
        "$immunizationDisplay: $immunizationCode | Lot: ${immunization.lotNumber}",
        immunizationDisplay,
        "${immunization.meta.lastUpdated}"
      )
    }

    /**
     * Creates MedicalAlertItem objects which displays MedicationRequest resources for which
     * doNotSubmit is true.
     */
    private fun createMedicalAlertItem(
      medicationRequest: MedicationRequest,
      resources: Resources
    ): PatientListViewModel.MedicalAlertItem {
      val code = medicationRequest.medicationCodeableConcept.codingFirstRep.code
      val display = medicationRequest.medicationCodeableConcept.codingFirstRep.display

      // Show nothing if no values available for datetime and value quantity.
      val medicationRequestDisplay = "$display [$code]: DO NOT PERFORM"

      return PatientListViewModel.MedicalAlertItem(
        medicationRequest.logicalId,
        medicationRequestDisplay,
        "",
        "${medicationRequest.meta.lastUpdated}"
      )
    }
  }
}

interface PatientDetailData {
  val firstInGroup: Boolean
  val lastInGroup: Boolean
}

data class PatientDetailHeader(
  val header: String,
  override val firstInGroup: Boolean = false,
  override val lastInGroup: Boolean = false
) : PatientDetailData

data class PatientDetailProperty(
  val patientProperty: PatientProperty,
  override val firstInGroup: Boolean = false,
  override val lastInGroup: Boolean = false
) : PatientDetailData

data class PatientDetailOverview(
  val patient: PatientListViewModel.PatientItem,
  override val firstInGroup: Boolean = false,
  override val lastInGroup: Boolean = false
) : PatientDetailData

data class PatientDetailObservation(
  val observation: PatientListViewModel.ImmunizationItem,
  override val firstInGroup: Boolean = false,
  override val lastInGroup: Boolean = false
) : PatientDetailData

data class PatientDetailCondition(
  val condition: PatientListViewModel.MedicalAlertItem,
  override val firstInGroup: Boolean = false,
  override val lastInGroup: Boolean = false
) : PatientDetailData

data class PatientProperty(val header: String, val value: String)

class PatientDetailsViewModelFactory(
  private val application: Application,
  private val fhirEngine: FhirEngine,
  private val patientId: String
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    require(modelClass.isAssignableFrom(PatientDetailsViewModel::class.java)) {
      "Unknown ViewModel class"
    }
    return PatientDetailsViewModel(application, fhirEngine, patientId) as T
  }
}

data class RiskAssessmentItem(
  var riskStatusColor: Int,
  var riskStatus: String,
  var lastContacted: String,
  var patientCardColor: Int
)
