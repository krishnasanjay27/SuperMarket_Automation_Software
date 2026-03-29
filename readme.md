# Supermarket Automation System (SAS)

## Database Setup
1. Start your MySQL Server.
2. Create the database and import the schema by running this command in your terminal:
```bash
mysql -u root -p sas_db < sas_schema.sql
```

## How to Run GUI Application (Recommended)

The easiest way to compile and run the application is to double-click the script from Windows Explorer or run it in a regular command prompt:

```bat
run_gui.bat
```
> **Note:** Do NOT run this from an IDE terminal (like VS Code or IntelliJ) because they are headless environments and will cause a `Graphics Device initialization failed` error. Run it from Windows Explorer or a standard Command Prompt.

---

## Manual Compile and Run Commands

If you prefer to run the commands manually from the project root directory (`software\`), here are the required steps:

### 1. Compile the Source Code
```bat
for /R src %f in (*.java) do @echo %f >> .build_sources.txt
javac -encoding UTF-8 -cp "lib\javafx.base.jar;lib\javafx.controls.jar;lib\javafx.fxml.jar;lib\javafx.graphics.jar;lib\javafx.media.jar;lib\mysql-connector-j-9.6.0.jar" -d out -sourcepath src @.build_sources.txt
del .build_sources.txt
```

### 2. Copy FXML/CSS Resources
```bat
xcopy /E /I /Y "src\app\views" "out\app\views"
```

### 3. Launch the Application
Setting the `PATH` is critical so that JavaFX can load its native DLLs (e.g., `glass.dll`, `prism_d3d.dll`) correctly.
```bat
set "LIBDIR=%CD%\lib"
set "PATH=%LIBDIR%;%PATH%"

java ^
  -Djava.library.path="%LIBDIR%" ^
  -Dprism.order=d3d,sw ^
  -Dfile.encoding=UTF-8 ^
  --module-path "%LIBDIR%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "out;%LIBDIR%\javafx.base.jar;%LIBDIR%\javafx.controls.jar;%LIBDIR%\javafx.fxml.jar;%LIBDIR%\javafx.graphics.jar;%LIBDIR%\javafx.media.jar;%LIBDIR%\mysql-connector-j-9.6.0.jar" ^
  app.MainApp
```

---

## CLI Test Harness
If you want to run the background tests via the command-line without the GUI (make sure you compiled the project first):

```bat
java -cp "lib\mysql-connector-j-9.6.0.jar;out" app.Main
```
