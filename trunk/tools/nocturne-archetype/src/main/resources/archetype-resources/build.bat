@echo off

IF "%1"=="" (
    echo mvn package
    mvn package
) ELSE (
    echo mvn *1
    mvn *%
)
