package cz.zalusky.orgmedia;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * @author Tomas Zalusky
 */
public class Main {

	public static final Map<String,Conversion> conversions = ImmutableMap.of(
			"canon1",new Canon1Conversion(),
			"samsung1",new Samsung1Conversion()
	);
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			throw new RuntimeException("Usage: java -jar orgmedia.jar <conversion> <from> <to>");
		}
		String conversionString = args[0];
		Conversion conversion = conversions.get(conversionString);
		if (conversion == null) {
			throw new RuntimeException("Invalid conversion " + conversionString + ", valid conversions are " + conversions.keySet());
		}
		String sourceString = args[1];
		File source = new File(sourceString);
		if (!source.exists() || !source.isDirectory()) {
			throw new RuntimeException("The source location " + source + " doesn't exist or is not a directory");
		}
		String targetString = args[2];
		File target = new File(targetString);
		if (!target.exists() || !target.isDirectory()) {
			throw new RuntimeException("The target location " + target + " doesn't exist or is not a directory");
		}
		conversion.execute(source, target);
		System.in.read();
	}

}
