package org.openmrs.module.ugandaemrreports.reports;

import org.openmrs.Concept;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.DateConverter;
import org.openmrs.module.reporting.data.person.definition.BirthdateDataDefinition;
import org.openmrs.module.reporting.data.person.definition.GenderDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonAttributeDataDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.ugandaemrreports.data.converter.ObsDataConverter;
import org.openmrs.module.ugandaemrreports.data.converter.PersonAttributeDataConverter;
import org.openmrs.module.ugandaemrreports.library.DataFactory;
import org.openmrs.module.ugandaemrreports.library.HIVPatientDataLibrary;
import org.openmrs.module.ugandaemrreports.reporting.dataset.definition.SharedDataDefintion;
import org.openmrs.module.ugandaemrreports.reporting.metadata.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TPT PROTECT Data Export Report
 */

@Component
public class SetUpTPTDataExportReport extends UgandaEMRDataExportManager {


    @Autowired
    SharedDataDefintion sdd;
    @Autowired
    private DataFactory df;
    @Autowired
    private HIVPatientDataLibrary hivPatientData;

    @Override
    public String getDescription() {
        return "Data Export For TPT Protect Data";
    }

    @Override
    public String getName() {
        return "TPT PROTECT DATA";
    }

    @Override
    public String getUuid() {
        return "69f2bbb4-d5ce-4a5d-b51c-37ad7398a0d1";
    }

    @Override
    public String getVersion() {
        return "1.0.1";
    }

    /**
     * @return the uuid for the report design for exporting to Excel
     */
    @Override
    public String getExcelDesignUuid() {
        return "9512d3c8-96d2-43e1-935a-e3d4f187bb4f";
    }

    @Override
    public List<Parameter> getParameters() {
        List<Parameter> l = new ArrayList<Parameter>();
        l.add(df.getStartDateParameter());
        l.add(df.getEndDateParameter());
        return l;
    }

    @Override
    public List<ReportDesign> constructReportDesigns(ReportDefinition reportDefinition) {
        List<ReportDesign> l = new ArrayList<ReportDesign>();
        l.add(buildReportDesign(reportDefinition));
        return l;
    }

    /**
     * Build the report design for the specified report, this allows a user to override the report
     * design by adding properties and other metadata to the report design
     *
     * @param reportDefinition
     * @return The report design
     */
    @Override
    public ReportDesign buildReportDesign(ReportDefinition reportDefinition) {
        ReportDesign rd = createCSVDesign(getExcelDesignUuid(), reportDefinition);
        return rd;
    }


    @Override
    public ReportDefinition constructReportDefinition() {

        ReportDefinition rd = new ReportDefinition();
        rd.setUuid(getUuid());
        rd.setName(getName());
        rd.setDescription(getDescription());
        rd.setParameters(getParameters());
        rd.addDataSetDefinition("TPT", Mapped.mapStraightThrough(dataSetDefinition()));

        return rd;
    }

    private Concept getConcept(String uuid) {
        return Dictionary.getConcept(uuid);
    }

    private CohortDefinition getPatientWithARTSummaryEncounterDuringPeriod() {
        return df.getAnyEncounterOfTypesBetweenDates(
                Arrays.asList(Dictionary.getEncounterType("8d5b27bc-c2cc-11de-8d13-0010c6dffd0f")));
    }

