package org.cprn.validator.app;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.beryx.textio.TextIoFactory;
import org.cprn.validator.CprnXmlValidator;

public class SwingApp
	extends ValidatorApp
{
	public static void main(String[] args) throws IOException {
		new SwingApp().run(args);
	}

	@Override
	public void run(String[] args) {
		boolean bErrorsFound = false;

		System.setProperty(TextIoFactory.TEXT_TERMINAL_CLASS_PROPERTY, "org.beryx.textio.swing.SwingTextTerminal");
		
		if (isHeadless()) {
			System.out.println("Headless");
			// If we're not headless, use the Swing terminal
		}
		else {
			// Now that we've determined what "mode" we're running in, initialize
			// the terminal.
			initializeTerminal();
	
			displayApplicationInformation(terminal);
	
			List<String> fileFolderList = readArgsFromConsole();
	
			List<File> fileList = buildFileList(fileFolderList);
	
			if (0 == fileList.size()) {
				terminal.println("No valid files selected");
			}
			else {
				for(File file : fileList) {
					terminal.println(String.format("Validating %s", file.toString()));
					bErrorsFound |= !CprnXmlValidator.validateFile(file);
				}
				terminal.println("Processed " + fileList.size() + " files.");
			}
	
			textIO
				.newStringInputReader()
				.withMinLength(0)
				.withPropertiesPrefix("info")
				.read("Press Enter to exit...");
	
			textIO.dispose();
		}	

		System.exit(bErrorsFound ? 1 : 0);
	}


}
