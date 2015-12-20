package cz.zalusky.orgmedia;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Tomas Zalusky
 */
public abstract class Conversion {

	public abstract Report execute(File source, File target);
	
	String getLogFileName() {
		String logFileName = String.format("%s-%s.log",this.getClass().getSimpleName(),LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
		return logFileName;
	}
	
}