    private DataSetDefinition dataSetDefinition() {
        PatientDataSetDefinition dsd = new PatientDataSetDefinition();
        dsd.setName("TPT");
        dsd.addParameters(getParameters());
        dsd.addRowFilter(Mapped.mapStraightThrough(getPatientWithARTSummaryEncounterDuringPeriod()));

        //start constructing of the dataset
        PersonAttributeType maritalStatus = Context.getPersonService().getPersonAttributeTypeByUuid("dce0c134-30ab-102d-86b0-7a5022ba4115");

        dsd.addColumn("HIV Clinic No", sdd.definition("serialNo", getConcept("1646AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Age", df.getHTSVisitDate(), (String) null, new DateConverter("yyyy-MM-dd"));
        dsd.addColumn("Brithdate", df.getPreferredAddress("address4"), (String) null);
        dsd.addColumn("Gender", df.getPreferredAddress("address3"), (String) null);
        dsd.addColumn("District", df.getPreferredAddress("countyDistrict"), (String) null);
        dsd.addColumn("County", new GenderDataDefinition(), (String) null);
        dsd.addColumn("Sub-county", new BirthdateDataDefinition(), (String) null, new BirthdateConverter("yyyy-MM-dd"));
        dsd.addColumn("Parish", new PersonAttributeDataDefinition("MaritalStatus", maritalStatus), "", new PersonAttributeDataConverter());
        dsd.addColumn("First Encounter Date", sdd.definition("accompaniedBy", getConcept("dc911cc1-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Last Encounter Date", sdd.definition("accompaniedByOther", getConcept("6cb349b1-9f45-4c96-84c7-9d7037c6a056")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Return Visit Date", sdd.definition("htsModel", getConcept("46648b1d-b099-433b-8f9c-3815ff1e0a0f")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Return Visit Date (1)", sdd.definition("htsModel", getConcept("46648b1d-b099-433b-8f9c-3815ff1e0a0f")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("BaseLine Regimen", sdd.definition("htsApproach", getConcept("ff820a28-1adf-4530-bf27-537bfa9ce0b2")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("ART Start Date", sdd.definition("entrypoint", getConcept("720a1e85-ea1c-4f7b-a31e-cb896978df79")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Current ARV Regimen", sdd.definition("entrypointSpecify", getConcept("adf31c43-c9a0-4ab8-b53a-42097eb3d2b6")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Current Regimen Other", sdd.definition("testingPoints", getConcept("4f4e6d1d-4343-42cc-ba47-2319b8a84369")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Transfer IN", sdd.definition("testingPointsOther", getConcept("16820069-b4bf-4c47-9efc-408746e1636b")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Transferred Out To Another Facility", sdd.definition("reason", getConcept("2afe1128-c3f6-4b35-b119-d17b9b9958ed")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Name Of Location Transferred To", sdd.definition("reasonOther", getConcept("8c628b5b-0045-40dc-a480-7e1518ffb256")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Entry Point Into HIV Care", sdd.definition("special", getConcept("927563c5-cb91-4536-b23c-563a72d3f829")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Pregnant", sdd.definition("special", getConcept("927563c5-cb91-4536-b23c-563a72d3f829")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());//TODO get concept_id
        dsd.addColumn("PMTCT", sdd.definition("firstTime", getConcept("2766c090-c057-44f2-98f0-691b6d0336dc")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Deceased", sdd.definition("lastHIVTestDate", getConcept("34c917f0-356b-40d0-b3d1-cf609517b5fc")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("DeathDate", sdd.definition("lastTestResults", getConcept("49ba801d-b6ff-47cd-8d29-e0ac8649cb7d")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Mid-Upper Arm Circumference Code", sdd.definition("timesTested", getConcept("8037192e-8f0c-4af3-ad8d-ccd1dd6880ba")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Body Weight", sdd.definition("noOfSexualPartners", getConcept("f1a6ede9-052e-4707-9cd8-a77fdeb2a02b")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Height", sdd.definition("Partner Tested Before", getConcept("adc0b1a1-39cf-412b-9ab0-28ec0f731220")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("WHO HIV Clinical Stage", sdd.definition("PartnerResults", getConcept("ee802cf2-295b-4297-b53c-205f794294a5")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Anti-Retroviral Drug Adherence Assessment Code", sdd.definition("pre-testConseling", getConcept("193039f1-c378-4d81-bb72-653b66c69914")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Adherence To Cotrimoxzole", sdd.definition("counseled as", getConcept("b92b1777-4356-49b2-9c83-a799680dc7d4")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Symptom,Diagnosis,Or Opportunistic Infection", sdd.definition("HIVresults", getConcept("3d292447-d7df-417f-8a71-e53e869ec89d")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Tuberculosis status", sdd.definition("syphillis duo results", getConcept("16091701-69b8-4bc7-82b3-b1726cf5a5df")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Method of Family Planning", sdd.definition("consent", getConcept("0698a45b-771c-4d11-84ff-095598c8883c")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("BaseLine CD4", sdd.definition("recency Results", getConcept("141520BBBBBBBBBBBBBBBBBBBBBBBBBBBBBB")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("INH Days Dispensed", sdd.definition("individualResultsRecived", getConcept("3437ae80-bcc5-41e2-887e-d56999a1b467")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Cause Of Death", sdd.definition("coupleResultsReceived", getConcept("2aa9f0c1-3f7e-49cd-86ee-baac0d2d5f2d")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("HIV Viral Load Date", sdd.definition("coupleResults", getConcept("94a5bd0a-b79d-421e-ab71-8e382eed100f")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("HIV Viral Load Date (1)", sdd.definition("TBCase", getConcept("b80f04a4-1559-42fd-8923-f8a6d2456a04")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Date Positive HIV Test Confirmed", sdd.definition("referedForTBServices", getConcept("c5da115d-f6a3-4d13-b182-c2e982a3a796")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Viral Load", sdd.definition("refferedTonrollment", getConcept("3d620422-0641-412e-ab31-5e45b98bc459")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("Crag Test Results", sdd.definition("referralPlace", getConcept("dce015bb-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
        dsd.addColumn("TB LAM Results", sdd.getDHIS2Uuid(), (String) null);
        dsd.addColumn( "Cotrimoxazole Pill Dosage", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn("Transfer Out Date", sdd.getDHIS2Uuid(), (String) null);
        dsd.addColumn( "CD4 Count", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn("TPT Status", sdd.getDHIS2Uuid(), (String) null);
        dsd.addColumn( "TPT Start Date", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn("TPT Completion Date", sdd.getDHIS2Uuid(), (String) null);
        dsd.addColumn( "Facility Based Individual Management Enrollment Date", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn( "Facility Based Groups Enrollment Date", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn( "Fast Track Drug Refill Enrollment Date", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn( "Community Drug Distribution Point Enrollment Date", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn( "Advanced Disease Status", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn( "Date Cotrim Started", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn( "Body Mass Index", df.getHTSEncounterUuid(), (String) null);
        dsd.addColumn("dhis2_uuid", sdd.getDHIS2Uuid(), (String) null);
        return dsd;
    }
}
