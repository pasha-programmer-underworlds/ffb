package com.fumbbl.ffb.client;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import static com.fumbbl.ffb.ClientMode.NO_COACH_NO_CONNECTION;
import static com.fumbbl.ffb.CommonProperty.LAST_LOADED_JNLP_FILE_FOLDER;
import static com.fumbbl.ffb.client.ClientParameters.Build.PROGRAMMER_UNDERWORLDS;
import static com.fumbbl.ffb.client.ClientParameters._ARGUMENT_BUILD;
import static com.fumbbl.ffb.client.util.JnlpToStringArrayParser.parseJnlpArguments;

public class LoadJnlpFileWindow extends JFrame {

    private final RunClientWithArguments runClientLambda;
    private final Map<String, Font> originalFontConfigurations;
    private final String[] fileChooserDialogKeysForFonts;
    Font font = new Font("SansSerif", Font.PLAIN, 18);

    public LoadJnlpFileWindow(RunClientWithArguments runClientLambda) {
        this.runClientLambda = runClientLambda;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setResizable(false);
        //Centering
        setLocationRelativeTo(null);
        setTitle("Fantasy Football");

        fileChooserDialogKeysForFonts = new String[]{
                "FileChooser.font",
                "FileChooser.listFont",
                "FileChooser.labelFont",
                "FileChooser.titleFont",
                "Button.font",           // Used for Open/Cancel buttons
                "ToggleButton.font",     // Used for some sidebar toggle elements
                "Label.font",            // Used for various labels
                "TextField.font",         // Used for the file name input box
                "ComboBox.font"
        };

        originalFontConfigurations = new HashMap<>();

        for (String prop : fileChooserDialogKeysForFonts)
            originalFontConfigurations.put(prop, (Font) UIManager.get(prop));

        // Create the button
        JButton button = createOpenJnlpButton();
        button.setFont(font);

        // Add the button to the window
        getContentPane().add(button);
        // Show the window
    }

    private JButton createOpenJnlpButton() {
//        FontConfig fc = fontConfigRegistry.getConfig(dimensionProvider.getLayoutSettings().getLayout());
//        Font font = fontCache.font(BOLD, fc.getSize(LARGE), dimensionProvider);
        // 1. Create a direct JButton
        JButton startGameUsingJnlpFile = new JButton("Start game/spectate using JNLP file");
//        startGameUsingJnlpFile.setFont(font);

        startGameUsingJnlpFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setUiManagerPropertiesForFileChooserFontsBeforeShowingFileChooser();

	            Preferences localyStoredPreferences = FantasyFootballClientAwt.getLocalyStoredPreferences(NO_COACH_NO_CONNECTION.getName());
	            final String lastLoadedJnlpFileFolderKey = LAST_LOADED_JNLP_FILE_FOLDER.getKey();
	            String lastPath = localyStoredPreferences.get(lastLoadedJnlpFileFolderKey, null);

	            JFileChooser fileChooser = new JFileChooser();
	            // 2. Set directory safely
	            if (lastPath != null) {
		            File lastDir = new File(lastPath);
		            // Check if it exists and is actually a directory
		            if (lastDir.exists() && lastDir.isDirectory()) {
			            fileChooser.setCurrentDirectory(lastDir);
		            } else {
			            localyStoredPreferences.remove(lastLoadedJnlpFileFolderKey);
		            }
	            }

                fileChooser.setPreferredSize(new Dimension(800, 600));

                // 1. Create a filter specifically for .jnlp files
                // The first argument is the description shown to the user, the second is the extension
                FileNameExtensionFilter jnlpFilter = new FileNameExtensionFilter("JNLP Files (*.jnlp)", "jnlp");

                // 2. Apply the filter to the file chooser
                fileChooser.setFileFilter(jnlpFilter);
                // 3. Optional: Disable the "All Files" option so they can ONLY see JNLP files
                fileChooser.setAcceptAllFileFilterUsed(false);

                // 4. Show the dialog
                int response = fileChooser.showOpenDialog(LoadJnlpFileWindow.this);

                if (response == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();

	                File directoryContainingJnlpFile = selectedFile.getParentFile();
	                //Memorizing folder from which we loaded jnlp file last time.
	                if (directoryContainingJnlpFile != null
			                && directoryContainingJnlpFile.isDirectory()) {
		                localyStoredPreferences.put(lastLoadedJnlpFileFolderKey, directoryContainingJnlpFile.getAbsolutePath());
	                }


	                java.util.List<String> clientArgumentsFromJnlpFile = parseJnlpArguments(selectedFile);

                    clientArgumentsFromJnlpFile.add(_ARGUMENT_BUILD);
                    clientArgumentsFromJnlpFile.add(PROGRAMMER_UNDERWORLDS.getName());

                    restoreUiManagerPropertiesForFileChooserFontsToOriginalAfterShowingFileChooserWasClosed();
                    //Destroying current instance of userinterface before creating a new one.
                    LoadJnlpFileWindow.this.dispose();
                    runClientLambda.setArgs(clientArgumentsFromJnlpFile.toArray(new String[]{}));
                    runClientLambda.run();
                    dispose();
                } else
                    restoreUiManagerPropertiesForFileChooserFontsToOriginalAfterShowingFileChooserWasClosed();
                //return to the fullscreen mode if client was in fullscreen mode before opening filechooser
            }
        });

        return startGameUsingJnlpFile;
    }

    public void setUiManagerPropertiesForFileChooserFontsBeforeShowingFileChooser() {
        for (String key : fileChooserDialogKeysForFonts)
            UIManager.put(key, font);
    }

    public void restoreUiManagerPropertiesForFileChooserFontsToOriginalAfterShowingFileChooserWasClosed() {
        Set<Map.Entry<String, Font>> entries = originalFontConfigurations.entrySet();
        for (Map.Entry<String, Font> entry : entries)
            UIManager.put(entry.getKey(), entry.getValue());

    }

    public abstract static class RunClientWithArguments implements Runnable {

        protected String[] args;

        public RunClientWithArguments() {
        }

        public void setArgs(String[] args) {
            this.args = args;
        }

    }

}
