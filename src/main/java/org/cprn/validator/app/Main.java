package org.cprn.validator.app;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.beryx.textio.TerminalProperties;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.beryx.textio.swing.SwingTextTerminal;
import org.cprn.validator.CprnXmlValidator;

public class Main {
	private static final String INPUT_FILE_PROMPT_BOOKMARK = "filePrompt";
	private static final String INITIAL_CHOOSER_SELECTION = "InitialChooserSelection";

	private boolean printedUsage = false;

	protected TextIO textIO;
	protected TextTerminal<?> terminal;

	protected AppPropertiesFile propFile;

	public static void main(String[] args) throws IOException {
		new Main().run(args);
	}

	public Main() throws IOException {
		textIO = TextIoFactory.getTextIO();
		terminal = textIO.getTextTerminal();

		TerminalProperties<?> props = terminal.getProperties();

		Properties props2 = new Properties();
		props2.load(getClass().getResourceAsStream("/textio.properties"));

		for (Entry<Object, Object> prop : props2.entrySet()) {
			props.put((String) prop.getKey(), prop.getValue());
		}

		try {
			propFile = new AppPropertiesFile();
		}
		catch (IOException ioe) {}
	}

	public void run(String[] args) {
		boolean bErrorsFound = false;
		boolean interactiveMode = false;

		displayApplicationInformation(terminal);

		List<String> argList = Arrays.asList(args);

		if (0 == argList.size()) {
			interactiveMode = true;
			printUsage();
			argList = readArgsFromConsole();
		}

		int fileCnt = 0;
		List<File> fileList = buildFileList(argList);

		for(File file : fileList) {
			terminal.println(String.format("Validating %s", file.toString()));
			bErrorsFound |= !CprnXmlValidator.validateFile(file);
			fileCnt++;
		}

		if (fileCnt > 0) {
			terminal.println("Processed " + fileCnt + " files.");
		} else {
			printUsage();
		}

		if (interactiveMode) {
			// We are in interactive mode so pause before exiting
			textIO
				.newStringInputReader()
				.withMinLength(0)
				.withPropertiesPrefix("info")
				.read("Press Enter to exit...");
		}

		textIO.dispose();

		System.exit(bErrorsFound ? 1 : 0);
	}

	private void printUsage() {
		if (!printedUsage) {
			terminal.println(
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
		boolean showError = false;
		boolean keepTrying = true;

		terminal.setBookmark(INPUT_FILE_PROMPT_BOOKMARK);

		while (keepTrying) {
			String[] args = {};

			if (showError) {
				terminal.println("Unable to access file(s) or directory.");
			}

			if (isSwingTerminal()) {
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
			}
			else {
				String inputLine = textIO.newStringInputReader()
						.withMinLength(0)	// use 0 length so we can make use of our bookmark
						.read("Enter files and/or folders to validate:");
				args = inputLine.split(" ");
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
				terminal.println(String.format("The specified file (%s) doees not exist", arg));
			}
		}

		return fileList;
	}

	/**
	 * Display the application information.
	 */
	private void displayApplicationInformation(TextTerminal<?> terminal) {
		try (InputStream inStrm = Main.class.getResourceAsStream("/version.txt")) {
			if (null != inStrm) {
				String appStr = new String(inStrm.readAllBytes(), StandardCharsets.UTF_8);

				terminal.println(appStr);
			}
		} catch (IOException e) {
		}
	}

	private boolean isSwingTerminal() {
		return (terminal instanceof SwingTextTerminal);
	}
}
