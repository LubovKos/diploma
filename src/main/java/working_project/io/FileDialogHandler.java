package working_project.io;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

public class FileDialogHandler {
    public File openFileDialog() {
        FileDialog fileDialog = new FileDialog((Frame) null, "Select a model file", FileDialog.LOAD);
        fileDialog.setFilenameFilter((dir, name) -> {
            String lowercaseName = name.toLowerCase();
            return lowercaseName.endsWith(".stl") || lowercaseName.endsWith(".ply") || lowercaseName.endsWith(".obj");
        });
        fileDialog.setVisible(true);
        String file = fileDialog.getFile();
        if (file != null) {
            return new File(fileDialog.getDirectory() + file);
        }
        return null;
    }

    public File saveFileDialog() {
        FileDialog fileDialog = new FileDialog((Frame) null, "Сохранить модель", FileDialog.SAVE);
        fileDialog.setFilenameFilter((dir, name) -> {
            String lowercaseName = name.toLowerCase();
            return lowercaseName.endsWith(".obj") || lowercaseName.endsWith(".stl") || lowercaseName.endsWith(".ply");
        });
        fileDialog.setVisible(true);
        String file = fileDialog.getFile();
        if (file != null) {
            return new File(fileDialog.getDirectory() + file);
        }
        return null;
    }
}