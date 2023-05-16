package com.mdix.fhir.terminology.server.provider;

import java.util.Collection;
import java.util.HashMap;

import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r5.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r5.model.ConceptMap.TargetElementComponent;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@Component
public class ConceptMapProvider extends AbstractJaxRsResourceProvider<ConceptMap> {

	FhirContext fhirContext;

	public ConceptMapProvider(FhirContext fhirContext) {
		super(fhirContext);
	}

	@Override
	public Class<ConceptMap> getResourceType() {
		return ConceptMap.class;
	}

	private static IdType createId(final Long id, final Long theVersionId) {
		return new IdType("ConceptMap", id);
	}

	static private long myNextId = 1;

	public static ConceptMap addConceptMap(ConceptMap theConceptMap) {
		theConceptMap.setId(createId(++myNextId, 1l));
		conceptMaps.put(createKey(theConceptMap.getSourceScopeUriType().getValue(),
				theConceptMap.getTargetScopeUriType().getValue()), theConceptMap);
		return theConceptMap;
	}

	@Create
	public MethodOutcome createConceptMap(@ResourceParam ConceptMap theConceptMap) throws FHIRException {
		addConceptMap(theConceptMap);
		MethodOutcome retval = new MethodOutcome();
		retval.setId(theConceptMap.getIdElement());
		retval.setResource(theConceptMap);
		return retval;
	}

	private static String createKey(String source, String target) {
		return source + "TO" + target;
	}

	private static boolean containsCode(String key, String code) {
		boolean flag = false;
		ValueSet valueset = ValueSetProvider.getValueSet(key);

		if (valueset == null) {
			return flag;
		} else {
			for (ConceptSetComponent include : valueset.getCompose().getInclude()) {
				for (ConceptReferenceComponent comp : include.getConcept()) {
					if (comp.getCode().toString().equals(code)) {
						flag = true;
						return flag;
					}

				}
			}
		}
		return flag;
	}

	static HashMap<String, ConceptMap> conceptMaps = new HashMap<>();

	@Read
	public ConceptMap getResourceById(@IdParam IIdType theId) {

		for (String key : conceptMaps.keySet()) {
			System.out.println(conceptMaps.get(key).getId());
			System.out.println(theId.getIdPart());
			if (theId.getIdPart().equals(conceptMaps.get(key).getId())) {
				return conceptMaps.get(key);
			}

		}
		throw new ResourceNotFoundException(theId);
	}

	@Search
	public Collection<ConceptMap> getAllConceptMaps() {
		return conceptMaps.values();
	}

	@Operation(name = "$translate", idempotent = true, returnParameters = {
			@OperationParam(name = "return", type = StringType.class) })
	public Parameters transform(@OperationParam(name = "code") CodeType code,
			@OperationParam(name = "system") StringParam system,
			@OperationParam(name = ConceptMap.SP_VERSION) StringParam version,
			@OperationParam(name = "source") StringParam source, @OperationParam(name = "coding") Coding coding,
			@OperationParam(name = "codeableConcept") CodeableConcept codeableConcept,
			@OperationParam(name = "target") StringParam target,
			@OperationParam(name = "targetsystem") StringParam targetSystem) {
		Parameters parameters = new Parameters();

		BooleanType value = new BooleanType();
		value.setValue(false);
		parameters.addParameter().setName("result").setValue(value);

		String conceptMapKey = createKey(source.getValue(), target.getValue());

		if (conceptMaps.containsKey(conceptMapKey)) {

			ConceptMap conceptMap = conceptMaps.get(createKey(source.getValue(), target.getValue()));

			ConceptMapGroupComponent conceptMapGroupComponent = conceptMap.getGroupFirstRep();
			for (SourceElementComponent sourceElementComponent : conceptMapGroupComponent.getElement()) {
				// System.out.println("testing:"+sourceElementComponent.getCode());
				if (sourceElementComponent.hasTarget() && sourceElementComponent.hasCode()) {
					if (sourceElementComponent.getCode().equals(code.getValue())) {
						value.setValue(true);
						TargetElementComponent tec = sourceElementComponent.getTargetFirstRep();

						parameters.getParameterFirstRep().setValue(value);
						// parameters.addParameter().setName("result").setValue(value);
						ParametersParameterComponent ppc = new ParametersParameterComponent();
						ppc.setName("match");
						CodeType codeValue = new CodeType();

						codeValue.setValue("equivalent");
						ppc.addPart().setName("equivalence").setValue(codeValue);
						Coding targetCoding = new Coding();
						targetCoding.setCode(tec.getCode());
						targetCoding.setDisplay(tec.getDisplay());

						// UriType targetURI = (UriType) conceptMap.getTarget();
						targetCoding.setSystem(conceptMapGroupComponent.getTarget());
						targetCoding.setUserSelected(false);

						ppc.addPart().setName("concept").setValue(targetCoding);
						parameters.addParameter(ppc);
					}
				} else if (sourceElementComponent.hasTarget() && sourceElementComponent.hasValueSet()) {
					if (containsCode(sourceElementComponent.getValueSet(), code.getValue())) {
						value.setValue(true);
						TargetElementComponent tec = sourceElementComponent.getTargetFirstRep();

						parameters.getParameterFirstRep().setValue(value);
						// parameters.addParameter().setName("result").setValue(value);
						ParametersParameterComponent ppc = new ParametersParameterComponent();
						ppc.setName("match");
						CodeType codeValue = new CodeType();

						codeValue.setValue("equivalent");
						ppc.addPart().setName("equivalence").setValue(codeValue);
						Coding targetCoding = new Coding();
						targetCoding.setCode(tec.getCode());
						targetCoding.setDisplay(tec.getDisplay());

						// UriType targetURI = (UriType) conceptMap.getTarget();
						targetCoding.setSystem(conceptMapGroupComponent.getTarget());
						targetCoding.setUserSelected(false);

						ppc.addPart().setName("concept").setValue(targetCoding);
						parameters.addParameter(ppc);
					}
				}

			}

		}
		return parameters;
	}

}
