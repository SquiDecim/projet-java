package io.github.squidecim.genialtcg.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.squidecim.genialtcg.GenialTCG;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(
            new GenialTCG(),
            getDefaultConfiguration()
        );
    }

    private static String loadSavedDisplayMode() {
        File prefsFile = new File(System.getProperty("user.home"), ".prefs/GenialTCG_Settings");
        if (!prefsFile.exists()) return "Plein ecran";
        try (FileInputStream fis = new FileInputStream(prefsFile)) {
            Properties props = new Properties();
            props.loadFromXML(fis);
            return props.getProperty("display_mode", "Plein ecran");
        } catch (Exception e) {
            return "Plein ecran";
        }
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration =
            new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("GenialTCG");
        configuration.useVsync(true);
        configuration.setForegroundFPS(
            Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1
        );

        String displayMode = loadSavedDisplayMode();
        if ("Plein ecran".equals(displayMode)) {
            configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else if ("Fenetre sans bordure".equals(displayMode)) {
            configuration.setDecorated(false);
            configuration.setWindowedMode(
                Lwjgl3ApplicationConfiguration.getDisplayMode().width,
                Lwjgl3ApplicationConfiguration.getDisplayMode().height
            );
        } else {
            configuration.setDecorated(true);
            configuration.setWindowedMode(1280, 720);
        }

        configuration.setWindowIcon(
            "libgdx128.png",
            "libgdx64.png",
            "libgdx32.png",
            "libgdx16.png"
        );

        return configuration;
    }
}
