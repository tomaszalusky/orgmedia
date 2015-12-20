package cz.zalusky.orgmedia;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.io.Files;

public class Report {
	
	private static final String LS = String.format("%n");

	private final List<String> content = new ArrayList<>();

	public void ok(String format, Object... args) {
		content.add(String.format("OK   : " + format,args));
	}
	
	public void error(String format, Object... args) {
		content.add(String.format("ERROR: " + format,args));
	}
	
	public List<String> getContent() {
		return content;
	}

	public void writeContentToLogFile(File logFile, File source, File target) {
		String contentAsString = "Converted " + source + " to " + target + ":" + LS
				+ content.stream().collect(Collectors.joining(LS));
		try {
			Files.write(contentAsString,logFile,Charset.defaultCharset());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
