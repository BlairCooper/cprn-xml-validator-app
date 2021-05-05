module cprn.xml.validator.app {
    requires java.logging;
	requires java.xml;
	requires jdk.crypto.cryptoki;	// Needed for SSL support, fetching XSDs from server
	requires cprn.xml.validator;
}