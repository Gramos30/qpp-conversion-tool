package gov.cms.qpp.acceptance;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.util.ClasspathHelper;

import gov.cms.qpp.TestHelper;
import gov.cms.qpp.conversion.Context;
import gov.cms.qpp.conversion.decode.QrdaDecoderEngine;
import gov.cms.qpp.conversion.decode.XmlDecoderEngine;
import gov.cms.qpp.conversion.decode.placeholder.DefaultDecoder;
import gov.cms.qpp.conversion.encode.QppOutputEncoder;
import gov.cms.qpp.conversion.model.Node;
import gov.cms.qpp.conversion.model.TemplateId;
import gov.cms.qpp.conversion.xml.XmlException;
import gov.cms.qpp.conversion.xml.XmlUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;

class ClinicalDocumentRoundTripTest {

	private static String expected;

	@BeforeAll
	static void setup() throws IOException {
		expected = TestHelper.getFixture("clinicalDocument.json");
	}

	@Test
	void parseClinicalDocument() throws Exception {
		InputStream stream =
				ClasspathHelper.contextClassLoader().getResourceAsStream("valid-QRDA-III-abridged.xml");
		String xmlFragment = IOUtils.toString(stream, StandardCharsets.UTF_8);

		Context context = new Context();
		Node clinicalDocumentNode = XmlDecoderEngine.decodeXml(context, XmlUtils.stringToDom(xmlFragment));

		// remove default nodes (will fail if defaults change)
		DefaultDecoder.removeDefaultNode(clinicalDocumentNode.getChildNodes());

		QppOutputEncoder encoder = new QppOutputEncoder(context);
		encoder.setNodes(Collections.singletonList(clinicalDocumentNode));

		StringWriter sw = new StringWriter();
		encoder.encode(new BufferedWriter(sw));

		assertThat(sw.toString()).isEqualTo(expected);
	}

	@Test
	void checkCorrectClinicalDocumentTemplateIdWins() throws XmlException {
		String similarClinicalDocumentBlob = "<ClinicalDocument xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
			+ "\t\t\t\t  xsi:schemaLocation=\"urn:hl7-org:v3 ../CDA_Schema_Files/infrastructure/cda/CDA_SDTC.xsd\"\n"
			+ "\t\t\t\t  xmlns=\"urn:hl7-org:v3\" xmlns:voc=\"urn:hl7-org:v3/voc\">\n"
			+ "\t<realmCode code=\"US\"/>\n"
			+ "\t<typeId root=\"2.16.840.1.113883.1.3\" extension=\"POCD_HD000040\"/>\n"
			+ "\t<templateId root=\"2.16.840.1.113883.10.20.27.1.2\"/>\n"
			+ "\t<templateId root=\"2.16.840.1.113883.10.20.27.1.2\" extension=\"2017-07-01\"/>\n"
			+ "</ClinicalDocument>";

		Node root = new QrdaDecoderEngine(new Context()).decode(XmlUtils.stringToDom(similarClinicalDocumentBlob));

		assertThat(root.getType()).isEqualTo(TemplateId.CLINICAL_DOCUMENT);
	}
}
