package cz.zalusky.orgmedia;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

public class Canon1ConversionTest {

	@Rule
	public TemporaryFolder source = new TemporaryFolder();

	@Rule
	public TemporaryFolder target = new TemporaryFolder();

	public Canon1Conversion conversion = new Canon1Conversion();

	private Report report;
	
	@Test
	public void emptySourceEmptyTarget() {
		performConversion();
		verifyTarget();
	}

	@Test
	public void sourceMustHaveOnlySubdirectories() {
		prepareSource("file.txt");
		performConversion();
		verifyTarget("ERROR: file $SOURCE$\\file.txt is not a directory, skipped");
		assertEquals(0, target.getRoot().listFiles().length);
	}

	private void prepareSource(String... fileNames) {
		try {
			for (String fileName : fileNames) {
				File file = source.newFile(fileName);
				Files.write((fileName + "***").substring(0,3), file, Charsets.UTF_8);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void performConversion() {
		this.report = conversion.execute(source.getRoot(), target.getRoot());
	}

	private void verifyTarget(String... expectedReportContent) {
		List<String> actualReportContent = report.getContent();
		assertEquals(expectedReportContent.length, actualReportContent.size());
		IntStream.range(0, expectedReportContent.length).forEach(i -> {
			String expectedReportContentForComparison = expectedReportContent[i]
					.replace("$SOURCE$",source.getRoot().getAbsolutePath());
			assertEquals(expectedReportContentForComparison, actualReportContent.get(i));
		});
	}

}

