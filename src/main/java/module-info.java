module cprn.xml.validator.app {
	requires java.desktop;			// Needed for JFileChooser
    requires java.logging;
	requires java.xml;
	requires jdk.crypto.cryptoki;	// Needed for SSL support, fetching XSDs from server
	requires com.fasterxml.jackson.core;	// Needed for version checking
	requires org.beryx.textio;
	requires cprn.xml.validator;
	requires cprn.java.library;
}
