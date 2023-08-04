/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gcfv2;

import com.google.api.services.healthcare.v1.CloudHealthcareScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.common.io.CharStreams;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FhirDatastoreProxy implements HttpFunction {

  private static final Logger logger = LoggerFactory.getLogger(FhirDatastoreProxy.class);

  private static final String CLOUD_FUNCTIONS_ENDPOINT = System.getenv("CLOUD_FUNCTIONS_ENDPOINT");
  private static final String PROJECT_ID = System.getenv("PROJECT_ID");
  private static final String DATASET_LOCATION = System.getenv("DATASET_LOCATION");
  private static final String DATASET_ID = System.getenv("DATASET_ID");
  private static final String FHIR_STORE_ID = System.getenv("FHIR_STORE_ID");
  private static final String FHIR_STORE_NAME = String.format(
      "https://healthcare.googleapis.com/v1beta1/projects/%s/locations/%s/datasets/%s/fhirStores/%s",
      PROJECT_ID, DATASET_LOCATION, DATASET_ID, FHIR_STORE_ID);

  public void service(final HttpRequest request, final HttpResponse response) throws Exception {

    logger.info(String.format("The request received is %s", request.getUri()));

    final BufferedWriter writer = response.getWriter();
    switch (request.getMethod()) {
      case "GET" -> fhirResourceGet(request.getPath(), request.getQueryParameters(), writer);
      case "POST" -> fhirResourcePost(CharStreams.toString(request.getReader()), writer);
      default -> throw new RuntimeException();
    }
  }

  private void fhirResourceGet(String resourceType, Map<String, List<String>> searchParams,
      BufferedWriter writer) {
    try {
      HttpClient httpClient = HttpClients.createDefault();
      String fhirStoreUrl = String.format("%s/fhir%s",  FHIR_STORE_NAME, resourceType);
      URIBuilder uriBuilder = new URIBuilder(fhirStoreUrl);

      for (String paramName : searchParams.keySet()) {
        for (String paramValue : searchParams.get(paramName)) {
          uriBuilder.addParameter(paramName, paramValue);
        }
      }

      HttpUriRequest getRequest = RequestBuilder.get()
          .addHeader("Authorization", String.format("Bearer %s", getAccessToken()))
          .setUri(uriBuilder.build())
          .build();
      logger.info(String.format("The request to send will be %s", getRequest.getURI()));
      org.apache.http.HttpResponse response = httpClient.execute(getRequest);
      processResponse(writer, response);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private void fhirResourcePost(String body, BufferedWriter writer) {
    try {
      HttpClient httpClient = HttpClients.createDefault();
      String fhirStoreUrl = String.format("%s/fhir", FHIR_STORE_NAME);

      HttpUriRequest postRequest = RequestBuilder
          .post()
          .addHeader("Authorization", String.format("Bearer %s", getAccessToken()))
          .addHeader("Content-Type", "application/fhir+json")
          .addHeader("Accept-Charset", "utf-8")
          .addHeader("Accept", "application/fhir+json; charset=utf-8")
          .setUri(new URIBuilder(fhirStoreUrl).build())
          .setEntity(new StringEntity(body))
          .build();

      org.apache.http.HttpResponse response = httpClient.execute(postRequest);

      processResponse(writer, response);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void processResponse(BufferedWriter writer,
      org.apache.http.HttpResponse response) throws IOException {
    HttpEntity responseEntity = response.getEntity();
    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      String errorMessage =
          String.format(
              "Exception retrieving FHIR resource: %s\n", response.getStatusLine().toString());
      logger.error(errorMessage);
      responseEntity.writeTo(System.err);
      throw new RuntimeException(errorMessage);
    }
    InputStreamReader reader = new InputStreamReader(responseEntity.getContent(),
        StandardCharsets.UTF_8);

    replaceAndCopyResponse(new BufferedReader(reader), writer);
  }


  private static void replaceAndCopyResponse(Reader entityContentReader, Writer writer)
      throws IOException {
    // To make this more efficient, this only does a string search/replace; we may need to add
    // proper URL parsing if we need to address edge cases in URL no-op changes. This string
    // matching can be done more efficiently if needed, but we should avoid loading the full
    // stream in memory.
    String fhirStoreUrl = String.format("%s/fhir", FHIR_STORE_NAME);
    int numMatched = 0;
    int n;
    while ((n = entityContentReader.read()) >= 0) {
      char c = (char) n;
      if (fhirStoreUrl.charAt(numMatched) == c) {
        numMatched++;
        if (numMatched == fhirStoreUrl.length()) {
          // A match found; replace it with proxy's base URL.
          writer.write(CLOUD_FUNCTIONS_ENDPOINT);
          numMatched = 0;
        }
      } else {
        writer.write(fhirStoreUrl.substring(0, numMatched));
        writer.write(c);
        numMatched = 0;
      }
    }
    if (numMatched > 0) {
      // Handle any remaining characters that partially matched.
      writer.write(fhirStoreUrl.substring(0, numMatched));
    }
    writer.close();
  }

  private static String getAccessToken() throws IOException {
    GoogleCredentials credential =
        GoogleCredentials.getApplicationDefault()
            .createScoped(Collections.singleton(CloudHealthcareScopes.CLOUD_PLATFORM));

    return credential.refreshAccessToken().getTokenValue();
  }
}
