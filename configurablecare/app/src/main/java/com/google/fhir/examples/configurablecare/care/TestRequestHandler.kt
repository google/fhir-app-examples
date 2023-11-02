package com.google.fhir.examples.configurablecare.care

import com.google.fhir.examples.configurablecare.care.RequestHandler
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Task

class TestRequestHandler: RequestHandler {

  override fun acceptProposedRequest(request: Resource): Boolean {
    if (request is Task)
      request.status = Task.TaskStatus.ACCEPTED
    return true
  }
}