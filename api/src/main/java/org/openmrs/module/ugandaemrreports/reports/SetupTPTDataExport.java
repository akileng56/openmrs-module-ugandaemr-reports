package org.openmrs.module.ugandaemrreports.reports;

import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.BaseObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.patient.library.BuiltInPatientDataLibrary;
import org.openmrs.module.reporting.data.person.definition.BirthdateDataDefinition;
import org.openmrs.module.reporting.data.person.definition.GenderDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonAttributeDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PreferredNameDataDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.ugandaemrreports.data.converter.ObsDataConverter;
import org.openmrs.module.ugandaemrreports.data.converter.PersonAttributeDataConverter;
import org.openmrs.module.ugandaemrreports.definition.data.converter.BirthDateConverter;
import org.openmrs.module.ugandaemrreports.definition.dataset.definition.UgandaEMRMobileDatasetDefinition;
import org.openmrs.module.ugandaemrreports.library.*;
import org.openmrs.module.ugandaemrreports.metadata.HIVMetadata;
import org.openmrs.module.ugandaemrreports.reporting.dataset.definition.SharedDataDefintion;
import org.openmrs.module.ugandaemrreports.reporting.metadata.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * TPT Protect  report
 */
@Component
public class SetupTPTDataExport extends UgandaEMRDataExportManager {

	@Autowired
	private DataFactory df;

	@Autowired
	ARTClinicCohortDefinitionLibrary hivCohorts;

	@Autowired
	private BuiltInPatientDataLibrary builtInPatientData;

	@Autowired
	private HIVPatientDataLibrary hivPatientData;

	@Autowired
	private BasePatientDataLibrary basePatientData;

	@Autowired
	private HIVMetadata hivMetadata;

	@Autowired
	private HIVCohortDefinitionLibrary hivCohortDefinitionLibrary;

	@Autowired
	SharedDataDefintion sdd;

	private Concept getConcept(String uuid) {
		return Dictionary.getConcept(uuid);
	}

	/**
	 * @return the uuid for the report design for exporting to Excel
	 */
	@Override
	public String getExcelDesignUuid() {
		return "63662aa2-013d-41c1-b0bf-fcbc11c3a843";
	}

	@Override
	public String getUuid() {
		return "dcefe1e9-dfdc-424b-be5d-87aa83accc65";
	}

	@Override
	public String getName() {
		return "TPT Protect Report";
	}

