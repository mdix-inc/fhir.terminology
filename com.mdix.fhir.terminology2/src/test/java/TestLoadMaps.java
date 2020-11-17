//package sample.fhir.server.jersey;

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
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.util.StringUtils;

//import com.sun.java.util.jar.pack.Package.File;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
//import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ch.qos.logback.classic.Level;
//import ca.uhn.fhir.rest.server.EncodingEnum;
//import sample.fhir.server.jersey.provider.TerminologyUtil;

public class TestLoadMaps {

	// http://localhost:8080/fhir/ConceptMap/$translate?code=C0349375&source=xxxxxxxxxx&target=1111222233334444
	// VAVistA 2.16.840.1.113883.6.233 http://hl7.org/fhir/ValueSet/v3-ReligiousAffiliation

	// http://localhost:8080/fhir/ConceptMap/$translate?code=DIVINATION&source=VAVistA&target=http://hl7.org/fhir/ValueSet/v3-ReligiousAffiliation
	private static IGenericClient client;

	private static FhirContext ourCtx = FhirContext.forDstu3();

	private static int ourPort = 8080;

	private static String HOST = "http://localhost:";

	@BeforeClass
	public static void setUpClass() throws Exception {

		ourCtx.getRestfulClientFactory().setConnectTimeout(50000);
		ourCtx.getRestfulClientFactory().setSocketTimeout(10000000);
		client = ourCtx.newRestfulGenericClient("http://ec2-18-188-214-103.us-east-2.compute.amazonaws.com:8080/fhir");
		client.setEncoding(EncodingEnum.JSON);
		client.registerInterceptor(new LoggingInterceptor(true));
	    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.OFF);
	}

	public static List<String> loadMapsxx(String directory) throws Exception {

		// FhirContext fhirContext = FhirContext.forDstu3();
		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
			for (Path path : directoryStream) {
				ConceptMap conceptMapFromTo = new ConceptMap();
				ConceptMap conceptMapToFrom = new ConceptMap();
				ConceptMapGroupComponent cmgcFromTo = conceptMapFromTo.addGroup();
				ConceptMapGroupComponent cmgcToFrom = conceptMapToFrom.addGroup();
				int count = 0;
				boolean foundValidCode=false;
				UriType sourceuri = new UriType();
				UriType targeturi = new UriType();
			
				for (String line : Files.readAllLines(path)) {
					
					if(count < 2) {
						if(count == 0) {
							String[] code2code = line.toString().split("\t");
							if(!StringUtils.isNullOrEmpty(code2code[1]) && !StringUtils.isNullOrEmpty(code2code[7])) {
								sourceuri.setValue(code2code[1]);
								targeturi.setValue(code2code[7]);
							}
						}
						count++;
						continue;
					}
					String[] code2code = line.toString().split("\t");
					if (code2code.length == 10 && !StringUtils.isNullOrEmpty(code2code[0]) && !StringUtils.isNullOrEmpty(code2code[2]) && !StringUtils.isNullOrEmpty(code2code[6]) && !StringUtils.isNullOrEmpty(code2code[8])  && !StringUtils.isNullOrEmpty(code2code[9])) {
						if (!foundValidCode) {
							
							foundValidCode = true;

							conceptMapFromTo.setSource(sourceuri);
							conceptMapToFrom.setTarget(sourceuri);

							conceptMapFromTo.setTarget(targeturi);
							conceptMapToFrom.setSource(targeturi);
						
							cmgcFromTo.setSource(code2code[2]);
							cmgcFromTo.setTarget(code2code[9]);

							cmgcToFrom.setTarget(code2code[2]);
							cmgcToFrom.setSource(code2code[9]);
							
							
						}
						
						
						SourceElementComponent secFromTo = cmgcFromTo.addElement();
						CodeType aaa = new CodeType();
						
						
						secFromTo.setCodeElement(aaa);
						secFromTo.setCode(code2code[0]).addTarget().setCode(code2code[6]).setEquivalence(
							ConceptMapEquivalence.EQUAL).setDisplay(code2code[8]);
						
//						System.out.println(" code2code[1] " + code2code[1]);
						
//						secFromTo.setDisplay("foo");

						SourceElementComponent secToFrom = cmgcToFrom.addElement();
						CodeType aaa2 = new CodeType();
						secToFrom.setCodeElement(aaa2);
						secToFrom.setCode(code2code[6]).addTarget().setCode(code2code[0]).setEquivalence(
							ConceptMapEquivalence.EQUAL).setDisplay(code2code[1]);
//						System.out.println(" code2code[7] " + code2code[8]);
						
//						secToFrom.setDisplay("bar");

					}

					}

				if(foundValidCode) {
					
					System.out.println("Adding file " +path.getFileName());
					
					
//				System.out.println(
//					"Concept Maps ::" +
//							ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(conceptMapFromTo));
//
//				
//				System.out.println(
//						"Concept Maps ::" +
//								ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(conceptMapToFrom));

				

				client.setEncoding(EncodingEnum.JSON);
				client.create().resource(conceptMapFromTo).prefer(
					PreferReturnEnum.REPRESENTATION).execute();
				//System.out.println(results.getId());
				client.create().resource(conceptMapToFrom).prefer(PreferReturnEnum.REPRESENTATION).execute();
				} else {
					System.err.println("NOT Adding file " +path.getFileName());
				}
			}
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		return fileNames;
	}
	
	
	


	@Test
	public void loadFromMappings() throws Exception {
		//TerminologyUtil.load(client, "src/test/resources/mappings/loinc");
		loadMapsxx("src/test/resources/mappings/loinc/dod");
	}

}