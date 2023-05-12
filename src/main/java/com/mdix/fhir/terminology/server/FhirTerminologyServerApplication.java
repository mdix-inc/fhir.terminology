package com.mdix.fhir.terminology.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r5.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r5.model.Enumerations.ConceptMapRelationship;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r5.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.mdix.fhir.terminology.server.provider.ConceptMapProvider;

@SpringBootApplication
public class FhirTerminologyServerApplication {

	@Value("#{systemProperties['fhir.conceptmaps'] ?: '/conceptmaps'}")
	private String conceptmapsFolders;

	static Logger logger = LoggerFactory.getLogger(FhirTerminologyServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(FhirTerminologyServerApplication.class, args);
	}

	@PostConstruct
	public void initialize() {

		Consumer<? super Path> loadConceptMap = new Consumer<Path>() {

			@Override
			public void accept(Path path) {

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
								if(code2code[1] != null && !code2code[1].contentEquals("")) {
									valueseturi.setValue(code2code[1]);
									valueset.setUrlElement(valueseturi);
									
								}
								count++;
								continue;
							}
							if(count < 2) {
								include.addConcept().setCode(code2code[0]).setDisplay(code2code[1]);
							}else
								count++;
						}
						compose.addInclude(include);
						valueset.setCompose(compose);
					}catch (FHIRFormatError e) {
						logger.error("Error loading " + path.getFileName(), e);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						logger.error("Error loading " + path.getFileName(), e);
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
								if(count == 0) {
									if((code2code[1] != null && !code2code[1].contentEquals(""))
											&& (code2code[7] != null && !code2code[7].contentEquals(""))) {
										sourceuri.setValue(code2code[1]);
										targeturi.setValue(code2code[7]);
									}
								}
								count++;
								continue;
							}
							if (code2code.length >= 10) {

								conceptMapFromTo.setSource(sourceuri);
								conceptMapToFrom.setTarget(sourceuri);

								conceptMapFromTo.setTarget(targeturi);
								conceptMapToFrom.setSource(targeturi);

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
								}
							} else {
								logger.error("Incomplete mapping " + path.getFileName() + " At line "
										+ String.valueOf(count) + " :: " + line);
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
			}

		};

		try {
			for (String mapsFolder : Stream.of(conceptmapsFolders.split(",", -1)).collect(Collectors.toList())) {
				try (Stream<Path> paths = Files.walk(Paths.get(mapsFolder))) {
					paths.filter(Files::isRegularFile).forEach(loadConceptMap);
				}
			}
		} catch (IOException e) {

			e.printStackTrace();
		}

		//
		// Set<Path> folders = Files.find(
		// Paths.get(mapsFolder), Integer.MAX_VALUE, (filePath, fileAttr) ->
		// fileAttr.isDirectory()).collect(
		// Collectors.toSet());
		//
		// for (Path folder : folders) {
		//
		// try (Stream<Path> paths = Files.walk(folder)) {
		//
		// paths.filter(Files::isRegularFile).forEach(loadConceptMap);
		// }
		//
		// // Set<String> maps = Stream.of(new
		// File(folder.toString()).listFiles()).filter(
		// // file -> (!file.isDirectory() &&
		// file.toString().endsWith("mdmi"))).map(File::getName).collect(
		// // Collectors.toSet());
		// // for (String map : maps) {
		// // logger.trace("Loading conceptmapsFolders " + map);
		// // InputStream targetStream = new FileInputStream(folder.toString() + "/" +
		// map);
		// //
		// //
		// //
		// // logger.trace("Loaded conceptmapsFolders " + map);
		// // }
		// }
		// }
		//
		// } catch (IOException e) {
		//
		// e.printStackTrace();
		// }
		// try {
		// for (String mapsFolder : Stream.of(conceptmapsFolders.split(",",
		// -1)).collect(Collectors.toList())) {
		//
		// Set<Path> folders = Files.find(
		// Paths.get(mapsFolder), Integer.MAX_VALUE, (filePath, fileAttr) ->
		// fileAttr.isDirectory()).collect(
		// Collectors.toSet());
		//
		// }
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// try {
		// loadConceptMaps("/v2tofhir");
		// loadConceptMaps("/localmaps");
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

	}

	// private void loadMaps() throws IOException {
	// synchronized (this) {
	//
	// if (loaded || lastModified == 0) {
	// long currentModified = 0;
	// for (String mapsFolder : Stream.of(mapsFolders.split(",",
	// -1)).collect(Collectors.toList())) {
	//
	// Set<Path> folders = Files.find(
	// Paths.get(mapsFolder), Integer.MAX_VALUE,
	// (filePath, fileAttr) -> fileAttr.isDirectory()).collect(Collectors.toSet());
	//
	// for (Path folder : folders) {
	//
	// Set<File> maps2 = Stream.of(new File(folder.toString()).listFiles()).filter(
	// file -> (!file.isDirectory() && file.toString().endsWith("mdmi"))).collect(
	// Collectors.toSet());
	//
	// for (File map : maps2) {
	// if (map.lastModified() > currentModified) {
	// currentModified = map.lastModified();
	// }
	//
	// }
	//
	// }
	// }
	//
	// if (currentModified > lastModified) {
	// loaded = false;
	// mapProperties.clear();
	// preprocessors.clear();
	// postprocessors.clear();
	// targetsemanticprocessors.clear();
	// sourcesemanticprocessors.clear();
	// lastModified = currentModified;
	//
	// }
	//
	// }
	//
	// if (loaded) {
	// return;
	// }
	//
	// FHIRTerminologyTransform.codeValues.clear();
	//
	// FHIRTerminologyTransform.processTerminology = true;
	//
	// FHIRTerminologyTransform.setFHIRTerminologyURL(terminologySettings.getUrl());
	//
	// FHIRTerminologyTransform.setUserName(terminologySettings.getUserName());
	//
	// FHIRTerminologyTransform.setPassword(terminologySettings.getPassword());
	//
	// for (String mapsFolder : Stream.of(mapsFolders.split(",",
	// -1)).collect(Collectors.toList())) {
	//
	// Set<Path> folders = Files.find(
	// Paths.get(mapsFolder), Integer.MAX_VALUE, (filePath, fileAttr) ->
	// fileAttr.isDirectory()).collect(
	// Collectors.toSet());
	//
	// for (Path folder : folders) {
	//
	// Set<String> maps = Stream.of(new File(folder.toString()).listFiles()).filter(
	// file -> (!file.isDirectory() &&
	// file.toString().endsWith("mdmi"))).map(File::getName).collect(
	// Collectors.toSet());
	// for (String map : maps) {
	// logger.trace("Loading map " + map);
	// InputStream targetStream = new FileInputStream(folder.toString() + "/" +
	// map);
	// Mdmi.INSTANCE().getResolver().resolve(targetStream);
	// logger.trace("Loaded map " + map);
	// }
	//
	// System.err.println(folder.toString() + "/terms");
	//
	// Set<String> datatypeterms = Stream.of(new File(folder.toString() +
	// "/terms").listFiles()).filter(
	// file -> (!file.isDirectory() && file.toString().endsWith("properties"))).map(
	// File::getName).collect(Collectors.toSet());
	// for (String datatypeterm : datatypeterms) {
	// logger.trace("Loading datatypeterm " + datatypeterm);
	// InputStream datatypetermStream = new FileInputStream(
	// folder.toString() + "/terms/" + datatypeterm);
	// Utils.mapOfTransforms.put(FilenameUtils.removeExtension(datatypeterm), new
	// Properties());
	// Utils.mapOfTransforms.get(FilenameUtils.removeExtension(datatypeterm)).load(datatypetermStream);
	// // Mdmi.INSTANCE().getResolver().resolve(targetStream);
	// logger.trace("Loaded map " + datatypeterm);
	// }
	//
	// logger.trace("Check for processors.yml ");
	// logger.trace("Looking for " + folder.toString() + "/" + "processors.yml");
	// logger.trace("EXISTS " + Files.exists(Paths.get(folder.toString() + "/" +
	// "processors.yml")));
	// if (Files.exists(Paths.get(folder.toString() + "/" + "processors.yml"))) {
	// logger.trace("Found processors.yml ");
	// Yaml processorYaml = new Yaml();
	// InputStream inputStream = new FileInputStream(folder.toString() + "/" +
	// "processors.yml");
	// Map<String, Object> obj = processorYaml.load(inputStream);
	//
	// if (obj.containsKey("preprocessors")) {
	// preprocessors.add((Map<String, Object>) obj.get("preprocessors"));
	// }
	// if (obj.containsKey("postprocessors")) {
	// postprocessors.add((Map<String, Object>) obj.get("postprocessors"));
	// }
	//
	// if (obj.containsKey("sourcesemanticprocessors")) {
	// sourcesemanticprocessors.add((Map<String, Object>)
	// obj.get("sourcesemanticprocessors"));
	// }
	//
	// if (obj.containsKey("targetsemanticprocessors")) {
	// targetsemanticprocessors.add((Map<String, Object>)
	// obj.get("targetsemanticprocessors"));
	// }
	//
	// }
	//
	// }
	//
	// }
	//
	// loaded = Boolean.TRUE;
	//
	// }
	//
	// }

	// static void loadConceptMaps(String pathToLoad) throws IOException {
	//
	// Consumer<? super Path> loadConceptMap = new Consumer<Path>() {
	//
	// @Override
	// public void accept(Path path) {
	//
	// ConceptMap conceptMapFromTo = new ConceptMap();
	// ConceptMap conceptMapToFrom = new ConceptMap();
	// ConceptMapGroupComponent cmgcFromTo = conceptMapFromTo.addGroup();
	// ConceptMapGroupComponent cmgcToFrom = conceptMapToFrom.addGroup();
	// int count = 0;
	// boolean isValid = false;
	//
	// try {
	// for (String line : Files.readAllLines(path)) {
	//
	// String[] code2code = line.toString().split("\t");
	// if (count < 2) {
	// count++;
	// continue;
	// }
	// if (code2code.length == 10) {
	//
	// UriType sourceuri = new UriType();
	// sourceuri.setValue(code2code[2]);
	//
	// conceptMapFromTo.setSource(sourceuri);
	// conceptMapToFrom.setTarget(sourceuri);
	//
	// UriType targeturi = new UriType();
	// targeturi.setValue(code2code[9]);
	//
	// conceptMapFromTo.setTarget(targeturi);
	// conceptMapToFrom.setSource(targeturi);
	//
	// cmgcFromTo.setSource(code2code[2]);
	// cmgcFromTo.setTarget(code2code[9]);
	//
	// cmgcToFrom.setTarget(code2code[2]);
	// cmgcToFrom.setSource(code2code[9]);
	//
	// if ((code2code[0] != null && !code2code[0].contentEquals("")) &&
	// (code2code[6] != null && !code2code[6].contentEquals(""))) {
	// SourceElementComponent secFromTo = cmgcFromTo.addElement();
	// CodeType aaa = new CodeType();
	// secFromTo.setCodeElement(aaa);
	// secFromTo.setCode(code2code[0]).addTarget().setCode(code2code[6]).setEquivalence(
	// ConceptMapEquivalence.EQUAL);
	//
	// SourceElementComponent secToFrom = cmgcToFrom.addElement();
	// CodeType aaa2 = new CodeType();
	// secToFrom.setCodeElement(aaa2);
	// secToFrom.setCode(code2code[6]).addTarget().setCode(code2code[0]).setEquivalence(
	// ConceptMapEquivalence.EQUAL);
	// isValid = true;
	// }
	// } else {
	// logger.error(
	// "Incomplete mapping " + path.getFileName() + " At line " +
	// String.valueOf(count) +
	// " :: " + line);
	// }
	// }
	// if (isValid) {
	// ConceptMapProvider.addConceptMap(conceptMapToFrom);
	// ConceptMapProvider.addConceptMap(conceptMapFromTo);
	// logger.info("Loaded Mapping File " + path.getFileName());
	// } else {
	// logger.info("Did not load Mapping File " + path.getFileName());
	//
	// }
	//
	// } catch (FHIRFormatError e) {
	// logger.error("Error loading " + path.getFileName(), e);
	// } catch (IOException e) {
	// logger.error("Error loading " + path.getFileName(), e);
	// }
	// }
	// };
	//
	// try (Stream<Path> paths = Files.walk(Paths.get(pathToLoad))) {
	//
	// paths.filter(Files::isRegularFile).forEach(loadConceptMap);
	// }
	// }

}
