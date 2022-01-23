package org.cprn.validator.app;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
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

	public Main() {
		try {
			propFile = new AppPropertiesFile();
		}
		catch (IOException ioe) {}
	}

	/**
	 * Run the validation.
	 * 
	 * @param args An array of arguments from the command line if there were
	 * 		any.
	 */
	public void run(String[] args) {
		boolean bErrorsFound = false;
		boolean interactiveMode = false;

		List<String> fileFolderList = Arrays.asList(args);

		// if there were not command line arguments assume we're running in
		// interactive mode as we'll need to prompt for what to validate.
		if (0 == fileFolderList.size()) {
			interactiveMode = true;

			if (!isHeadless()) {
				// If we're not headless, use the Swing terminal
				System.setProperty(TextIoFactory.TEXT_TERMINAL_CLASS_PROPERTY, "org.beryx.textio.swing.SwingTextTerminal");
			}
		}
		else {
			System.setProperty(TextIoFactory.TEXT_TERMINAL_CLASS_PROPERTY, "org.beryx.textio.jline.JLineTextTerminal");
		}

		// Now that we've determined what "mode" we're running in, initialize
		// the terminal.
		initializeTerminal();

		displayApplicationInformation(terminal);

		if (interactiveMode) {
			printUsage();
			fileFolderList = readArgsFromConsole();
		}

		List<File> fileList = buildFileList(fileFolderList);

		if (0 == fileList.size()) {
			printUsage();
		}
		else {
			for(File file : fileList) {
				terminal.println(String.format("Validating %s", file.toString()));
				bErrorsFound |= !CprnXmlValidator.validateFile(file);
			}
			terminal.println("Processed " + fileList.size() + " files.");
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

	/**
	 * Initialize the terminal we'll be using.
	 * <p>
	 * To work around the issue that Text-IO does not support reading the
	 * property file within a module we manually load our properties into the
	 * terminal.
	 * <p>
	 * Similarly,Text-IO cannot load the icon from within our module.
	 */
	private void initializeTerminal() {
		textIO = TextIoFactory.getTextIO();
		terminal = textIO.getTextTerminal();

		try {
			TerminalProperties<?> props = terminal.getProperties();
	
			Properties props2 = new Properties();
			props2.load(getClass().getResourceAsStream("/textio.properties"));
	
			for (Entry<Object, Object> prop : props2.entrySet()) {
				props.put((String) prop.getKey(), prop.getValue());
			}
		}
		catch (IOException ioe) {
			
		}
	}

	/**
	 * Print the application usage.
	 */
	private void printUsage() {
		if (!printedUsage) {
			terminal.println(
					new StringBuilder()
					.append("Usage:").append(System.lineSeparator())
					.append("\tInput files and/or folders can be provided on the command line or entered when prompted.").append(System.lineSeparator())
					.append("\tProviding multiple files and/or folders is allowed.").append(System.lineSeparator())
					.append(System.lineSeparator())
					.append("\tE.g. CprnXmlValidator <input.xml> OR <folder containing XML files>").append(System.lineSeparator())
					.toString());
			printedUsage = true;
		}
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

	/**
	 * Build the list of files to be validated.
	 * 
	 * @param argList The potential files/folder provided via the command
	 * 			line, from the terminal prompt or the JFileChoooser.
	 *		
	 * @return A list of files to validated.
	 */
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

	/**
	 * Check if the terminal being used is a Swing terminal.
	 * 
	 * @return Returns true if it is a Swing terminal, otherwise returns false.
	 */
	private boolean isSwingTerminal() {
		return (terminal instanceof SwingTextTerminal);
	}

	/**
	 * Check if we're running in a headless environment. 
	 * 
	 * @return Returns true if the environment is headless, otherwise returns false.
	 */
    private static boolean isHeadless() {
        if (GraphicsEnvironment.isHeadless()) return true;
        try {
            GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            return screenDevices == null || screenDevices.length == 0;
        } catch (HeadlessException e) {
            return true;
        }
    }
}
