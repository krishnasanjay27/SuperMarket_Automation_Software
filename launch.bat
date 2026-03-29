@echo off
java -Dprism.order=sw -Dfile.encoding=UTF-8 -cp "out;lib\javafx.base.jar;lib\javafx.controls.jar;lib\javafx.fxml.jar;lib\javafx.graphics.jar;lib\javafx.media.jar;lib\mysql-connector-j-9.6.0.jar" --module-path lib --add-modules javafx.controls,javafx.fxml app.MainApp
