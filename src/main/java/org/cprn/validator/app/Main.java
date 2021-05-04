package org.cprn.validator.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.io.InputStreamReader;

import org.cprn.validator.CprnXmlValidator;

public class Main {
	private boolean printedUsage = false;

	public static void main(String[] args) {
		new Main().run(args);
	}
	
	public void run(String[] args) {
		boolean bErrorsFound = false;

		List<String> argList = Arrays.asList(args);

		if (argList.size() == 0) {
			printUsage(true);
			argList = readArgsFromConsole();
		}

		int fileCnt = 0;
		List<File> fileList = buildFileList(argList);

		for(File file : fileList) {
			bErrorsFound |= !CprnXmlValidator.validateFile(file);
			fileCnt++;
		}

		if (fileCnt > 0) {
			System.out.println("Processed " + fileCnt + " files.");
		} else {
			printUsage();
		}
		
		System.exit(bErrorsFound ? 1 : 0);
	}
	
	private void printUsage() {
		printUsage(false);
	}

	private void printUsage(boolean isInteractive) {
		if (!printedUsage) {
			System.out.println(
					new StringBuilder()
					.append("Usage:").append(System.lineSeparator())
					.append("\tInput files and/or folders can be provided on the command line or entered when prompted.").append(System.lineSeparator())
					.append("\tProviding multiple files and or folders is allowed.").append(System.lineSeparator())
					.append(System.lineSeparator())
					.append("\tE.g. CprnXmlValidator <input.xml> OR <folder containing XML files>").append(System.lineSeparator())
					.toString());
			printedUsage = true;
		}
	}

	private List<String> readArgsFromConsole() {
		List<String> argList = new ArrayList<String>();

		try (BufferedReader reader = new BufferedReader(
	            new InputStreamReader(System.in)) )
		{
			System.out.print("Enter files and/or folders to validate: ");;
			String inputLine = reader.readLine();
			String[] args = inputLine.split(" ");
			
			for(String arg : args) {
				arg = arg.trim();
				if (arg.length() > 0) {
					argList.add(arg);
				}
			}
		}
		catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}

		return argList;
	}

	private List<File> buildFileList(List<String> argList) {
		List<File> fileList = new LinkedList<File>();

		for(String arg : argList) {
			File src = new File(arg);
			
			if (src.isDirectory()) {
				File[] directoryListing = src.listFiles(new FilenameFilter() {
					@Override
				    public boolean accept(File dir, String name) {
						return name.endsWith(".xml");
				    }
				});
				
				if (directoryListing != null) {
				    for (File file : directoryListing) {
				    	if (!fileList.contains(file)) {
				    		fileList.add(file);
				    	}
				    }
				}
			}
			else if (src.isFile() && !fileList.contains(src)) {
				fileList.add(src);
			}
			else if (!src.exists()) {
				System.out.println(String.format("The specified file (%s) doees not exist", arg));
			}
		}

		return fileList;
	}
}
