@echo off
set NOTE_PAD_HOME=%~dp0..
scala -Dnote_pad.cmd=%1 -Dnote_pad.dir=%2 -classpath "%SCAS_HOME%"\target\scala-2.11\scas_2.11-2.1.jar;"%SCAS_HOME%"\lib\txt2xhtml.jar;"%SQLITE_HOME%"\sqlite-jdbc-3.7.2.jar "%NOTE_PAD_HOME%"\conduit\note_pad.scala
