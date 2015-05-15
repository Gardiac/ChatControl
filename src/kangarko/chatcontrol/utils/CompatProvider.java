package kangarko.chatcontrol.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CompatProvider {

	private static Method getPlayersMethod;
	private static boolean isGetPlayersCollection = false;

	public static Collection<? extends Player> getAllPlayers() {
		return isGetPlayersCollection ? Bukkit.getOnlinePlayers() : Arrays.asList( (Player[]) getRawPlayers());
	}

	private static Object getRawPlayers() {
		try {
			return getPlayersMethod.invoke(null);

		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Reflection malfunction", ex);
		}
	}

	static {
		try {
			Class.forName("org.bukkit.Sound"); // test for too old craftbukkits			
			getPlayersMethod = Bukkit.class.getMethod("getOnlinePlayers");

			if (getPlayersMethod.getReturnType() == Collection.class)
				isGetPlayersCollection = true;			

		} catch (ReflectiveOperationException ex) {
			throw new UnsupportedServerException();
		}

		Common.Debug("&7[Reflection] &bUsing " + (isGetPlayersCollection ? "&anew" : "&cold") + " &bgetter for players");
	}

	public static class UnsupportedServerException extends RuntimeException {
		private static final long serialVersionUID = 1L;

	}
}