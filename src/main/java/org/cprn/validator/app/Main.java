package org.cprn.validator.app;

import java.io.File;
import java.io.FilenameFilter;

import org.cprn.validator.CprnXmlValidator;

public class Main {

	public static void main(String[] args) {
		if (args.length != 0) {
			int fileCnt = 0;

			System.setProperty("http.agent", "Chrome");	// Needed so server will accept our request and not return 403
			
			File src = new File(args[0]);
			
			if (src.isDirectory()) {
				File[] directoryListing = src.listFiles(new FilenameFilter() {
					@Override
				    public boolean accept(File dir, String name) {
						return name.endsWith(".xml");
				    }
				});
				
				if (directoryListing != null) {
				    for (File file : directoryListing) {
				    	new CprnXmlValidator().validateFile(file);
						fileCnt++;
				    }
				}
			}
			else if (src.isFile()) {
		    	new CprnXmlValidator().validateFile(src);
				fileCnt++;
			}
			else if (!src.exists()) {
				System.out.println("The specified file doees not exist");
				Main.printUsage();
			}
			else {
				System.out.println("Invalid argument(s)");
				Main.printUsage();
			}
			
			if (fileCnt > 0) {
				System.out.println("Processed " + fileCnt + " files.");
			}
		}
		else {
			Main.printUsage();
		}

	}
	
	private static void printUsage() {
		System.out.println(
				new StringBuilder()
				.append("Usage:").append(System.lineSeparator())
				.append("\t   CprnXmlValidator <input.xml> OR <folder containing XML files>").append(System.lineSeparator())
				.append("\tor java -jar CprnXmlValidator.jar <input.xml> OR <folder containing XML files\">").append(System.lineSeparator())
				.toString());
	}
}
