@echo off
set NOTE_PAD_HOME=%~dp0..
set JAVA_OPTS=-Dnote_pad.cmd=%1 -Dnote_pad.dir=%2 -Dfile.encoding=UTF-8
scala -nc -classpath "%NOTE_PAD_HOME%\libs\scas.jar";"%NOTE_PAD_HOME%\libs\txt2xhtml.jar";"%SQLITE_HOME%\sqlite-jdbc-3.7.2.jar" "%NOTE_PAD_HOME%\conduit\note_pad.scala"
