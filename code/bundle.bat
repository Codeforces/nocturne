setlocal enabledelayedexpansion

if not "!JAVA17_64_HOME!"=="" (
    set PATH=!JAVA17_64_HOME!\bin;!PATH!
    set JAVA_HOME=!JAVA17_64_HOME!
)

call mvn.cmd validate --batch-mode
call mvn.cmd -Dfile.encoding=UTF-8 -DcreateChecksum=true clean source:jar javadoc:jar repository:bundle-create install --batch-mode
