package cz.zalusky.orgmedia;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.Files;

/**
 * @author Tomas Zalusky
 */
public class HuaweiXiaomi1Conversion extends Conversion {

	static final Pattern VALID_FILES = Pattern.compile("(IMG|PANO|VID|SL_MO_VID)_(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)_\\d\\d\\d\\d\\d\\d(_HDR|_HHT|_\\d|_BURST\\d\\d\\d(_COVER)?)?\\.(jpg|mp4)");

	@Override
	public Report execute(File source, File target) {
		Report report = new Report();
		for (File sourceChild : source.listFiles()) {
			if (sourceChild.isDirectory()) {
				report.error("file %s is a directory, skipped",sourceChild);
				continue;
			}
			Matcher m = VALID_FILES.matcher(sourceChild.getName());
			if (!m.matches()) {
				report.error("file %s does not match /" + VALID_FILES.pattern() + "/, skipped",sourceChild);
				continue;
			}
			String year = m.group(2);
			String month = m.group(3);
			String day = m.group(4);
			String targetMonth = year + month;
			File targetMonthDirectory = new File(target,targetMonth);
			if (targetMonthDirectory.exists() && !targetMonthDirectory.isDirectory()) {
				report.error("target %s for file %s exists but is not a directory, skipped",targetMonthDirectory,sourceChild);
				continue;
			}
			if (!targetMonthDirectory.exists()) {
				if (!targetMonthDirectory.mkdir()) {
					report.error("target %s for file %s could not be created, skipped",targetMonthDirectory,sourceChild);
					continue;
				}
			}
			String targetDay = year + month + day;
			File targetDayDirectory = new File(targetMonthDirectory,targetDay);
			if (targetDayDirectory.exists() && !targetDayDirectory.isDirectory()) {
				report.error("target %s for file %s exists but is not a directory, skipped",targetDayDirectory,sourceChild);
				continue;
			}
			if (!targetDayDirectory.exists()) {
				if (!targetDayDirectory.mkdir()) {
					report.error("target %s for file %s could not be created, skipped",targetDayDirectory,sourceChild);
					continue;
				}
			}
			String name = sourceChild.getName();
			Set<File> filesWithEqualContent = findByEqualContent(targetDayDirectory,sourceChild);
			if (filesWithEqualContent.stream().anyMatch(f -> f.getName().equals(name))) { // same content and name
				delete(sourceChild, report, "file %s deleted because target exists with same content and name", sourceChild);
			} else if (!filesWithEqualContent.isEmpty()) { // same content, different name
				delete(sourceChild, report, "file %s deleted because target exists with same content and different name %s", sourceChild,
						filesWithEqualContent.stream().map(f -> f.getName()).collect(toSet()));
			} else { // content does not exist in target
				if (Arrays.stream(targetDayDirectory.listFiles()).anyMatch(f -> f.getName().equals(name))) { // name is already in use -> rename
					String newName = name;
					do {
						newName = newName.replaceAll("(.*)(\\..*)","$1_$2");
					} while (new File(targetDayDirectory,newName).exists());
					File targetChild = new File(targetDayDirectory,newName);
					move(sourceChild, targetChild, report, "file %s moved into %s because source name was in use in target with different content", sourceChild, targetChild);
				} else { // name not used -> just move
					File targetChild = new File(targetDayDirectory,name);
					move(sourceChild, targetChild, report, "file %s moved into %s", sourceChild, targetChild);
				}
			}
		}
		if (source.listFiles().length == 0) {
			delete(source, report, "there was no file remaining in directory %s", source);
		} else {
			report.error("preserving directory %s, there are remaining files %s", source, Arrays.asList(source.list()));
		}
		// TODO vysusit
		// TODO spustit na ostrych datech
		// TODO serazeni v ramci adresare
		File logFile = getLogFile(target);
		report.writeContentToLogFile(logFile,source,target);
		System.out.println("Converted " + source + " to " + target + " and logged into " + logFile.getAbsolutePath() + ".");
		return report;
	}

	private File getLogFile(File target) {
		String logFileName = getLogFileName();
		File logFile = new File(target,logFileName);
		return logFile;
	}

	private Set<File> findByEqualContent(File dir, File searched) {
		Set<File> result = Arrays.stream(dir.listFiles())
				.filter(f -> f.isFile())
				.filter(f -> {try {return Files.equal(f,searched);} catch (IOException e) {return false;}})
				.collect(toSet());
		return result;
	}

	private void delete(File file, Report report, String format, Object... args) {
		if (file.delete()) {
			report.ok(format, args);
		} else {
			report.error("unsuccessful attempt to perform delete with success message: " + format, args);
		}
	}
	
	private void move(File source, File target, Report report, String format, Object... args) {
		try {
			Files.move(source, target);
			report.ok(format, args);
		} catch (IOException e) {
			report.error("unsuccessful attempt to perform move with success message: " + format, args);
		}
	}
	
}
