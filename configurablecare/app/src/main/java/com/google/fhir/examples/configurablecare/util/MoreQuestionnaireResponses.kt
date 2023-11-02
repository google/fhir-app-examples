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

package com.google.fhir.examples.configurablecare.util

import android.text.Spanned
import androidx.core.text.toSpanned
import com.google.android.fhir.datacapture.descendant
import java.util.Locale
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumeration
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.UriType

/** Pre-order list of all questionnaire response items in the questionnaire. */
val QuestionnaireResponse.allItems: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>
  get() = item.flatMap { it.descendant }

/**
 * Packs repeated groups under the same questionnaire response item.
 *
 * Repeated groups need some massaging before the questionnaire view model can interpret them
 * correctly. This is because they are flattened out and nested directly under the parent in the
 * FHIR data format.
 *
 * More details: https://build.fhir.org/questionnaireresponse.html#link.
 *
 * This function should be called before the questionnaire view model accepts an
 * application-provided questionnaire response.
 *
 * See also [unpackRepeatedGroups].
 */
internal fun QuestionnaireResponse.packRepeatedGroups() {
  item = item.packRepeatedGroups()
}

/**
 * All the enums defined in [org.hl7.fhir.r4.model.Enumerations] have these common methods
 * [fromCode, valueOf, values, getDefinition, getDisplay, getSystem, toCode]. This function converts
 * the high level [org.hl7.fhir.r4.model.Enumerations] of something like
 * [org.hl7.fhir.r4.model.Enumerations.AdministrativeGender] into a corresponding [Coding]. The
 * reason we use reflection here to get the actual value is that [Enumeration] provides a default
 * implementation for some of the apis like [Enumeration.getDisplay] and always return null. So as
 * client, we have to call the desired api on the GenericType passed to the [Enumeration] and get
 * the desired value by calling the api's as described above.
 */
internal fun Enumeration<*>.toCoding(): Coding {
  val enumeration = this
  return Coding().apply {
    display =
      if (enumeration.hasDisplay()) {
        enumeration.display
      } else {
        enumeration.value.invokeFunction("getDisplay") as String?
      }
    code =
      if (enumeration.hasCode()) {
        enumeration.code
      } else {
        enumeration.value.invokeFunction("toCode") as String?
      }
    system =
      if (enumeration.hasSystem()) {
        enumeration.system
      } else {
        enumeration.value.invokeFunction("getSystem") as String?
      }
  }
}

/** Converts StringType to IdType. */
internal fun StringType.toIdType(): IdType {
  return IdType(value)
}

/**
 * Invokes function specified by [functionName] on the calling object with the provided arguments
 * [args]
 */
internal fun Any.invokeFunction(
  functionName: String,
  parameterTypes: List<Class<*>> = listOf(),
  vararg args: Any?,
): Any? =
  this::class
    .java
    .getDeclaredMethod(functionName, *parameterTypes.toTypedArray())
    .apply { isAccessible = true }
    .invoke(this, *args)


/** Converts StringType to CodeType. */
internal fun StringType.toCodeType(): CodeType {
  return CodeType(value)
}

/** Converts Coding to CodeType. */
internal fun Coding.toCodeType(): CodeType {
  return CodeType(code)
}

val Resource.logicalId: String
  get() {
    return this.idElement?.idPart.orEmpty()
  }

/** Converts StringType to toUriType. */
internal fun StringType.toUriType(): UriType {
  return UriType(value)
}

private fun List<QuestionnaireResponse.QuestionnaireResponseItemComponent>.packRepeatedGroups():
  List<QuestionnaireResponse.QuestionnaireResponseItemComponent> {
  forEach { it ->
    it.item = it.item.packRepeatedGroups()
    it.answer.forEach { it.item = it.item.packRepeatedGroups() }
  }
  val linkIdToPackedResponseItems =
    groupBy { it.linkId }
      .mapValues { (linkId, questionnaireResponseItems) ->
        questionnaireResponseItems.singleOrNull()
          ?: QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            this.linkId = linkId
            answer =
              questionnaireResponseItems.map {
                QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                  item = it.item
                }
              }
          }
      }
  return map { it.linkId }.distinct().map { linkIdToPackedResponseItems[it]!! }
}

