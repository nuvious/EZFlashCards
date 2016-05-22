@echo off
REM This batch file uses JarJar (http://code.google.com/p/jarjar/) to convert StAX and other XML libraries
REM so they are compatible with Android.

REM Change any updated library versions in below variables
set jackson-ver=2.1.2
set stax2-ver=3.1.1
set stax-ver=1.0-2
set aalto-ver=0.9.8

REM Delete existing files
del output\*-android.jar

REM Generate Android-compatible XML library JARs
echo Generating Android-compatible XML library JARs...
java -jar jarjar-1.3.jar process rules.txt jackson-dataformat-xml-%jackson-ver%.jar output\jackson-dataformat-xml-android-%jackson-ver%.jar
java -jar jarjar-1.3.jar process rules.txt stax2-api-%stax2-ver%.jar output\stax2-api-android-%stax2-ver%.jar
java -jar jarjar-1.3.jar process rules.txt stax-api-%stax-ver%.jar output\stax-api-android-%stax-ver%.jar
java -jar jarjar-1.3.jar process rules.txt aalto-xml-%aalto-ver%.jar output\aalto-xml-android-%aalto-ver%.jar

REM Install these artifacts in your local Maven repository - check for error after each command
REM NOTE - Depending on whether each version is a SNAPSHOT or RELEASE, 
REM make sure its being deployed to the correct directory by changing the last path parameter in each line below!!!!

echo Installing Android-compatible XML library JARs in Maven repo at 'Git Projects\cutr-mvn-repo'...
call mvn install:install-file -DgroupId=edu.usf.cutr.android.xml -DartifactId=jackson-dataformat-xml-android -Dversion=%jackson-ver% -Dfile=output\jackson-dataformat-xml-android-%jackson-ver%.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath="/Git Projects/cutr-mvn-repo/releases"
if not "%ERRORLEVEL%" == "0" exit /b
call mvn install:install-file -DgroupId=edu.usf.cutr.android.xml -DartifactId=stax2-api-android -Dversion=%stax2-ver% -Dfile=output\stax2-api-android-%stax2-ver%.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath="/Git Projects/cutr-mvn-repo/releases"
if not "%ERRORLEVEL%" == "0" exit /b
call mvn install:install-file -DgroupId=edu.usf.cutr.android.xml -DartifactId=stax-api-android -Dversion=%stax-ver% -Dfile=output\stax-api-android-%stax-ver%.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath="/Git Projects/cutr-mvn-repo/releases"
if not "%ERRORLEVEL%" == "0" exit /b
call mvn install:install-file -DgroupId=edu.usf.cutr.android.xml -DartifactId=aalto-xml-android -Dversion=%aalto-ver% -Dfile=output\aalto-xml-android-%aalto-ver%.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath="/Git Projects/cutr-mvn-repo/releases"

echo ----------------------------
echo Done!  Check the /output directory for your Android-compatible XML library JARs, and a local maven respository at '\Git Projects\cutr-mvn-repo' for the artifacts.
echo Artifacts can be updated in the main CUTR Maven repository by pushing contents of cutr-mvn-repo folder to this Github project:
echo https://github.com/CUTR-at-USF/cutr-mvn-repo
echo ----------------------------
pause