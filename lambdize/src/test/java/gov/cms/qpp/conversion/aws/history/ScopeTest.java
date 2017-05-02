package gov.cms.qpp.conversion.aws.history;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import gov.cms.qpp.conversion.Converter;
import gov.cms.qpp.conversion.TransformationStatus;
import gov.cms.qpp.conversion.util.JsonHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

public class ScopeTest {
    private static final String BUCKET = "historical-qrda";
    private static final String ACCESS = "aws.accessKeyId";
	private static final String SECRET = "aws.secretKey";


	@BeforeClass
	@SuppressWarnings("unchecked")
	public static void setup() throws IOException {
		Map<String, String> properties = getS3Properties();

		System.setProperty(ACCESS, properties.get(ACCESS));
		System.setProperty(SECRET, properties.get(SECRET));
	}

	private static Map<String, String> getS3Properties() {
		Map<String, String> properties = null;
		try {
			Path path = Paths.get("src")
					.resolve("test")
					.resolve("resources")
					.resolve("s3Properties.json");
			properties = JsonHelper.readJson(path, Map.class);
		} catch (Exception e) {
			System.err.println("You must configure an s3Properties.json file: ");
			e.printStackTrace(System.err);
		}

		if (properties == null || !(properties.containsKey(ACCESS) && properties.containsKey(SECRET))){
			String message = String.format("s3Properties.json must contain %s and %s configurations.", ACCESS, SECRET);
			System.err.println(message);
		}

		return properties;
	}

	@Test
	public void s3() throws IOException {
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion("us-east-1").build();
		ObjectListing listing
				= s3Client.listObjects(BUCKET);
		listing.getObjectSummaries().parallelStream()
				.map(S3ObjectSummary::getKey)
				.map(key -> s3Client.getObject(BUCKET, key))
				.forEach( s3Object -> {
					InputStream stream = s3Object.getObjectContent();
					Converter convert = new Converter(stream);

					TransformationStatus status = convert.transform();
					assertThat("The file should have transformed correctly.", status, is(TransformationStatus.ERROR));

					InputStream jsonStream = convert.getConversionResult();

					List<Map<?, ?>> aciSections = JsonHelper.readJsonAtJsonPath(jsonStream,
						"$.measurementSets[?(@.category=='aci')]", List.class);
					assertThat("There must not be any ACI sections in historical data.", aciSections, hasSize(0));
				} );
	}

}
