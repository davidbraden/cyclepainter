set MAPLE=C:\Program Files\Maple 13


java -Djava.library.path="%MAPLE%\bin.win" -classpath "%MAPLE%\java\externalcall.jar;%MAPLE%\java\jopenmaple.jar;dist/CyclePainter.jar" cyclepainter.Main

