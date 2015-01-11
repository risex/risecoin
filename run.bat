@ECHO OFF
IF EXIST java (
	start "RISE" java -cp rise.jar;lib\*;conf rise.Rise
) ELSE (
	IF EXIST "%PROGRAMFILES%\Java\jre7" (
		start "RISE" "%PROGRAMFILES%\Java\jre7\bin\java.exe" -cp rise.jar;lib\*;conf rise.Rise
	) ELSE (
		IF EXIST "%PROGRAMFILES(X86)%\Java\jre7" (
			start "RISE" "%PROGRAMFILES(X86)%\Java\jre7\bin\java.exe" -cp rise.jar;lib\*;conf rise.Rise
		) ELSE (
			IF EXIST "%PROGRAMFILES%\Java\jre8" (
				start "RISE" "%PROGRAMFILES%\Java\jre8\bin\java.exe" -cp rise.jar;lib\*;conf rise.Rise
			) ELSE (
				IF EXIST "%PROGRAMFILES(X86)%\Java\jre8" (
					start "RISE" "%PROGRAMFILES(X86)%\Java\jre8\bin\java.exe" -cp rise.jar;lib\*;conf rise.Rise
				) ELSE (
					ECHO Java software not found on your system. Please go to http://java.com/en/ to download a copy of Java.
					PAUSE
				)
			)
		)
	)
)

