package org.cprn.validator.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.beryx.textio.TextIoFactory;
import org.beryx.textio.swing.SwingTextTerminal;
import org.cprn.validator.CprnXmlValidator;

public class SwingApp
	extends ValidatorApp
{
	private static final String INPUT_FILE_PROMPT_BOOKMARK = "filePrompt";
	private static final String INITIAL_CHOOSER_SELECTION = "InitialChooserSelection";

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


	/**
	 * Read the file and/or folders from the terminal. The assumption is that
	 * this method is called because no files or folder were provided on the
	 * command line.
	 * <p>
	 * When the Swing terminal is being used we use the JFileChooser to select
	 * the file or folder to validate. In the case, there will only be one.
	 * If a Console or JLine terminal is used it is possible for the user to
	 * provide multiple files and/or folders.
	 * 
	 * @return A list of files and/or folders to be validate.
	 */
	protected List<String> readArgsFromConsole() {
		List<String> argList = new ArrayList<String>();
		boolean showError = false;
		boolean keepTrying = true;

		terminal.setBookmark(INPUT_FILE_PROMPT_BOOKMARK);

		while (keepTrying) {
			String[] args = {};

			if (showError) {
				terminal.println("Unable to access file(s) or directory.");
			}

			SwingTextTerminal swingTerminal = (SwingTextTerminal) terminal;
			File initialFile = new File(propFile.getProperty(INITIAL_CHOOSER_SELECTION, ""));

			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("CPRN XML Validation Tool");
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.setSelectedFile(initialFile);
			chooser.addChoosableFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					boolean result = false;

					if (f.isDirectory()) {
						result = true;
					}
					else {
						String ext = null;
						String s = f.getName();
						int i = s.lastIndexOf('.');

						if (i > 0 &&  i < s.length() - 1) {
							ext = s.substring(i+1).toLowerCase();
						}
						result = "xml".equals(ext);
					}

					return result;
				}

				@Override
				public String getDescription() { return "XML Files"; }
			});

			int dlgResponse = chooser.showOpenDialog(swingTerminal.getFrame());
			if (JFileChooser.APPROVE_OPTION == dlgResponse) {
				final File fileLocn = chooser.getSelectedFile();
				propFile.setProperty(INITIAL_CHOOSER_SELECTION, fileLocn.getAbsolutePath());
				try { propFile.save(); } catch (IOException ioe) {}

				args = new String[] {fileLocn.toString()};
			}
			else {
				terminal.println("Cancelled");
				keepTrying = false;
			}

			for(String arg : args) {
				arg = arg.trim();

				if (arg.length() > 0) {
					Path path = Paths.get(arg);
					if (Files.exists(path)) {
						if (Files.isReadable(path) ) {
							argList.add(arg);
							keepTrying = false;
						}
					}
				}
			}

			if (keepTrying) {
				terminal.resetToBookmark(INPUT_FILE_PROMPT_BOOKMARK);
				showError = true;
			}
		}

		return argList;
	}
}
