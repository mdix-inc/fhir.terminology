package sample.fhir.server.jersey.provider;

import java.util.HashMap;
import java.util.Iterator;

/*-
 * #%L
 * hapi-fhir-spring-boot-sample-server-jersey
 * %%
 * Copyright (C) 2014 - 2017 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.concurrent.ConcurrentHashMap;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;


import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r5.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r5.model.ConceptMap.TargetElementComponent;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

@Component
public class ConceptMapProvider extends AbstractJaxRsResourceProvider<ConceptMap> {

    private static Long counter = 1L;
    DynamoDB dynamoDB;

	Table table;

	FhirContext fhirContext;

    private static final ConcurrentHashMap<String, Patient> patients = new ConcurrentHashMap<>();

    static {
        patients.put(String.valueOf(counter), createPatient("Van Houte"));
        patients.put(String.valueOf(counter), createPatient("Agnew"));
        for (int i = 0; i < 20; i++) {
            patients.put(String.valueOf(counter), createPatient("Random Patient " + counter));
        }
    }

    public ConceptMapProvider(FhirContext fhirContext) {
        super(fhirContext);
    }

     @Override
    public Class<ConceptMap> getResourceType() {
        return ConceptMap.class;
    }

    private static IdType createId(final Long id, final Long theVersionId) {
        return new IdType("ConceptMap",  id);
    }

    private static Patient createPatient(final String name) {
        final Patient patient = new Patient();
        patient.getName().add(new HumanName().setFamily(name));
        patient.setId(createId(counter, 1L));
        counter++;
        return patient;
    }

    /*private static IdType createId2(final Long id, final Long theVersionId) {
        return new IdType("Patient", "" + id, "" + theVersionId);
    }*/
    
    private long myNextId = 1;
    @Create
	public MethodOutcome createConceptMap(@ResourceParam ConceptMap theConceptMap) throws FHIRException {
		long id = myNextId++;
		//IdDt idDt = new IdDt(id);
		theConceptMap.setId(createId(id,1l));
		// Table myTable = getDynamicDB().getTable(tableName);
		// IParser parser = fhirContext.newJsonParser();
		// parser.encodeResourceToString(theConceptMap);
		// Item myItem = Item.fromJSON(parser.encodeResourceToString(theConceptMap));
		// myTable.putItem(myItem);
		// conceptMaps.clear();
		
		conceptMaps.put(
			createKey(theConceptMap.getSourceScopeUriType().getValue(), theConceptMap.getTargetScopeUriType().getValue()),
			theConceptMap);
		MethodOutcome retval = new MethodOutcome();
		retval.setId(theConceptMap.getIdElement());
		// Set resource for response
		retval.setResource(theConceptMap);
		
		return retval;
	}
    
    private String createKey(String source, String target) {
		return source + "TO" + target;
	}

	static HashMap<String, ConceptMap> conceptMaps = new HashMap<String, ConceptMap>();

	@Read
	public ConceptMap getResourceById(@IdParam IIdType theId) {

		for (String key : conceptMaps.keySet()) {
			System.out.println(conceptMaps.get(key).getId());
			System.out.println(theId.getIdPart());
			if (theId.getIdPart().equals(conceptMaps.get(key).getId())) {
				return conceptMaps.get(key);
			}

		}
		// Index index = table.getIndex("IdIndex");
		// QuerySpec spec = new QuerySpec().withKeyConditionExpression("id = :v_id").withValueMap(
		// new ValueMap().withString(":v_id", theId.getIdPart()));
		// ItemCollection<QueryOutcome> items = index.query(spec);
		// Iterator<Item> iterator = items.iterator();
		// Item item = null;
		// while (iterator.hasNext()) {
		// item = iterator.next();
		// IParser parser = fhirContext.newJsonParser();
		// ConceptMap map = parser.parseResource(ConceptMap.class, item.toJSON());
		// return map;
		// }
		throw new ResourceNotFoundException(theId);
	}

	@Operation(name = "$translate", idempotent = true, returnParameters = {
			@OperationParam(name = "return", type = StringDt.class) })
	public Parameters transform(@OperationParam(name = "code") CodeType code,
			@OperationParam(name = "system") StringParam system,
			@OperationParam(name = ConceptMap.SP_VERSION) StringParam version,
			@OperationParam(name = "source") StringParam source, @OperationParam(name = "coding") Coding coding,
			@OperationParam(name = "codeableConcept") CodeableConcept codeableConcept,
			@OperationParam(name = "target") StringParam target,
			@OperationParam(name = "targetsystem") StringParam targetSystem) {
		Parameters parameters = new Parameters();
		// try {
		BooleanType value = new BooleanType();
		value.setValue(false);
		parameters.addParameter().setName("result").setValue(value);

		String conceptMapKey = createKey(source.getValue(), target.getValue());
		if (!conceptMaps.containsKey(conceptMapKey)) {
			// loadConceptMap(source.getValue(), target.getValue());
		}

		if (conceptMaps.containsKey(conceptMapKey)) {

			ConceptMap conceptMap = conceptMaps.get(createKey(source.getValue(), target.getValue()));

			ConceptMapGroupComponent conceptMapGroupComponent = conceptMap.getGroupFirstRep();
			for (SourceElementComponent sourceElementComponent : conceptMapGroupComponent.getElement()) {
				//System.out.println("testing:"+sourceElementComponent.getCode());
				if (sourceElementComponent.hasTarget() && sourceElementComponent.getCode().equals(code.getValue())) {
					value.setValue(true);
					TargetElementComponent tec = sourceElementComponent.getTargetFirstRep();

					parameters.getParameterFirstRep().setValue(value);
					// parameters.addParameter().setName("result").setValue(value);
					ParametersParameterComponent ppc = new ParametersParameterComponent();
					ppc.setName("match");
					CodeType codeValue = new CodeType();
					;
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

		return parameters;
	}

	/**
	 * @param value
	 * @param value2
	 */
	private void loadConceptMap(String source, String target) {

		QuerySpec spec = new QuerySpec().withKeyConditionExpression(
			"sourceUri = :v_id and targetUri = :t_id").withValueMap(
				new ValueMap().withString(":v_id", source).withString(":t_id", target));
		ItemCollection<QueryOutcome> items = table.query(spec);
		Iterator<Item> iterator = items.iterator();
		Item item = null;
		while (iterator.hasNext()) {
			item = iterator.next();
			System.out.println(item.toJSONPretty());
			IParser parser = fhirContext.newJsonParser();
			ConceptMap map = parser.parseResource(ConceptMap.class, item.toJSON());
			conceptMaps.put(createKey(source, target), map);
		}
	}

}
