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

public class HuaweiXiaomi1ConversionTest {

	@Rule
	public TemporaryFolder source = new TemporaryFolder();

	@Rule
	public TemporaryFolder target = new TemporaryFolder();

	public HuaweiXiaomi1Conversion conversion = new HuaweiXiaomi1Conversion();

	private Report report;
	
	@Test
	public void emptySourceEmptyTarget() {
		performConversion();
		verifyTarget("OK   : there was no file remaining in directory $SOURCE$");
	}

	@Test
	public void sourceMustNotHaveIllegalFiles() {
		prepareSource("file.txt");
		performConversion();
		verifyTarget("ERROR: file $SOURCE$\\file.txt does not match /" + HuaweiXiaomi1Conversion.VALID_FILES + "/, skipped","ERROR: preserving directory $SOURCE$, there are remaining files [file.txt]");
		assertEquals(1, target.getRoot().listFiles().length); // log
	}

	@Test
	public void sourceMustNotHaveDirectories() {
		prepareSource("invaliddir/");
		performConversion();
		verifyTarget("ERROR: file $SOURCE$\\invaliddir is a directory, skipped","ERROR: preserving directory $SOURCE$, there are remaining files [invaliddir]");
		assertEquals(1, target.getRoot().listFiles().length); // log
	}
	
	@Test
	public void sourceMonthMustNotExistAsTargetFile() {
		prepareSource("IMG_20151210_010203.jpg");
		prepareTarget("201512");
		performConversion();
		verifyTarget("ERROR: target $TARGET$\\201512 for file $SOURCE$\\IMG_20151210_010203.jpg exists but is not a directory, skipped","ERROR: preserving directory $SOURCE$, there are remaining files [IMG_20151210_010203.jpg]");
	}
	
	@Test
	public void sourceDayMustNotExistAsTargetFile() {
		prepareSource("IMG_20151210_010203.jpg");
		prepareTarget("201512/","201512/20151210");
		performConversion();
		verifyTarget("ERROR: target $TARGET$\\201512\\20151210 for file $SOURCE$\\IMG_20151210_010203.jpg exists but is not a directory, skipped","ERROR: preserving directory $SOURCE$, there are remaining files [IMG_20151210_010203.jpg]");
	}
	
	@Test
	public void sameContentSameNameDeleted() {
		prepareSource("IMG_20151210_010203.jpg~abc");
		prepareTarget("201512/","201512/20151210/IMG_20151210_010203.jpg~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\IMG_20151210_010203.jpg deleted because target exists with same content and name", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertNotExists("$SOURCE$");
	}
	
	@Test
	public void sameContentDifferentNameDeleted() {
		prepareSource("IMG_20151210_010203.jpg~abc");
		prepareTarget("201512/","201512/20151210/IMG_20151210_040506.jpg~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\IMG_20151210_010203.jpg deleted because target exists with same content and different name [IMG_20151210_040506.jpg]", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertNotExists("$SOURCE$");
	}
	
	@Test
	public void differentContentSameNameRenamed() throws IOException {
		prepareSource("IMG_20151210_010203.jpg~abc");
		prepareTarget("201512/","201512/20151210/IMG_20151210_010203.jpg~def");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\IMG_20151210_010203.jpg moved into $TARGET$\\201512\\20151210\\IMG_20151210_010203_.jpg because source name was in use in target with different content", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",2);
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203.jpg","def");
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203_.jpg","abc");
		assertNotExists("$SOURCE$");
	}
	
	@Test
	public void differentContentSameNameRenamedAfterRecoveryFromNameConflict() throws IOException {
		prepareSource("IMG_20151210_010203.jpg~abc");
		prepareTarget("201512/","201512/20151210/IMG_20151210_010203.jpg~def");
		prepareTarget("201512/","201512/20151210/IMG_20151210_010203_.jpg~ghi");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\IMG_20151210_010203.jpg moved into $TARGET$\\201512\\20151210\\IMG_20151210_010203__.jpg because source name was in use in target with different content", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",3);
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203.jpg","def");
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203_.jpg","ghi");
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203__.jpg","abc");
		assertNotExists("$SOURCE$");
	}

