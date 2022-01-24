package org.cprn.validator.app;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.beryx.textio.TextIoFactory;

public class ConsoleApp
	extends ValidatorApp
{
	public static void main(String[] args) throws IOException {
		new ConsoleApp().run(args);
	}

	@Override
	public void run(String[] args) {
		boolean bErrorsFound = false;

//		System.setProperty(TextIoFactory.TEXT_TERMINAL_CLASS_PROPERTY, "org.beryx.textio.system.SystemTextTerminal");
		System.setProperty(TextIoFactory.TEXT_TERMINAL_CLASS_PROPERTY, "org.beryx.textio.jline.JLineTextTerminal");
		
		List<String> fileFolderList = Arrays.asList(args);

		initializeTerminal();

		displayApplicationInformation(terminal);
		
		if (0 == fileFolderList.size()) {
			printUsage();
			bErrorsFound = true;
		}
		else {
			List<File> fileList = buildFileList(fileFolderList);
	
			if (0 == fileList.size()) {
				printUsage();
			}
			else {
				bErrorsFound = validateFiles(fileList);
			}

			textIO.dispose();
		}

		System.exit(bErrorsFound ? 1 : 0);
	}

	/**
	 * Print the application usage.
	 */
	protected void printUsage() {
		terminal.println(
				new StringBuilder()
				.append("Usage:").append(System.lineSeparator())
				.append("\tInput files and/or folders are provided on the command line.").append(System.lineSeparator())
				.append("\tProviding multiple files, folders, or a combination is allowed.").append(System.lineSeparator())
				.append(System.lineSeparator())
				.append("\tE.g. CprnXmlValidator <input.xml> OR <folder containing XML files>").append(System.lineSeparator())
				.toString());

		textIO
			.newStringInputReader()
			.withMinLength(0)
			.withPropertiesPrefix("info")
			.read("Press Enter to exit...");
	}

}
