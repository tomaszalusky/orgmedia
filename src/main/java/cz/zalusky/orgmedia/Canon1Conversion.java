package cz.zalusky.orgmedia;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.io.Files;

/**
 * @author Tomas Zalusky
 */
public class Canon1Conversion extends Conversion {

	private static final Pattern DAY_DIRECTORY = Pattern.compile("(\\d\\d\\d\\d)_(\\d\\d)_(\\d\\d)");

	private static final Pattern VALID_FILES = Pattern.compile("IMG_\\d\\d\\d\\d\\.JPG|MVI_\\d\\d\\d\\d\\.AVI");
	
	@Override
	public Report execute(File source, File target) {
		Report report = new Report();
		for (File sourceChild : source.listFiles()) {
			if (!sourceChild.isDirectory()) {
				report.error("file %s is not a directory, skipped",sourceChild);
				continue;
			}
			Matcher m = DAY_DIRECTORY.matcher(sourceChild.getName());
			if (!m.matches()) {
				report.error("directory %s is not YYYY_MM_DD, skipped",sourceChild);
				continue;
			}
			String year = m.group(1);
			String month = m.group(2);
			String day = m.group(3);
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
			for (File sourceGrandChild : sourceChild.listFiles()) {
				String name = sourceGrandChild.getName();
				if ("ZbThumbnail.info".equals(name)) {
					delete(sourceGrandChild, report, "file %s deleted", sourceGrandChild);
				} else if (name.matches("(?i).*\\.thm")) {
					delete(sourceGrandChild, report, "file %s deleted", sourceGrandChild);
				} else if (VALID_FILES.matcher(name).matches()) {
					Set<File> filesWithEqualContent = findByEqualContent(targetDayDirectory,sourceGrandChild);
					if (filesWithEqualContent.stream().anyMatch(f -> f.getName().equals(name))) { // same content and name
						delete(sourceGrandChild, report, "file %s deleted because target exists with same content and name", sourceGrandChild);
					} else if (!filesWithEqualContent.isEmpty()) { // same content, different name
						delete(sourceGrandChild, report, "file %s deleted because target exists with same content and different name ", sourceGrandChild,
								filesWithEqualContent.stream().map(f -> f.getName()).collect(Collectors.toSet()));
					} else { // content does not exist in target
						if (Arrays.stream(targetDayDirectory.listFiles()).anyMatch(f -> f.getName().equals(name))) { // name is already in use -> rename
							String newName = name;
							do {
								newName = newName.replaceAll("(.*)(\\..*)","$1_$2");
							} while (new File(targetDayDirectory,newName).exists());
							File targetGrandChild = new File(targetDayDirectory,newName);
							move(sourceGrandChild, targetGrandChild, report, "file %s moved into %s because source name was in use in target with different content", sourceGrandChild, targetGrandChild);
						} else { // name not used -> just move
							File targetGrandChild = new File(targetDayDirectory,name);
							move(sourceGrandChild, targetGrandChild, report, "file %s moved into %s", sourceGrandChild, targetGrandChild);
						}
					}
				} else {
					report.error("unexpected file %s, skipped", sourceGrandChild);
				}
			}
			if (sourceChild.listFiles().length == 0) {
				delete(sourceChild, report, "there was no file remaining in directory");
			} else {
				report.error("preserving directory %s, there are remaining files %s", sourceChild, Arrays.asList(sourceChild.list()));
			}
			// TODO unit test
			// TODO rucni test
			// TODO test na kopii ostrych dat
			// TODO spustit na ostrych datech
			// TODO samsungconversion
			// TODO serazeni v ramci adresare
		}
		System.out.println(report.getContent());
		System.out.println("Converting " + source + " to " + target);
		return report;
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
			report.error("attempt unsuccessful: " + format, args);
		}
	}
	
	private void move(File source, File target, Report report, String format, Object... args) {
		try {
			Files.move(source, target);
			report.ok(format, args);
		} catch (IOException e) {
			report.error("attempt unsuccessful: " + format, args);
		}
	}
	
}
