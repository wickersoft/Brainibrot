C:\mingw64\bin\gcc --fast-math -o braini_brot.exe braini_brot.c
braini_brot.exe
IF %ERRORLEVEL% NEQ 0 (
	echo "rrreeeeeeee"
    goto :stop
)

java -Xmx32G BinToPNG.java 73250 29300

:stop
