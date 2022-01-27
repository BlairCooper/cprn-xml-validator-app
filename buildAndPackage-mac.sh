#!/bin/bash

# This script is for local testing of the packaging process and is not use for the actual build process.

mvn clean package

jpackage --verbose --app-version 99.0.1 "@jpackage/jpackage.cfg" "@jpackage/jpackage-mac-image.cfg" 
