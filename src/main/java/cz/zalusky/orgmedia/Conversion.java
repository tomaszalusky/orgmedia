package cz.zalusky.orgmedia;

import java.io.File;

/**
 * @author Tomas Zalusky
 */
public abstract class Conversion {

	public abstract Report execute(File source, File target);
	
}
