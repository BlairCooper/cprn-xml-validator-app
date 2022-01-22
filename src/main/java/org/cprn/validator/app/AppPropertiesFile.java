package org.cprn.validator.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.cprn.utils.PropertiesFile;

public class AppPropertiesFile
	extends PropertiesFile
{
	private static final long serialVersionUID = -8800512435305840019L;

	public AppPropertiesFile() throws IOException {
		super(getPropertyFilePath());
	}

	private static Path getPropertyFilePath() {
		Path userPropertyFilePath = Paths.get(System.getProperty("user.home")).resolve(".cprn-xml-validator").resolve("CprnXmlValidator.properties");

		if (!Files.exists(userPropertyFilePath) ) {
			try {
				Files.createDirectories(userPropertyFilePath.getParent());
				Files.createFile(userPropertyFilePath);
			} catch (IOException e) {
				userPropertyFilePath = null;
			}
		}

		return userPropertyFilePath;
	}
}
