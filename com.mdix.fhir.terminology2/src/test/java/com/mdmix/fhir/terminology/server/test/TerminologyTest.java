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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import com.mdix.fhir.terminology.server.FhirTerminologyServerApplication;

@SpringBootTest(classes = FhirTerminologyServerApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class TerminologyTest {

	@Autowired
	private TestRestTemplate template;

	@Test
	public void testGetConceptMaps() {
		ResponseEntity<String> response = template.getForEntity("fhir/ConceptMap", String.class);
		// assertTrue(response.getStatusCode().equals(HttpStatus.OK));
		System.out.println(response.getBody());
	}

}
