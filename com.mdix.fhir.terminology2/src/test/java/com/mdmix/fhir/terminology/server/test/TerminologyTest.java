/*******************************************************************************
 * Copyright (c) 2018 seanmuir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir - initial API and implementation
 *
 *******************************************************************************/
package com.mdmix.fhir.terminology.server.test;

import java.util.HashMap;

import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.mdix.fhir.terminology.server.FhirTerminologyServerApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FhirTerminologyServerApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class TerminologyTest {

	@Autowired
	private TestRestTemplate template;

	@BeforeAll
	public static void setEnvironment() {
		System.setProperty("fhir.conceptmaps", "src/main/resources/conceptmaps");
	}

	@Test
	public void testGetConceptMaps() {

		System.out.println(template.getRootUri());

		ResponseEntity<String> response = template.getForEntity("/fhir/ConceptMap", String.class);
		// assertTrue(response.getStatusCode().equals(HttpStatus.OK));
		System.out.println(response.getBody());
	}

	@Test
	public void testTransform() {

		// ;template.get

		HashMap<String, Object> params = new HashMap<>();
		params.put("source", "http://hl7.org/fhir/name-use");
		params.put("target", "HL70200");
		params.put("code", "official");
		// Object params;
		ResponseEntity<String> response = template.getForEntity(
			"/fhir/ConceptMap/$translate?source=http://hl7.org/fhir/name-use&target=HL70200&code=official&system=system",
			String.class, params);
		// assertTrue(response.getStatusCode().equals(HttpStatus.OK));
		System.out.println(response.getBody());

		// Unirest.setTimeouts(0, 0);
		// HttpResponse<String> response = Unirest.get(
		// "localhost:8080/fhir/ConceptMap/$translate?source=&target=HL70200&code=official&system=system").asString();

	}

}
