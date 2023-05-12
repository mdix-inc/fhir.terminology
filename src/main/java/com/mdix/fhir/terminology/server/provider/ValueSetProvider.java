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
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.model.primitive.StringDt;
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
public class ValueSetProvider extends AbstractJaxRsResourceProvider<ValueSet> {

	FhirContext fhirContext;

	public ValueSetProvider(FhirContext fhirContext) {
		super(fhirContext);
	}

	@Override
	public Class<ValueSet> getResourceType() {
		return ValueSet.class;
	}

	private static IdType createId(final Long id, final Long theVersionId) {
		return new IdType("ValueSet", id);
	}

	static private long myNextId = 1;
	
	public static ValueSet addValueSet(ValueSet theValueSet) {
		theValueSet.setId(createId(++myNextId, 1l));
		valueSets.put(theValueSet.getUrl(),theValueSet);
		return theValueSet;
	}
	
	@Create
	public MethodOutcome createValueSet(@ResourceParam ValueSet theValueSet) throws FHIRException {
		addValueSet(theValueSet);
		MethodOutcome retval = new MethodOutcome();
		retval.setId(theValueSet.getIdElement());
		retval.setResource(theValueSet);
		return retval;
	}
	
	static HashMap<String, ValueSet> valueSets = new HashMap<>();
	
	@Read
	public ValueSet getResourceById(@IdParam IIdType theId) {

		for (String key : valueSets.keySet()) {
			System.out.println(valueSets.get(key).getId());
			System.out.println(theId.getIdPart());
			if (theId.getIdPart().equals(valueSets.get(key).getId())) {
				return valueSets.get(key);
			}

		}
		throw new ResourceNotFoundException(theId);
	}

	@Search
	public Collection<ValueSet> getAllValueSets() {
		return valueSets.values();
	}

}
