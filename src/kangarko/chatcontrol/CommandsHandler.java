package kangarko.chatcontrol;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import kangarko.chatcontrol.config.ConfHelper;
import kangarko.chatcontrol.config.ConfHelper.ChatMessage;
import kangarko.chatcontrol.config.ConfHelper.GroupSpecificHelper;
import kangarko.chatcontrol.config.Localization;
import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.hooks.HookManager;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.CompatProvider;
import kangarko.chatcontrol.utils.Permissions;

@SuppressWarnings("deprecation")
public class CommandsHandler implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		try {
			handleCommand(sender, args);

		} catch (MissingPermissionException ex) {
			Common.tell(sender, ex.getMessage());

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return true;
	}

	private void handleCommand(CommandSender sender, String[] args) {
		if (args.length == 0) {
			Common.tell(sender,
					"&8-----------------------------------------------------|",
					"&3ChatControl &8// &fRunning &7v" + ChatControl.instance().getDescription().getVersion(),
					"&3ChatControl &8// &fBy &7kangarko &f© 2013 - 2015",
					(!HookManager.isRushCoreLoaded() && Bukkit.getIp().startsWith("93.91") ? "&3ChatControl &8// &fNavstivte: &7http://rushmine.6f.sk" + (new Random().nextInt(5) == 1 ? " &b< Prid si zahrat!" : "") : ""));
			return;
		}

		String argument = args[0].toLowerCase();
		String param = (args.length > 1 && args[1].startsWith("-") ? args[1] : "").toLowerCase();
		String reason = "";

		for (int i = param.isEmpty() ? 1 : 2; i < args.length; i++)
			reason += (reason.isEmpty() ? "" : " ") + args[i];

		/**
		 * MUTE COMMAND
		 */
		if ("mute".equals(argument) || "m".equals(argument)) {
			checkPerm(sender, Permissions.Commands.MUTE);

			if (param.isEmpty())
				Common.broadcastIfEnabled(Settings.Mute.BROADCAST, sender, ChatControl.muted ? Localization.MUTE_UNMUTE_BROADCAST : Localization.MUTE_BROADCAST, reason);
			else if ((param.equals("-silent") || param.equals("-s")) && Common.hasPerm(sender, Permissions.Commands.MUTE_SILENT)) {
				// do nothing
			} else if ((param.equals("-anonymous") || param.equals("-a")) && Common.hasPerm(sender, Permissions.Commands.MUTE_ANONYMOUS))
				Common.broadcastIfEnabled(Settings.Mute.BROADCAST, sender, ChatControl.muted ? Localization.MUTE_ANON_UNMUTE_BROADCAST : Localization.MUTE_ANON_BROADCAST, reason);
			else if (param.startsWith("-")) {
				Common.tell(sender, Localization.WRONG_PARAMETERS);
				return;
			}

			Common.tell(sender, ChatControl.muted ? Localization.MUTE_UNMUTE_SUCCESS : Localization.MUTE_SUCCESS);
			ChatControl.muted = !ChatControl.muted;
		}

		/**
		 * CLEAR COMMAND
		 */
		else if ("clear".equals(argument) || "c".equals(argument)) {
			checkPerm(sender, Permissions.Commands.CLEAR);

			if ((param.equals("-console") || param.equals("-c"))
					&& Common.hasPerm(sender, Permissions.Commands.CLEAR_CONSOLE)) {
				for (int i = 0; i < Settings.Clear.CONSOLE_LINES; i++)
					System.out.println("           ");

				if (sender instanceof Player)
					Common.Log(Localization.CLEAR_CONSOLE_MSG.replace("%player", sender.getName()));

				Common.tell(sender, Localization.CLEAR_CONSOLE);
				return;
			}

			final String Reason = reason;
			final CommandSender Sender = sender;
			if (param.isEmpty()) {
				// Workaround; delay the message so it's displayed after blank lines.
				new BukkitRunnable() {
					@Override
					public void run() {
						Common.broadcastIfEnabled(Settings.Clear.BROADCAST, Sender, Localization.CLEAR_BROADCAST, Reason);
					}
				}.runTaskLater(ChatControl.instance(), 2);
			} else if ((param.equals("-silent") || param.equals("-s")) && Common.hasPerm(sender, Permissions.Commands.CLEAR_SILENT)) {
				// broadcast nothing
			} else if ((param.equals("-anonymous") || param.equals("-a")) && Common.hasPerm(sender, Permissions.Commands.CLEAR_ANONYMOUS)) {
				new BukkitRunnable() {
					@Override
					public void run() {
						Common.broadcastIfEnabled(Settings.Clear.BROADCAST, Sender, Localization.CLEAR_ANON_BROADCAST, Reason);
					}
				}.runTaskLater(ChatControl.instance(), 2);
			} else if (param.startsWith("-")) {
				Common.tell(sender, Localization.WRONG_PARAMETERS);
				return;
			}

			for (Player pl : CompatProvider.getAllPlayers()) {
				if (Settings.Clear.IGNORE_STAFF && Common.hasPerm(pl, Permissions.Bypasses.CHAT_CLEARING)) {
					Common.tell(pl, Localization.CLEAR_STAFF, sender.getName());
					continue;
				}
				for (int i = 0; i < 120; i++)
					pl.sendMessage(ChatColor.RESET + "      ");
			}
		}

		/**
		 * FAKE COMMAND
		 */
		else if ("fake".equals(argument) || "f".equals(argument)) {
			checkPerm(sender, Permissions.Commands.FAKE);

			if (args.length < 2 || args.length > 3) {
				Common.tell(sender, Localization.USAGE_FAKE_CMD);
				return;
			}

			param = args[1].toLowerCase();
			String fakePlayer = args.length == 3 ? Common.colorize(args[2]) : sender.getName();

			Player onlineFakePlayer = Bukkit.getPlayer(fakePlayer);
			PlayerCache fakePlayerData = onlineFakePlayer != null && onlineFakePlayer.isOnline() ? ChatControl.getDataFor(onlineFakePlayer) : null;

			ChatMessage fakeMessage;
			GroupSpecificHelper<ChatMessage> messageHelper;

			if (param.equals("join") || param.equals("j")) {
				messageHelper = Settings.Messages.JOIN;				
				fakeMessage = fakePlayerData != null ? messageHelper.getFor(fakePlayerData) : messageHelper.getDefault();

				if (fakeMessage.getType() == ChatMessage.Type.DEFAULT)
					Common.broadcast(ChatColor.YELLOW + fakePlayer + ChatColor.YELLOW + " joined the game.");

				else if (fakeMessage.getType() == ChatMessage.Type.HIDDEN)
					Common.tell(sender, Localization.CANNOT_BROADCAST_EMPTY_MESSAGE.replace("%event", Localization.Parts.JOIN));

				else
					Common.broadcastWithPlayer(replacePlayerVariables(fakeMessage.getMessage(), onlineFakePlayer), fakePlayer);

			} else if (param.equals("quit") || param.equals("q") || param.equals("leave") || param.equals("l")) {
				messageHelper = Settings.Messages.QUIT;				
				fakeMessage = fakePlayerData != null ? messageHelper.getFor(fakePlayerData) : messageHelper.getDefault();

				if (fakeMessage.getType() == ChatMessage.Type.DEFAULT)
					Common.broadcast(ChatColor.YELLOW + fakePlayer + ChatColor.YELLOW + " left the game.");

				else if (fakeMessage.getType() == ChatMessage.Type.HIDDEN)
					Common.tell(sender, Localization.CANNOT_BROADCAST_EMPTY_MESSAGE.replace("%event", Localization.Parts.QUIT));

				else
					Common.broadcastWithPlayer(replacePlayerVariables(fakeMessage.getMessage(), onlineFakePlayer), fakePlayer);

			} else if (param.equals("kick") || param.equals("k")) {
				messageHelper = Settings.Messages.KICK;				
				fakeMessage = fakePlayerData != null ? messageHelper.getFor(fakePlayerData) : messageHelper.getDefault();

				if (fakeMessage.getType() == ChatMessage.Type.DEFAULT)
					Common.broadcast(ChatColor.YELLOW + fakePlayer + ChatColor.YELLOW + " left the game.");

				else if (fakeMessage.getType() == ChatMessage.Type.HIDDEN)
					Common.tell(sender, Localization.CANNOT_BROADCAST_EMPTY_MESSAGE.replace("%event", Localization.Parts.QUIT));

				else
					Common.broadcastWithPlayer(replacePlayerVariables(fakeMessage.getMessage(), onlineFakePlayer), fakePlayer);

			} else
				Common.tell(sender, Localization.USAGE_FAKE_CMD);

		}

		/**
		 * RELOAD COMMAND
		 */
		else if ("reload".equals(argument) || "znovunacitat".equals(argument) || "r".equals(argument)) {
			checkPerm(sender, Permissions.Commands.RELOAD);

			ChatControl instance = ChatControl.instance();
			try {
				ConfHelper.loadAll();				
				instance.onReload();
			} catch (Throwable t) {
				t.printStackTrace();
				Common.tell(sender, Localization.RELOAD_FAILED.replace("%error", t.getMessage()));
				return;
			}

			Common.tell(sender, Localization.RELOAD_COMPLETE);
		}

		/**
		 * LIST COMMAND
		 */
		else if ("commands".equals(argument) || "?".equals(argument) || "list".equals(argument) || "help".equals(argument)) {
			checkPerm(sender, Permissions.Commands.LIST);

			Common.tell(sender,
					" ",
					"&3  ChatControl &f(v" + ChatControl.instance().getDescription().getVersion() + ")",
					"&2  [] &f= optional arguments (use only 1 at once)",
					"&6  <> &f= required arguments",
					" ",
					"  &f/chc mute &9[-silent] [-anonymous] &2[reason] &e- Chat (un)mute.",
					"  &f/chc clear &9[-s] [-a] [-console] &2[reason] &e- Chat clear.",
					"  &f/chc fake &6<join/leave> &2[name] &e- Fake join/quit messages.",
					"  &f/chc reload &e- Reload configuration.",
					"  &f/chc list &e- Command list.");
			/*} 

		else if ("test".equals(argument)) {
			if (args.length == 1) {
				Common.tell(sender, "Usage: /chc test <processEngine>");
				return;
			}

			if (sender.isOp()) {
				try {
					if (reason.equals("1"))
						reason = "get -> org.bukkit.Bukkit|getServicesManager()|getRegistration(net.milkbowl.vault.chat.Chat.class)|getProvider()|getPlayerPrefix(\"world\",\"kangarko\")";

					LagCatcher.start("test");
					Common.tell(sender, "Method returned: " + ProcessingEngine.process(reason, sender));
					LagCatcher.end("test", 0);
				} catch (Exception ex) {
					ex.printStackTrace();
					Common.tell(sender, ChatColor.RED + ex.getClass().getSimpleName() + ": " + ex.getMessage());
				}
			}*/

		} else
			Common.tell(sender, Localization.WRONG_ARGUMENTS);
	}

	private void checkPerm(CommandSender sender, String perm) {
		if (sender instanceof Player && !Common.hasPerm(sender, perm))
			throw new MissingPermissionException(perm);
	}

	public String replacePlayerVariables(String msg, Player pl) {
		if (pl != null && pl.isOnline()) {
			msg = msg.replace("%player", pl.getName());

			if (ChatControl.instance().formatter != null)
				msg = ChatControl.instance().formatter.replacePlayerVariables(pl, msg);
		}
		return Common.colorize(msg);
	}
}

class MissingPermissionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public MissingPermissionException(String perm) {
		super(Localization.NO_PERMISSION.replace("%perm", perm));
	}
}
