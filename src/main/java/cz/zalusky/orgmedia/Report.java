package cz.zalusky.orgmedia;

import java.util.ArrayList;
import java.util.List;

public class Report {

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
	
}
