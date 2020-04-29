/*******************************************************************************
 * Copyright (c) 2017 seanmuir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir - initial API and implementation
 *
 *******************************************************************************/
package sample.fhir.server.jersey.provider;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.dstu3.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.dstu3.model.Enumerations.ConceptMapEquivalence;
import org.hl7.fhir.dstu3.model.UriType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * @author seanmuir
 *
 */
public class TerminologyUtil {

	/**
	 * @TODO Implement command line load of mappings passing in mapping source and target server
	 * @param args
	 */

	public static void main(String[] args) {
		// String host = args[0];
		String directory = args[0];
		FhirContext ourCtx = FhirContext.forDstu3();
		IGenericClient client = ourCtx.newRestfulGenericClient("localhost:8180/asdfasdfasdfterminology/fhir");
		client.setEncoding(EncodingEnum.JSON);
		client.registerInterceptor(new LoggingInterceptor(true));
		load(client, directory);
	}

	public static List<String> load(IGenericClient client, String directory) {

		// FhirContext fhirContext = FhirContext.forDstu3();
		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
			for (Path path : directoryStream) {
				if (Files.isDirectory(path)) {
					continue;
				}

				ConceptMap conceptMapFromTo = new ConceptMap();
				ConceptMap conceptMapToFrom = new ConceptMap();
				ConceptMapGroupComponent cmgcFromTo = conceptMapFromTo.addGroup();
				ConceptMapGroupComponent cmgcToFrom = conceptMapToFrom.addGroup();
				boolean firstLine = true;

				for (String line : Files.readAllLines(path)) {
					String[] code2code = line.toString().split("\t");
					if (firstLine) {
						firstLine = false;

						if (code2code.length == 4) {

							UriType sourceuri = new UriType();
							sourceuri.setValue(code2code[0]);

							conceptMapFromTo.setUrl(code2code[0]);

							conceptMapFromTo.setSource(sourceuri);

							conceptMapToFrom.setTarget(sourceuri);

							UriType targeturi = new UriType();
							targeturi.setValue(code2code[2]);

							conceptMapFromTo.setTarget(targeturi);
							conceptMapToFrom.setSource(targeturi);

							conceptMapFromTo.setId(java.util.UUID.randomUUID().toString());

							cmgcFromTo.setSource(code2code[1]);
							cmgcFromTo.setTarget(code2code[3]);

							cmgcToFrom.setTarget(code2code[1]);
							cmgcToFrom.setSource(code2code[3]);

						} else {
							System.out.println("invalid " + line);
						}
					} else {
						if (code2code.length == 4) {

							SourceElementComponent secFromTo = cmgcFromTo.addElement();
							CodeType aaa = new CodeType();
							secFromTo.setCodeElement(aaa);
							secFromTo.setCode(code2code[0]).addTarget().setCode(code2code[2]).setEquivalence(
								ConceptMapEquivalence.EQUAL);

							SourceElementComponent secToFrom = cmgcToFrom.addElement();
							CodeType aaa2 = new CodeType();
							secToFrom.setCodeElement(aaa2);
							secToFrom.setCode(code2code[2]).addTarget().setCode(code2code[0]).setEquivalence(
								ConceptMapEquivalence.EQUAL);

						} else {
							System.out.println("invalid " + line);
						}
					}
				}

				// System.out.println(
				// "Appointment JSon::" +
				// ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(conceptMapFromTo));

				client.setEncoding(EncodingEnum.JSON);
				final MethodOutcome results = client.create().resource(conceptMapFromTo).prefer(
					PreferReturnEnum.REPRESENTATION).execute();
				client.create().resource(conceptMapToFrom).prefer(PreferReturnEnum.REPRESENTATION).execute();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return fileNames;
	}

}
