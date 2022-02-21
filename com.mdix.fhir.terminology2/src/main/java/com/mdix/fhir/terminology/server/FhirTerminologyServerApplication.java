package com.mdix.fhir.terminology.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.dstu3.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.dstu3.model.Enumerations.ConceptMapEquivalence;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.mdix.fhir.terminology.server.provider.ConceptMapProvider;

@SpringBootApplication
public class FhirTerminologyServerApplication {

	static Logger logger = LoggerFactory.getLogger(FhirTerminologyServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(FhirTerminologyServerApplication.class, args);
	}

	@PostConstruct
	public static void initialize() {
		try {
			loadConceptMaps("/v2tofhir");
			loadConceptMaps("/localmaps");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static void loadConceptMaps(String pathToLoad) throws IOException {

		Consumer<? super Path> loadConceptMap = new Consumer<Path>() {

			@Override
			public void accept(Path path) {

				ConceptMap conceptMapFromTo = new ConceptMap();
				ConceptMap conceptMapToFrom = new ConceptMap();
				ConceptMapGroupComponent cmgcFromTo = conceptMapFromTo.addGroup();
				ConceptMapGroupComponent cmgcToFrom = conceptMapToFrom.addGroup();
				int count = 0;
				boolean isValid = false;

				try {
					for (String line : Files.readAllLines(path)) {

						String[] code2code = line.toString().split("\t");
						if (count < 2) {
							count++;
							continue;
						}
						if (code2code.length == 10) {

							UriType sourceuri = new UriType();
							sourceuri.setValue(code2code[2]);

							conceptMapFromTo.setSource(sourceuri);
							conceptMapToFrom.setTarget(sourceuri);

							UriType targeturi = new UriType();
							targeturi.setValue(code2code[9]);

							conceptMapFromTo.setTarget(targeturi);
							conceptMapToFrom.setSource(targeturi);

							cmgcFromTo.setSource(code2code[2]);
							cmgcFromTo.setTarget(code2code[9]);

							cmgcToFrom.setTarget(code2code[2]);
							cmgcToFrom.setSource(code2code[9]);

							if ((code2code[0] != null && !code2code[0].contentEquals("")) &&
									(code2code[6] != null && !code2code[6].contentEquals(""))) {
								SourceElementComponent secFromTo = cmgcFromTo.addElement();
								CodeType aaa = new CodeType();
								secFromTo.setCodeElement(aaa);
								secFromTo.setCode(code2code[0]).addTarget().setCode(code2code[6]).setEquivalence(
									ConceptMapEquivalence.EQUAL);

								SourceElementComponent secToFrom = cmgcToFrom.addElement();
								CodeType aaa2 = new CodeType();
								secToFrom.setCodeElement(aaa2);
								secToFrom.setCode(code2code[6]).addTarget().setCode(code2code[0]).setEquivalence(
									ConceptMapEquivalence.EQUAL);
								isValid = true;
							}
						} else {
							logger.error(
								"Incomplete mapping " + path.getFileName() + " At line " + String.valueOf(count) +
										" :: " + line);
						}
					}
					if (isValid) {
						ConceptMapProvider.addConceptMap(conceptMapToFrom);
						ConceptMapProvider.addConceptMap(conceptMapFromTo);
						logger.info("Loaded Mapping File " + path.getFileName());
					} else {
						logger.info("Did not load Mapping File " + path.getFileName());

					}

				} catch (FHIRFormatError e) {
					logger.error("Error loading " + path.getFileName(), e);
				} catch (IOException e) {
					logger.error("Error loading " + path.getFileName(), e);
				}
			}
		};

		try (Stream<Path> paths = Files.walk(Paths.get(pathToLoad))) {

			paths.filter(Files::isRegularFile).forEach(loadConceptMap);
		}
	}

}
