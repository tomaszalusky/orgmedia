package cz.zalusky.orgmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
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
	
	@Test
	public void thmDeleted() {
		prepareSource("2015_12_10/","2015_12_10/MVI_1234.THM","2015_12_10/mvi_5678.thm");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\2015_12_10\\MVI_1234.THM deleted", "OK   : file $SOURCE$\\2015_12_10\\mvi_5678.thm deleted", "OK   : there was no file remaining in directory");
		assertExists("$TARGET$\\201512\\20151210",0);
		assertNotExists("$SOURCE$\\2015_12_10");
	}
	
	@Test
	public void sameContentSameNameDeleted() {
		prepareSource("2015_12_10/","2015_12_10/IMG_1234.JPG~abc");
		prepareTarget("201512/","201512/20151210/IMG_1234.JPG~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\2015_12_10\\IMG_1234.JPG deleted because target exists with same content and name", "OK   : there was no file remaining in directory");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertNotExists("$SOURCE$\\2015_12_10");
	}
	
	@Test
	public void sameContentDifferentNameDeleted() {
		prepareSource("2015_12_10/","2015_12_10/IMG_1234.JPG~abc");
		prepareTarget("201512/","201512/20151210/IMG_5678.JPG~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\2015_12_10\\IMG_1234.JPG deleted because target exists with same content and different name [IMG_5678.JPG]", "OK   : there was no file remaining in directory");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertNotExists("$SOURCE$\\2015_12_10");
	}
	
	@Test
	public void differentContentSameNameRenamed() throws IOException {
		prepareSource("2015_12_10/","2015_12_10/IMG_1234.JPG~abc");
		prepareTarget("201512/","201512/20151210/IMG_1234.JPG~def");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\2015_12_10\\IMG_1234.JPG moved into $TARGET$\\201512\\20151210\\IMG_1234_.JPG because source name was in use in target with different content", "OK   : there was no file remaining in directory");
		assertExists("$TARGET$\\201512\\20151210",2);
		assertExists("$TARGET$\\201512\\20151210\\IMG_1234.JPG","def");
		assertExists("$TARGET$\\201512\\20151210\\IMG_1234_.JPG","abc");
		assertNotExists("$SOURCE$\\2015_12_10");
	}
	
	@Test
	public void differentContentSameNameRenamedAfterRecoveryFromNameConflict() throws IOException {
		prepareSource("2015_12_10/","2015_12_10/IMG_1234.JPG~abc");
		prepareTarget("201512/","201512/20151210/IMG_1234.JPG~def");
		prepareTarget("201512/","201512/20151210/IMG_1234_.JPG~ghi");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\2015_12_10\\IMG_1234.JPG moved into $TARGET$\\201512\\20151210\\IMG_1234__.JPG because source name was in use in target with different content", "OK   : there was no file remaining in directory");
		assertExists("$TARGET$\\201512\\20151210",3);
		assertExists("$TARGET$\\201512\\20151210\\IMG_1234.JPG","def");
		assertExists("$TARGET$\\201512\\20151210\\IMG_1234_.JPG","ghi");
		assertExists("$TARGET$\\201512\\20151210\\IMG_1234__.JPG","abc");
		assertNotExists("$SOURCE$\\2015_12_10");
	}

	@Test
	public void justMove() throws IOException {
		prepareSource("2015_12_10/","2015_12_10/IMG_1234.JPG~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\2015_12_10\\IMG_1234.JPG moved into $TARGET$\\201512\\20151210\\IMG_1234.JPG", "OK   : there was no file remaining in directory");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertExists("$TARGET$\\201512\\20151210\\IMG_1234.JPG","abc");
		assertNotExists("$SOURCE$\\2015_12_10");
	}

	@Test
	public void unexpectedFileSkipped() throws IOException {
		prepareSource("2015_12_10/","2015_12_10/foo.dat~abc");
		performConversion();
		verifyTarget("ERROR: unexpected file $SOURCE$\\2015_12_10\\foo.dat, skipped", "ERROR: preserving directory $SOURCE$\\2015_12_10, there are remaining files [foo.dat]");
		assertExists("$TARGET$\\201512\\20151210",0);
		assertExists("$SOURCE$\\2015_12_10\\foo.dat","abc");
	}

	@Test
	public void complexTest() throws IOException {
		prepareSource("2015_01_01/","2015_01_01/IMG_0573.JPG~abc","2015_01_01/IMG_0574.JPG~def","2015_01_01/MVI_0575.AVI~ghi","2015_01_01/MVI_0575.THM~jkl","2015_01_01/ZbThumbnail.info~mno");
		performConversion();
		verifyTarget(
				"OK   : file $SOURCE$\\2015_01_01\\IMG_0573.JPG moved into $TARGET$\\201501\\20150101\\IMG_0573.JPG",
				"OK   : file $SOURCE$\\2015_01_01\\IMG_0574.JPG moved into $TARGET$\\201501\\20150101\\IMG_0574.JPG",
				"OK   : file $SOURCE$\\2015_01_01\\MVI_0575.AVI moved into $TARGET$\\201501\\20150101\\MVI_0575.AVI",
				"OK   : file $SOURCE$\\2015_01_01\\MVI_0575.THM deleted",
				"OK   : file $SOURCE$\\2015_01_01\\ZbThumbnail.info deleted",
				"OK   : there was no file remaining in directory"
		);
		assertExists("$TARGET$\\201501\\20150101",3);
		assertExists("$TARGET$\\201501\\20150101\\IMG_0573.JPG","abc");
		assertExists("$TARGET$\\201501\\20150101\\IMG_0574.JPG","def");
		assertExists("$TARGET$\\201501\\20150101\\MVI_0575.AVI","ghi");
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
					int tilde = fileName.indexOf("~");
					String content;
					if (tilde == -1) {
						content = (fileName + "***").substring(0,3);
					} else {
						content = fileName.substring(tilde + 1);
						fileName = fileName.substring(0,tilde);
					}
					File file = new File(dir,fileName);
					if (!file.createNewFile()) throw new IOException();
					Files.write(content, file, Charsets.UTF_8);
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

	private void assertExists(String filePath, String expectedContent) throws IOException {
		String replaced = filePath
				.replace("$SOURCE$",source.getRoot().getAbsolutePath())
				.replace("$TARGET$",target.getRoot().getAbsolutePath());
		File f = new File(replaced);
		assertTrue(f.getAbsolutePath(),f.exists());
		String actualContent = Files.toString(f, Charsets.UTF_8);
		assertEquals(expectedContent, actualContent);
	}

	private void assertNotExists(String filePath) {
		String replaced = filePath
				.replace("$SOURCE$",source.getRoot().getAbsolutePath())
				.replace("$TARGET$",target.getRoot().getAbsolutePath());
		File f = new File(replaced);
		assertFalse(f.exists());
	}

}