	@Test
	public void justMove() throws IOException {
		prepareSource("IMG_20151210_010203.jpg~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\IMG_20151210_010203.jpg moved into $TARGET$\\201512\\20151210\\IMG_20151210_010203.jpg", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203.jpg","abc");
		assertNotExists("$SOURCE$");
	}

	@Test
	public void justMove_Hdr() throws IOException {
		prepareSource("IMG_20151210_010203_HDR.jpg~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\IMG_20151210_010203_HDR.jpg moved into $TARGET$\\201512\\20151210\\IMG_20151210_010203_HDR.jpg", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203_HDR.jpg","abc");
		assertNotExists("$SOURCE$");
	}

	@Test
	public void justMove_Hht() throws IOException {
		prepareSource("IMG_20151210_010203_HHT.jpg~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\IMG_20151210_010203_HHT.jpg moved into $TARGET$\\201512\\20151210\\IMG_20151210_010203_HHT.jpg", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203_HHT.jpg","abc");
		assertNotExists("$SOURCE$");
	}

	@Test
	public void justMove_NumberAtTheEnd() throws IOException {
		prepareSource("IMG_20151210_010203_1.jpg~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\IMG_20151210_010203_1.jpg moved into $TARGET$\\201512\\20151210\\IMG_20151210_010203_1.jpg", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertExists("$TARGET$\\201512\\20151210\\IMG_20151210_010203_1.jpg","abc");
		assertNotExists("$SOURCE$");
	}

	@Test
	public void justMove_Pano() throws IOException {
		prepareSource("PANO_20151210_010203.jpg~abc");
		performConversion();
		verifyTarget("OK   : file $SOURCE$\\PANO_20151210_010203.jpg moved into $TARGET$\\201512\\20151210\\PANO_20151210_010203.jpg", "OK   : there was no file remaining in directory $SOURCE$");
		assertExists("$TARGET$\\201512\\20151210",1);
		assertExists("$TARGET$\\201512\\20151210\\PANO_20151210_010203.jpg","abc");
		assertNotExists("$SOURCE$");
	}

	@Test
	public void complexTest() throws IOException {
		prepareSource("IMG_20150129_082558.jpg~abc","IMG_20150129_082616.jpg~def","IMG_20150129_082621.jpg~ghi","IMG_20150130_181055.jpg~jkl","VID_20150130_184800.mp4~mno");
		performConversion();
		verifyTarget(
				"OK   : file $SOURCE$\\IMG_20150129_082558.jpg moved into $TARGET$\\201501\\20150129\\IMG_20150129_082558.jpg",
				"OK   : file $SOURCE$\\IMG_20150129_082616.jpg moved into $TARGET$\\201501\\20150129\\IMG_20150129_082616.jpg",
				"OK   : file $SOURCE$\\IMG_20150129_082621.jpg moved into $TARGET$\\201501\\20150129\\IMG_20150129_082621.jpg",
				"OK   : file $SOURCE$\\IMG_20150130_181055.jpg moved into $TARGET$\\201501\\20150130\\IMG_20150130_181055.jpg",
				"OK   : file $SOURCE$\\VID_20150130_184800.mp4 moved into $TARGET$\\201501\\20150130\\VID_20150130_184800.mp4",
				"OK   : there was no file remaining in directory $SOURCE$"
		);
		assertExists("$TARGET$\\201501\\20150129",3);
		assertExists("$TARGET$\\201501\\20150129\\IMG_20150129_082558.jpg","abc");
		assertExists("$TARGET$\\201501\\20150129\\IMG_20150129_082616.jpg","def");
		assertExists("$TARGET$\\201501\\20150129\\IMG_20150129_082621.jpg","ghi");
		assertExists("$TARGET$\\201501\\20150130",2);
		assertExists("$TARGET$\\201501\\20150130\\IMG_20150130_181055.jpg","jkl");
		assertExists("$TARGET$\\201501\\20150130\\VID_20150130_184800.mp4","mno");
		assertNotExists("$SOURCE$");
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

