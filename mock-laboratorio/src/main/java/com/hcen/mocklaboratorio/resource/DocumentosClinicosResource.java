package com.hcen.mocklaboratorio.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Exposes documentos clínicos estáticos para validar integraciones.
 * The payload mirrors docs/documento.xml but drops the signature wrapper.
 */
@Path("/documentos-clinicos")
public class DocumentosClinicosResource {

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response obtenerDocumentos() {
        String payload = """
<?xml version="1.0" encoding="UTF-8"?>
<Documentos xmlns="urn:salud.uy/2014/signed-clinical-document">
    <Documento>
        <ClinicalDocument xmlns="urn:hl7-org:v3" xmlns:voc="urn:hl7-org:v3/voc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:hl7-org:v3 CDA.xsd">
            <typeId root="2.16.840.1.113883.1.3" extension="POCD_HD000040"/>
            <templateId root="2.16.858.2.10000675.72591.1.101.1"/>
            <id root="2.16.858.2.1.67430.20240503090000.1.1"/>
            <code xsi:type="CV" code="11502-2" codeSystem="2.16.840.1.113883.6.1" codeSystemName="LOINC" displayName="Informe de laboratorio"/>
            <title xsi:type="ST">Perfil hematológico</title>
            <effectiveTime value="20240503090000"/>
            <confidentialityCode code="N" codeSystem="2.16.840.1.113883.5.25"/>
            <languageCode code="es-UY"/>
            <recordTarget>
                <patientRole>
                    <id root="2.16.858.2.10000675.68909" extension="0001-000001"/>
                    <patient>
                        <name>
                            <family>Pérez</family>
                            <given>Lucía</given>
                        </name>
                        <administrativeGenderCode code="2" displayName="Femenino" codeSystem="2.16.858.2.10000675.69600"/>
                        <birthTime value="19880412"/>
                    </patient>
                </patientRole>
            </recordTarget>
            <author>
                <time value="20240503090000"/>
                <assignedAuthor>
                    <id root="2.16.858.2.10000675.68909" extension="LAB-001"/>
                    <assignedPerson>
                        <name>
                            <family>Ramos</family>
                            <given>Carolina</given>
                        </name>
                    </assignedPerson>
                    <representedOrganization>
                        <id root="1.2.3.4.5"/>
                        <name>Laboratorio Central HCEN</name>
                    </representedOrganization>
                </assignedAuthor>
            </author>
            <componentOf>
                <encompassingEncounter>
                    <effectiveTime>
                        <low value="20240503083000"/>
                        <high value="20240503090000"/>
                    </effectiveTime>
                </encompassingEncounter>
            </componentOf>
            <component>
                <structuredBody>
                    <component>
                        <section>
                            <code xsi:type="CV" code="18719-5" codeSystem="2.16.840.1.113883.6.1" displayName="Resultados de pruebas de laboratorio"/>
                            <title>Resultados</title>
                            <text>
                                <list>
                                    <item>Hemoglobina: 13.5 g/dL</item>
                                    <item>Hematocrito: 40%</item>
                                    <item>Leucocitos: 6.1 x10^3/uL</item>
                                </list>
                            </text>
                        </section>
                    </component>
                </structuredBody>
            </component>
        </ClinicalDocument>
    </Documento>
    <Documento>
        <ClinicalDocument xmlns="urn:hl7-org:v3" xmlns:voc="urn:hl7-org:v3/voc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:hl7-org:v3 CDA.xsd">
            <typeId root="2.16.840.1.113883.1.3" extension="POCD_HD000040"/>
            <templateId root="2.16.858.2.10000675.72591.1.101.1"/>
            <id root="2.16.858.2.1.67430.20240515091500.1.1"/>
            <code xsi:type="CV" code="11502-2" codeSystem="2.16.840.1.113883.6.1" codeSystemName="LOINC" displayName="Informe de laboratorio"/>
            <title xsi:type="ST">Perfil lipídico</title>
            <effectiveTime value="20240515091500"/>
            <confidentialityCode code="N" codeSystem="2.16.840.1.113883.5.25"/>
            <languageCode code="es-UY"/>
            <recordTarget>
                <patientRole>
                    <id root="2.16.858.2.10000675.68909" extension="0001-000002"/>
                    <patient>
                        <name>
                            <family>Fernández</family>
                            <given>Martín</given>
                        </name>
                        <administrativeGenderCode code="1" displayName="Masculino" codeSystem="2.16.858.2.10000675.69600"/>
                        <birthTime value="19750521"/>
                    </patient>
                </patientRole>
            </recordTarget>
            <author>
                <time value="20240515090000"/>
                <assignedAuthor>
                    <id root="2.16.858.2.10000675.68909" extension="LAB-002"/>
                    <assignedPerson>
                        <name>
                            <family>Díaz</family>
                            <given>Andrea</given>
                        </name>
                    </assignedPerson>
                    <representedOrganization>
                        <id root="1.2.3.4.5"/>
                        <name>Laboratorio Central HCEN</name>
                    </representedOrganization>
                </assignedAuthor>
            </author>
            <componentOf>
                <encompassingEncounter>
                    <effectiveTime>
                        <low value="20240515083000"/>
                        <high value="20240515091500"/>
                    </effectiveTime>
                </encompassingEncounter>
            </componentOf>
            <component>
                <structuredBody>
                    <component>
                        <section>
                            <code xsi:type="CV" code="18719-5" codeSystem="2.16.840.1.113883.6.1" displayName="Resultados de pruebas de laboratorio"/>
                            <title>Resultados</title>
                            <text>
                                <list>
                                    <item>Colesterol total: 210 mg/dL</item>
                                    <item>HDL: 45 mg/dL</item>
                                    <item>LDL: 145 mg/dL</item>
                                    <item>Triglicéridos: 160 mg/dL</item>
                                </list>
                            </text>
                        </section>
                    </component>
                </structuredBody>
            </component>
        </ClinicalDocument>
    </Documento>
    <Documento>
        <ClinicalDocument xmlns="urn:hl7-org:v3" xmlns:voc="urn:hl7-org:v3/voc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:hl7-org:v3 CDA.xsd">
            <typeId root="2.16.840.1.113883.1.3" extension="POCD_HD000040"/>
            <templateId root="2.16.858.2.10000675.72591.1.101.1"/>
            <id root="2.16.858.2.1.67430.20240601084500.1.1"/>
            <code xsi:type="CV" code="11502-2" codeSystem="2.16.840.1.113883.6.1" codeSystemName="LOINC" displayName="Informe de laboratorio"/>
            <title xsi:type="ST">Glucemia en ayunas</title>
            <effectiveTime value="20240601084500"/>
            <confidentialityCode code="N" codeSystem="2.16.840.1.113883.5.25"/>
            <languageCode code="es-UY"/>
            <recordTarget>
                <patientRole>
                    <id root="2.16.858.2.10000675.68909" extension="0001-000003"/>
                    <patient>
                        <name>
                            <family>García</family>
                            <given>Sofía</given>
                        </name>
                        <administrativeGenderCode code="2" displayName="Femenino" codeSystem="2.16.858.2.10000675.69600"/>
                        <birthTime value="19930217"/>
                    </patient>
                </patientRole>
            </recordTarget>
            <author>
                <time value="20240601084500"/>
                <assignedAuthor>
                    <id root="2.16.858.2.10000675.68909" extension="LAB-003"/>
                    <assignedPerson>
                        <name>
                            <family>Sosa</family>
                            <given>Ignacio</given>
                        </name>
                    </assignedPerson>
                    <representedOrganization>
                        <id root="9.8.7.6.5"/>
                        <name>Laboratorios del Sur</name>
                    </representedOrganization>
                </assignedAuthor>
            </author>
            <componentOf>
                <encompassingEncounter>
                    <effectiveTime>
                        <low value="20240601081500"/>
                        <high value="20240601084500"/>
                    </effectiveTime>
                </encompassingEncounter>
            </componentOf>
            <component>
                <structuredBody>
                    <component>
                        <section>
                            <code xsi:type="CV" code="18719-5" codeSystem="2.16.840.1.113883.6.1" displayName="Resultados de pruebas de laboratorio"/>
                            <title>Resultados</title>
                            <text>
                                <list>
                                    <item>Glucosa: 90 mg/dL</item>
                                    <item>Hemoglobina A1c: 5.2%</item>
                                    <item>Observaciones: Valores dentro de rango</item>
                                </list>
                            </text>
                        </section>
                    </component>
                </structuredBody>
            </component>
        </ClinicalDocument>
    </Documento>
</Documentos>
""";
        return Response.ok(payload, MediaType.APPLICATION_XML).build();
    }
}
