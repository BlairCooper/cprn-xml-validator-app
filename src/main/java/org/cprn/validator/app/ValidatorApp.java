package org.cprn.validator.app;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.beryx.textio.TerminalProperties;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.cprn.validator.CprnXmlValidator;

import com.fasterxml.jackson.core.Version;


abstract class ValidatorApp {
	protected TextIO textIO;
	protected TextTerminal<?> terminal;

	protected AppPropertiesFile propFile;
	protected boolean debugSSL = false;

	protected String versionText;
	protected Version localVersion;
	protected Version serverVersion;

	private final Pattern versionRegexPattern = Pattern.compile("([1-9])\\.([1-9][0-9]?)\\.([1-9][0-9]{0,2})");

	public ValidatorApp() {
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
	public abstract void run(String[] args);

	protected void processArgs(String[] args) {
		for(String arg : args) {
			if (arg.toLowerCase().contains("-debugssl")) {
				debugSSL = true;
				System.setProperty("javax.net.debug", "all");
			}
		}

		initVersionInfo();
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
	protected void initializeTerminal() {
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
	 * Build the list of files to be validated.
	 * 
	 * @param argList The potential files/folder provided via the command
	 * 			line, from the terminal prompt or the JFileChoooser.
	 *		
	 * @return A list of files to validated.
	 */
	protected List<File> buildFileList(List<String> argList) {
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
				terminal.println(String.format("The specified file (%s) does not exist", arg));
			}
		}

		return fileList;
	}

	protected void initVersionInfo() {
		try (InputStream inStrm = ValidatorApp.class.getResourceAsStream("/version.txt")) {
			if (null != inStrm) {
				versionText = new String(inStrm.readAllBytes(), StandardCharsets.UTF_8);

				Matcher matcher = versionRegexPattern.matcher(versionText);
				if (matcher.find() && 3 == matcher.groupCount()) {
					localVersion = new Version(Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2)), Integer.valueOf(matcher.group(3)), null, null, null);

					URL url = new URL("https://cprn.org/wp-admin/admin-ajax.php?action=fetchValidationToolVersion");
					try (InputStream in = url.openStream()) {
						matcher = versionRegexPattern.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
						if (matcher.find() && 3 == matcher.groupCount()) {
							serverVersion = new Version(Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2)), Integer.valueOf(matcher.group(3)), null, null, null);
						}
					}
					catch (IOException e) {
					}
				}
			}
		} catch (IOException e) {
		}
	}

	/**
	 * Display the application information.
	 */
	protected void displayApplicationInformation(TextTerminal<?> terminal) {
		terminal.println(versionText);

		if (serverVersion.compareTo(localVersion) > 0) {
			terminal.executeWithPropertiesConfigurator(
			        props -> props.setPromptColor("yellow"),
			        t -> t.println("A newer version of the CPRN Validation Tool is avabile. Please visit https://cprn.org/research/cprn-data-collection/ to retrieve it."));
			terminal.println();
		}
	}

	/**
	 * Check if we're running in a headless environment. 
	 * 
	 * @return Returns true if the environment is headless, otherwise returns false.
	 */
    protected static boolean isHeadless() {
        if (GraphicsEnvironment.isHeadless()) return true;
        try {
            GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            return screenDevices == null || screenDevices.length == 0;
        } catch (HeadlessException e) {
            return true;
        }
    }

    /**
     * Validate a list of files.
     * 
     * @param fileList The list of files to validate
     * 
     * @return Returns true of errors were found, otherwise returns false.
     */
    protected boolean validateFiles(List<File> fileList, String versionInfo) {
		int errCnt = 0;

		for(File file : fileList) {
			terminal.println(String.format("Validating %s", file.toString()));

			if (debugSSL) {
				try {
					PrintStream printStream = new PrintStream(new FileOutputStream(file.getPath() + "-sysout.log"));
					System.setOut(printStream);
					System.setErr(printStream);
				}
				catch (FileNotFoundException e) {
				}
			}

			if (!CprnXmlValidator.validateFile(file, versionInfo)) {
				errCnt++;
			}
		}

		terminal.println(String.format("Processed %d files, of which %d had errors.", fileList.size(), errCnt));

		return 0 != errCnt;
	}
}
