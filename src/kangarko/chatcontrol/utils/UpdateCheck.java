package kangarko.chatcontrol.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import kangarko.chatcontrol.ChatControl;
import kangarko.chatcontrol.config.Localization;
import kangarko.chatcontrol.config.Settings;

public class UpdateCheck implements Runnable {

	public static boolean needsUpdate = false;
	public static String newVersion;

	// A file with version info
	private final String versionInfoUrl;
	
	// A folder with precompiled files.
	private final String filesFolderUrl;
	
	public UpdateCheck() {
		this.versionInfoUrl = "https://raw.github.com/kangarko/ChatControl/master/plugin.yml";
		this.filesFolderUrl = "https://raw.githubusercontent.com/kangarko/ChatControl/master/precompiled/";
	}

	@Override
	public void run() {
		String oldversion = ChatControl.instance().getDescription().getVersion();

		if (oldversion.contains("SNAPSHOT") || oldversion.contains("DEV"))
			return;

		try {
			InputStream is = new URL(versionInfoUrl).openConnection().getInputStream();
			YamlConfiguration conf = CompatProvider.loadConfiguration(is);
			
			String newversion = conf.getString("version");

			if (newversion.contains("SNAPSHOT") || newversion.contains("DEV"))
				return;

			if (toNumber(newversion) > toNumber(oldversion))
				if (Settings.Updater.DOWNLOAD) {
					URL adresa = null;

					try {
						Common.Log("&bChatControl is updating! Downloading v" + newversion);

						adresa = new URL(filesFolderUrl + "ChatControl_v" + newversion + ".jar");

						Common.Log("Got file of size: " + (double) adresa.openConnection().getContentLengthLong() / 1000 + " kb");

						File file = new File(Bukkit.getUpdateFolder(), "ChatControl.jar");

						if (!file.exists())
							file.mkdirs();

						Files.copy(adresa.openStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

						Common.Log("Downloaded! File uploaded into the " + Bukkit.getUpdateFolder() + " folder. Please copy it to the plugins folder.");
					} catch (FileNotFoundException ex) {
						Common.Warn("Cannot download file from " + adresa.toString() + " (Malformed URL / file not uploaded yet)");
					} catch (IOException ex) {
						Common.Warn("Cannot download file from " + adresa.toString() + " (check console for error)");
						ex.printStackTrace();
					}
				} else {
					needsUpdate = true;
					newVersion = newversion;

					Common.Log(Localization.UPDATE_AVAILABLE.replace("%current", oldversion).replace("%new", newversion));
				}

		} catch (UnknownHostException | MalformedURLException ex) {
			Common.Warn("Update check failed, could not connect to: " + versionInfoUrl);

			if (Settings.DEBUG)
				ex.printStackTrace();
		} catch (NumberFormatException ex) {
			Common.Warn("Update check failed, malformed version string: " + ex.getMessage());
		} catch (IOException ex) {
			if (ex.getMessage().equals("Permission denied: connect"))
				Common.Warn("Unable to connect to the update site, check your internet/firewall.");
			else
				Common.Error("Error while checking for update from: " + versionInfoUrl, ex);
		}
	}

	private int toNumber(String s) {
		return Integer.valueOf(s.replace(".", "").replace("-BETA", "").replace("-ALPHA", ""));
	}
}