	@Override
	public String getDescription() {
		return "Lists Patients that been on TPT";
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

	@Override

	public ReportDesign buildReportDesign(ReportDefinition reportDefinition) {
		ReportDesign rd = createExcelTemplateDesign(getExcelDesignUuid(), reportDefinition, "TPTProtectReport.xls");
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:8,dataset:TPT_LIST");
		props.put("sortWeight", "5000");
		rd.setProperties(props);
		return rd;
	}

	/**
	 * Build the report design for the specified report, this allows a user to override the report design by adding
	 * properties and other metadata to the report design
	 *
	 * @return The report design
	 */


	@Override
	public ReportDefinition constructReportDefinition() {

		ReportDefinition rd = new ReportDefinition();

		rd.setUuid(getUuid());
		rd.setName(getName());
		rd.setDescription(getDescription());
		rd.setParameters(getParameters());

		PatientDataSetDefinition dsd = new PatientDataSetDefinition();

		CohortDefinition enrolledInCareBetweenDates = hivCohortDefinitionLibrary.getEnrolledInCareBetweenDates();
		//CohortDefinition cohortDefinition = df.getPatientsInAny(completedTPTDuringPeriod,startedTPTDuringPeriod);

		PersonAttributeType maritalStatus = Context.getPersonService().getPersonAttributeTypeByUuid("dce0c134-30ab-102d-86b0-7a5022ba4115");



		dsd.setName(getName());
		dsd.setParameters(getParameters());
		dsd.addRowFilter(Mapped.mapStraightThrough(enrolledInCareBetweenDates));

		dsd.addColumn("ID", sdd.getDHIS2Uuid(), (String) null);

		dsd.addColumn("Health Unit", df.getPreferredAddress("address4"), (String) null);

		dsd.addColumn("District", df.getPreferredAddress("countyDistrict"), (String) null);

		dsd.addColumn("Client Category", sdd.definition("Client Category", getConcept("8143023b-e9d9-4c9f-91b8-7c82b5524412")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Sex", new GenderDataDefinition(), (String) null);

		dsd.addColumn("Birth Date", new BirthdateDataDefinition(), (String) null, new BirthdateConverter("yyyy-MM-dd"));

		dsd.addColumn("Marital Status", new PersonAttributeDataDefinition("MaritalStatus", maritalStatus), "", new PersonAttributeDataConverter());

		dsd.addColumn("Special Population", sdd.definition("Special Population", getConcept("927563c5-cb91-4536-b23c-563a72d3f829")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		addColumn(dsd, "Care Entry Point", hivPatientData.getEntryPoint());

		addColumn(dsd, "ART Start Date", hivPatientData.getArtStartDate());

		addColumn(dsd, "Clinical Stage", hivPatientData.getBaselineWHOStage());

		addColumn(dsd, "CD4", hivPatientData.getBaselineCD4());

		dsd.addColumn("Date of Encounter", sdd.definition("Date of Encounter", getConcept("bdd1b59b-328d-42fa-a5ce-5e81d1c4042a")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Nutrition Assessment", sdd.definition("Nutrition Assessment", getConcept("165050AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("TB Status", sdd.definition("TB Status", getConcept("dce02aa1-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		addColumn(dsd, "Presenting TB Signs and Symptoms", hivPatientData.getClinicNumber());

		dsd.addColumn("Advanced Disease Status", sdd.definition("Advanced Disease Status", getConcept("17def5f6-d6b4-444b-99ed-40eb05d2c4f8")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("ARV Drugs Adherence", sdd.definition("ARV Drugs Adherence", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("CD4 Result", sdd.definition("CD4 Result", getConcept("dcbcba2c-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("TB LAM", sdd.definition("TB LAM", getConcept("066b84a0-e18f-4cdd-a0d7-189454f4c7a4")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Viral Load Quantitative", sdd.definition("Viral Load Quantitative", getConcept("dc8d83e3-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Viral Load Qualitative", sdd.definition("Viral Load Qualitative", getConcept("dca12261-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("Age At TPT Start", sdd.definition("Age At TPT Start", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
//
//		dsd.addColumn("ART Status At TPT Start", sdd.definition("ART Status At TPT Start", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("TPT Initiation Date", sdd.definition("TPT Initiation Date", getConcept("483939c7-79ba-4ca4-8c3e-346488c97fc7")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("Weight", sdd.definition("Weight", getConcept("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Side Effects", sdd.definition("Side Effects", getConcept("23a6dc6e-ac16-4fa6-8029-155522548d04")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Outcome At End of TPT", sdd.definition("Outcome At End of TPT", getConcept("37d4ac43-b3b4-4445-b63b-e3acf47c8910")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("TPT Outcome Date", sdd.definition("TPT Outcome Date", getConcept("813e21e7-4ccb-4fe9-aaab-3c0e40b6e356")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("Date Of Registration in TB Unit", sdd.definition("Date Of Registration in TB Unit", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Disease Classification", sdd.definition("Disease Classification", getConcept("d45871ee-62d6-4d4d-b905-f7b75a3fd3bb")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Date Started On First Line Treatment", sdd.definition("Date Started On First Line Treatment", getConcept("7326297e-0ccd-4355-9b86-dde1c056e2c2")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		dsd.addColumn("Type Of TB Patient", sdd.definition("Type Of TB Patient", getConcept("e077f196-c19a-417f-adc6-b175a3343bfd")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("Pre-treatment Sputum Smear", sdd.definition("Pre-treatment Sputum Smear", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
//
//		dsd.addColumn("Pre-treatment GeneXpert Test", sdd.definition("Pre-treatment GeneXpert Test", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
//
//		dsd.addColumn("Other Treatment Investigation", sdd.definition("Other Treatment Investigation", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("TB treatment outcome", sdd.definition("Pre-treatment Sputum Smear", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
//
//		dsd.addColumn("TB treatment outcome date", sdd.definition("Pre-treatment GeneXpert Test", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
//
//		dsd.addColumn("Initial ART regimen", sdd.definition("Other Treatment Investigation", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("New ART regimen", sdd.definition("Pre-treatment Sputum Smear", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
//
//		dsd.addColumn("Date regimen switch", sdd.definition("Pre-treatment GeneXpert Test", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
//
//		dsd.addColumn("Status – Transfer out - Date", sdd.definition("Other Treatment Investigation", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("Status – LTFU – Date", sdd.definition("Pre-treatment Sputum Smear", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("Status – Dead - Date", sdd.definition("Other Treatment Investigation", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

//		dsd.addColumn("Cause of death", sdd.definition("Pre-treatment Sputum Smear", getConcept("dce03b2f-30ab-102d-86b0-7a5022ba4115")), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());

		rd.addDataSetDefinition("TPT_LIST", Mapped.mapStraightThrough(dsd));
		return rd;
	}

	@Override
	public String getVersion() {
		return "0.0.17";
	}
}
