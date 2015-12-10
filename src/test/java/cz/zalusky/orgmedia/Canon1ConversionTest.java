package cz.zalusky.orgmedia;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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
	public void sourceMustNotHaveFiles() {
		prepareSource("file.txt");
		performConversion();
		verifyTarget("ERROR: file $SOURCE$\\file.txt is not a directory, skipped");
		assertEquals(0, target.getRoot().listFiles().length);
	}

	@Test
	public void sourceMustNotHaveDirectoriesNotMatchingDayPattern() {
		prepareSource("invaliddir/");
		performConversion();
		verifyTarget("ERROR: directory $SOURCE$\\invaliddir is not YYYY_MM_DD, skipped");
		assertEquals(0, target.getRoot().listFiles().length);
	}
	
	@Test
	public void sourceMonthMustNotExistAsTargetFile() {
		prepareSource("2015_12_10/");
		prepareTarget("201512");
		performConversion();
		verifyTarget("ERROR: target $TARGET$\\201512 for file $SOURCE$\\2015_12_10 exists but is not a directory, skipped");
	}
	
	@Test
	public void sourceDayMustNotExistAsTargetFile() {
		prepareSource("2015_12_10/");
		prepareTarget("201512/","201512/20151210");
		performConversion();
		verifyTarget("ERROR: target $TARGET$\\201512\\20151210 for file $SOURCE$\\2015_12_10 exists but is not a directory, skipped");
	}
	
	@Test
	public void zbThumbnailInfoDeleted() {
		prepareSource("2015_12_10/","2015_12_10/ZbThumbnail.info");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\2015_12_10\\ZbThumbnail.info deleted", "OK   : there was no file remaining in directory");
		assertExists("$TARGET$\\201512\\20151210",0);
		assertNotExists("$SOURCE$\\2015_12_10");
	}
	
	private void prepareSource(String... fileNames) {
		prepare(source, fileNames);
	}
	
	private void prepareTarget(String... filePaths) {
		prepare(target, filePaths);
	}
	
	private static void prepare(TemporaryFolder tempFolder, String... filePaths) {
		try {
			for (String filePath : filePaths) {
				List<String> split = Splitter.on('/').splitToList(filePath);
				File dir = tempFolder.getRoot();
				for (String subdirName : split.subList(0, split.size() - 1)) {
					File subdir = new File(dir,subdirName);
					if (!subdir.exists()) {
						if (!subdir.mkdir()) throw new IOException();
					}
					if (!subdir.isDirectory()) throw new IOException();
					dir = subdir;
				}
				String fileName = Iterables.getLast(split);
				if (!"".equals(fileName)) {
					File file = new File(dir,fileName);
					if (!file.createNewFile()) throw new IOException();
					Files.write((fileName + "***").substring(0,3), file, Charsets.UTF_8);
				}
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
		assertEquals("Unequal length of expected and actual report:\n" + Arrays.asList(expectedReportContent) + "\n" + actualReportContent,
				expectedReportContent.length, actualReportContent.size());
		IntStream.range(0, expectedReportContent.length).forEach(i -> {
			String expectedReportContentForComparison = expectedReportContent[i]
					.replace("$SOURCE$",source.getRoot().getAbsolutePath())
					.replace("$TARGET$",target.getRoot().getAbsolutePath());
			assertEquals(expectedReportContentForComparison, actualReportContent.get(i));
		});
	}

	private void assertExists(String filePath, Integer expectedChildCount) {
		String replaced = filePath
				.replace("$SOURCE$",source.getRoot().getAbsolutePath())
				.replace("$TARGET$",target.getRoot().getAbsolutePath());
		File f = new File(replaced);
		assertTrue(f.getAbsolutePath(),f.exists());
		if (expectedChildCount != null) {
			assertEquals(expectedChildCount.intValue(), f.listFiles().length);
		}
	}

	private void assertNotExists(String filePath) {
		String replaced = filePath
				.replace("$SOURCE$",source.getRoot().getAbsolutePath())
				.replace("$TARGET$",target.getRoot().getAbsolutePath());
		File f = new File(replaced);
		assertFalse(f.exists());
	}

}