/**
 * Unpacks repeated groups as separate questionnaire response items under their parent.
 *
 * Repeated groups need some massaging for their returned data-format; each instance of the group
 * should be flattened out to be its own item in the parent, rather than an answer to the main item.
 *
 * More details: https://build.fhir.org/questionnaireresponse.html#link.
 *
 * For example, if the group contains 2 questions, and the user answered the group 3 times, this
 * function will return a list with 3 responses; each of those responses will have the linkId of the
 * provided group, and each will contain an item array with 2 items (the answers to the individual
 * questions within this particular group instance).
 *
 * This function should be called before returning the questionnaire response to the application.
 *
 * See also [packRepeatedGroups].
 */
internal fun QuestionnaireResponse.unpackRepeatedGroups(questionnaire: Questionnaire) {
  item = unpackRepeatedGroups(questionnaire.item, item)
}

/**
 * Returns a list of values built from the elements of `this` and the
 * `questionnaireResponseItemList` with the same linkId using the provided `transform` function
 * applied to each pair of questionnaire item and questionnaire response item.
 *
 * It is assumed that the linkIds are unique in `this` and in `questionnaireResponseItemList`.
 *
 * Although linkIds may appear more than once in questionnaire response, they would not appear more
 * than once within a list of questionnaire response items sharing the same parent.
 */
internal inline fun <T> List<Questionnaire.QuestionnaireItemComponent>.zipByLinkId(
  questionnaireResponseItemList: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
  transform:
    (
    Questionnaire.QuestionnaireItemComponent,
    QuestionnaireResponse.QuestionnaireResponseItemComponent,
  ) -> T,
): List<T> {
  val linkIdToQuestionnaireResponseItemMap = questionnaireResponseItemList.associateBy { it.linkId }
  return mapNotNull { questionnaireItem ->
    linkIdToQuestionnaireResponseItemMap[questionnaireItem.linkId]?.let { questionnaireResponseItem,
      ->
      transform(questionnaireItem, questionnaireResponseItem)
    }
  }
}

private fun unpackRepeatedGroups(
  questionnaireItems: List<Questionnaire.QuestionnaireItemComponent>,
  questionnaireResponseItems: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
): List<QuestionnaireResponse.QuestionnaireResponseItemComponent> {
  return questionnaireItems
    .zipByLinkId(questionnaireResponseItems) { questionnaireItem, questionnaireResponseItem ->
      unpackRepeatedGroups(questionnaireItem, questionnaireResponseItem)
    }
    .flatten()
}

/**
 * Localized and spanned value of [Questionnaire.QuestionnaireItemComponent.text] if translation is
 * present. Default value otherwise.
 */
val Questionnaire.QuestionnaireItemComponent.localizedTextSpanned: Spanned?
  get() = textElement?.getLocalizedText()?.toSpanned()

fun StringType.getLocalizedText(lang: String = Locale.getDefault().toLanguageTag()): String? {
  return getTranslation(lang) ?: getTranslation(lang.split("-").firstOrNull()) ?: value
}

private fun unpackRepeatedGroups(
  questionnaireItem: Questionnaire.QuestionnaireItemComponent,
  questionnaireResponseItem: QuestionnaireResponse.QuestionnaireResponseItemComponent,
): List<QuestionnaireResponse.QuestionnaireResponseItemComponent> {
  questionnaireResponseItem.item =
    unpackRepeatedGroups(questionnaireItem.item, questionnaireResponseItem.item)
  questionnaireResponseItem.answer.forEach {
    it.item = unpackRepeatedGroups(questionnaireItem.item, it.item)
  }
  return if (
    questionnaireItem.type == Questionnaire.QuestionnaireItemType.GROUP && questionnaireItem.repeats
  ) {
    questionnaireResponseItem.answer.map {
      QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
        linkId = questionnaireItem.linkId
        text = questionnaireItem.localizedTextSpanned?.toString()
        item = it.item
      }
    }
  } else {
    listOf(questionnaireResponseItem)
  }
}
