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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r5.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r5.model.Enumerations.ConceptMapRelationship;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r5.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
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
import com.mdix.fhir.terminology.server.provider.ConceptMapProvider;
import com.mdix.fhir.terminology.server.provider.ValueSetProvider;

import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.PreferReturnEnum;

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
		params.put("source", "http://cdamdmi/encounter-status");
		params.put("target", "http://hl7.org/fhir/ValueSet/encounter-status");
		params.put("code", "active");
		// Object params;
		ResponseEntity<String> response = template.getForEntity(
				"/fhir/ConceptMap/$translate?source=http://cdamdmi/encounter-status&target=http://hl7.org/fhir/ValueSet/encounter-status&code=active",
				String.class, params);
		// assertTrue(response.getStatusCode().equals(HttpStatus.OK));
		System.out.println(response.getBody());

		// Unirest.setTimeouts(0, 0);+
		// HttpResponse<String> response = Unirest.get(
		// "localhost:8080/fhir/ConceptMap/$translate?source=&target=HL70200&code=official&system=system").asString();

	}

	public static List<String> loadMapsxx(String directory) throws Exception {

		// FhirContext fhirContext = FhirContext.forDstu3();
		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
			for (Path path : directoryStream) {
				if (path.getFileName().toString().endsWith("ValueSet.tsv")) {
					ValueSet valueset = new ValueSet();
					UriType valueseturi = new UriType();

					ValueSetComposeComponent compose = new ValueSetComposeComponent();
					ConceptSetComponent include = new ConceptSetComponent();
					int count = 0;
					try {
						for (String line : Files.readAllLines(path)) {

							String[] code2code = line.toString().split("\t");
							if (count == 0) {
								if (code2code[1] != null && !code2code[1].contentEquals("")) {
									valueseturi.setValue(code2code[1]);
									valueset.setUrlElement(valueseturi);

								}
								count++;
								continue;
							}
							if (count > 1) {
								include.addConcept().setCode(code2code[0]).setDisplay(code2code[1]);
								count++;
							} else
								count++;
						}
						compose.addInclude(include);
						valueset.setCompose(compose);
						ValueSetProvider.addValueSet(valueset);
						System.out.println("Loaded ValueSet File " + path.getFileName());
					} catch (FHIRFormatError e) {
						System.out.println("Error loading " + path.getFileName() + e);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.out.println("Error loading " + path.getFileName() + e);
					}

				} else {
					ConceptMap conceptMapFromTo = new ConceptMap();
					ConceptMap conceptMapToFrom = new ConceptMap();
					ConceptMapGroupComponent cmgcFromTo = conceptMapFromTo.addGroup();
					ConceptMapGroupComponent cmgcToFrom = conceptMapToFrom.addGroup();
					int count = 0;
					boolean isValid = false;
					UriType sourceuri = new UriType();
					UriType targeturi = new UriType();

					try {
						for (String line : Files.readAllLines(path)) {

							String[] code2code = line.toString().split("\t");
							if (count < 2) {
								if (count == 0) {
									if ((code2code[1] != null && !code2code[1].contentEquals(""))
											&& (code2code[7] != null && !code2code[7].contentEquals(""))) {
										sourceuri.setValue(code2code[1]);
										targeturi.setValue(code2code[7]);
									}
								}
								count++;
								continue;
							}
							if (code2code.length >= 10) {

								conceptMapFromTo.setSourceScope(sourceuri);
								conceptMapToFrom.setTargetScope(sourceuri);

								conceptMapFromTo.setTargetScope(targeturi);
								conceptMapToFrom.setSourceScope(targeturi);

								cmgcFromTo.setSource(code2code[2]);
								cmgcFromTo.setTarget(code2code[9]);

								cmgcToFrom.setTarget(code2code[2]);
								cmgcToFrom.setSource(code2code[9]);

								if ((code2code[0] != null && !code2code[0].contentEquals(""))
										&& (code2code[6] != null && !code2code[6].contentEquals(""))) {
									SourceElementComponent secFromTo = cmgcFromTo.addElement();
									CodeType aaa = new CodeType();
									secFromTo.setCodeElement(aaa);
									secFromTo.setCode(code2code[0]).addTarget().setCode(code2code[6])
											.setRelationship(ConceptMapRelationship.EQUIVALENT)
											.setDisplay(code2code[8]);

									SourceElementComponent secToFrom = cmgcToFrom.addElement();
									CodeType aaa2 = new CodeType();
									secToFrom.setCodeElement(aaa2);
									secToFrom.setCode(code2code[6]).addTarget().setCode(code2code[0])
											.setRelationship(ConceptMapRelationship.EQUIVALENT)
											.setDisplay(code2code[1]);

									isValid = true;
								} else if ((code2code[3] != null && !code2code[3].contentEquals(""))
										&& (code2code[6] != null && !code2code[6].contentEquals(""))) {
									SourceElementComponent secFromTo = cmgcFromTo.addElement();
									CodeType aaa = new CodeType();
									secFromTo.setCodeElement(aaa);
									secFromTo.setValueSet(code2code[3]).addTarget().setCode(code2code[6])
											.setRelationship(ConceptMapRelationship.EQUIVALENT)
											.setDisplay(code2code[8]);

									isValid = true;
								} else if ((code2code[0] != null && !code2code[0].contentEquals(""))
										&& (code2code[10] != null && !code2code[10].contentEquals(""))) {
									SourceElementComponent secToFrom = cmgcToFrom.addElement();
									CodeType aaa2 = new CodeType();
									secToFrom.setCodeElement(aaa2);
									secToFrom.setValueSet(code2code[10]).addTarget().setCode(code2code[0])
											.setRelationship(ConceptMapRelationship.EQUIVALENT)
											.setDisplay(code2code[1]);

									isValid = true;
								}
							} else {
								System.out.println("Incomplete mapping " + path.getFileName() + " At line "
										+ String.valueOf(count) + " :: " + line);
							}
						}
						if (isValid) {
							ConceptMapProvider.addConceptMap(conceptMapToFrom);
							ConceptMapProvider.addConceptMap(conceptMapFromTo);
							System.out.println("Loaded Mapping File " + path.getFileName());
						} else {
							System.out.println("Did not load Mapping File " + path.getFileName());

						}

					} catch (FHIRFormatError e) {
						System.out.println("Error loading " + path.getFileName() + e);
					} catch (IOException e) {
						System.out.println("Error loading " + path.getFileName() + e);
					}
				}
			}
		}
		return fileNames;
	}

	@Test
	public void loadFromMappings() throws Exception {
		// TerminologyUtil.load(client, "src/test/resources/mappings/loinc");
		loadMapsxx("src/test/resources/mappings/cda");
	}

}
