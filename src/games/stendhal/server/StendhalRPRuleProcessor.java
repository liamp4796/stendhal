/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server;

import games.stendhal.common.Debug;
import games.stendhal.server.account.AccountCreator;
import games.stendhal.server.account.CharacterCreator;
import games.stendhal.server.actions.CommandCenter;
import games.stendhal.server.entity.Entity;
import games.stendhal.server.entity.RPEntity;
import games.stendhal.server.entity.npc.NPC;
import games.stendhal.server.entity.npc.NPCList;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.events.LoginNotifier;
import games.stendhal.server.events.TurnNotifier;
import games.stendhal.server.events.TutorialNotifier;
import games.stendhal.server.scripting.ScriptRunner;

import java.util.LinkedList;
import java.util.List;

import marauroa.common.Configuration;
import marauroa.common.Pair;
import marauroa.common.game.AccountResult;
import marauroa.common.game.CharacterResult;
import marauroa.common.game.IRPZone;
import marauroa.common.game.RPAction;
import marauroa.common.game.RPObject;
import marauroa.server.game.Statistics;
import marauroa.server.game.container.PlayerEntry;
import marauroa.server.game.container.PlayerEntryContainer;
import marauroa.server.game.db.Transaction;
import marauroa.server.game.rp.IRPRuleProcessor;
import marauroa.server.game.rp.RPServerManager;

import org.apache.log4j.Logger;

public class StendhalRPRuleProcessor implements IRPRuleProcessor {

	/** the logger instance. */
	private static final Logger logger = Logger.getLogger(StendhalRPRuleProcessor.class);

	/** The Singleton instance */
	protected static StendhalRPRuleProcessor instance;

	private StendhalPlayerDatabase database;

	private RPServerManager rpman;

	/**
	 * A list of all players who are currently logged in.
	 */
	protected List<Player> players;

	private List<Player> playersRmText;

	private List<NPC> npcs;

	private List<NPC> npcsToAdd;

	private List<NPC> npcsToRemove;

	/**
	 * A list of RPEntities that were killed in the current turn, together with
	 * the Entity that killed it.
	 */
	private List<Pair<RPEntity, Entity>> entityToKill;

	protected StendhalRPRuleProcessor() {
		players = new LinkedList<Player>();
		playersRmText = new LinkedList<Player>();
		npcs = new LinkedList<NPC>();
		npcsToAdd = new LinkedList<NPC>();
		npcsToRemove = new LinkedList<NPC>();
		entityToKill = new LinkedList<Pair<RPEntity, Entity>>();

	}

	private void init() {
		database = StendhalPlayerDatabase.getDatabase();

		instance = this;
		addGameEvent("server system", "startup");
	}

	public static StendhalRPRuleProcessor get() {
		if (instance == null) {
			instance = new StendhalRPRuleProcessor();
			instance.init();
		}
		return instance;
	}

	public void addGameEvent(String source, String event, String... params) {
		try {
			database.addGameEvent(source, event, params);
		} catch (Exception e) {
			logger.warn("Can't store game event", e);
		}
	}

	/**
	 * Gets the points of named player in the specified hall of fame
	 * 
	 * @param playername
	 *            name of the player
	 * @param fametype
	 *            type of the hall of fame
	 * @return points points to add
	 */
	public int getHallOfFamePoints(String playername, String fametype) {
		int res = 0;
		try {
			Transaction transaction = database.getTransaction();
			res = database.getHallOfFamePoints(transaction, playername,
					fametype);
			transaction.commit();
		} catch (Exception e) {
			logger.warn("Can't store game event", e);
		}
		return res;
	}

	/**
	 * Add points to the named player in the specified hall of fame
	 * 
	 * @param playername
	 *            name of the player
	 * @param fametype
	 *            type of the hall of fame
	 * @param points
	 *            points to add
	 */
	public void addHallOfFamePoints(String playername, String fametype,
			int points) {
		try {
			Transaction transaction = database.getTransaction();
			int oldPoints = database.getHallOfFamePoints(transaction,
					playername, fametype);
			int totalPoints = oldPoints + points;
			database.setHallOfFamePoints(transaction, playername, fametype,
					totalPoints);
			transaction.commit();
		} catch (Exception e) {
			logger.warn("Can't store game event", e);
		}
	}

	/**
	 * 
	 * Set the context where the actions are executed. Load/Run optional
	 * StendhalServerExtension(s) as defined in marauroa.ini file example:
	 * groovy=games.stendhal.server.scripting.StendhalGroovyRunner
	 * myservice=games.stendhal.server.MyService
	 * server_extension=groovy,myservice if no server_extension property is
	 * found, only the groovy extension is loaded to surpress loading groovy
	 * extension use server_extension= in the properties file.
	 * 
	 * @param rpman
	 */
	public void setContext(RPServerManager rpman) {
		try {
			this.rpman = rpman;
			StendhalRPAction.initialize(rpman);
			/* Initialize quests */
			StendhalQuestSystem.get().init();

			new ScriptRunner();

			Configuration config = Configuration.getConfiguration();
			try {
				String[] extensionsToLoad = config.get("server_extension").split(
						",");
				for (String element : extensionsToLoad) {
					String extension = null;
					try {
						extension = element;
						if (extension.length() > 0) {
							StendhalServerExtension.getInstance(
									config.get(extension)).init();
						}
					} catch (Exception ex) {
						logger.error("Error while loading extension: "
								+ extension, ex);
					}
				}
			} catch (Exception ep) {
				logger.info("No server extensions configured in ini file.");
			}

		} catch (Exception e) {
			logger.error("cannot set Context. exiting", e);
			System.exit(-1);
		}
	}

	public boolean checkGameVersion(String game, String version) {
		if (game.equals("stendhal")) {
			return true;
		}
		return false;
	}

	public void addNPC(NPC npc) {
		npcsToRemove.remove(npc);

		if (!npcs.contains(npc)) {
			npcsToAdd.add(npc);
		}
	}

	public void killRPEntity(RPEntity entity, Entity killer) {
		entityToKill.add(new Pair<RPEntity, Entity>(entity, killer));
	}

	/**
	 * Checks whether the given RPEntity has been killed this turn.
	 * 
	 * @param entity
	 *            The entity to check.
	 * @return true if the given entity has been killed this turn.
	 */
	private boolean wasKilled(RPEntity entity) {
		for (Pair<RPEntity, Entity> entry : entityToKill) {
			if (entity.equals(entry.first())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the entity which has killed the given RPEntity this turn.
	 * 
	 * @param entity
	 *            The killed RPEntity.
	 * @return The killer, or null if the given RPEntity hasn't been killed this
	 *         turn.
	 */
	private Entity killerOf(RPEntity entity) {
		for (Pair<RPEntity, Entity> entry : entityToKill) {
			if (entity.equals(entry.first())) {
				return entry.second();
			}
		}
		return null;
	}

	public void removePlayerText(Player player) {
		playersRmText.add(player);
	}

	/**
	 * Gets all players who are currently online.
	 * 
	 * @return A list of all online players
	 */
	public List<Player> getPlayers() {
		return players;
	}

	/**
	 * Finds an online player with a specific name.
	 * 
	 * @param name
	 *            The player's name
	 * @return The player, or null if no player with the given name is currently
	 *         online.
	 */
	public Player getPlayer(String name) {
		for (Player player : getPlayers()) {
			if (player.getTitle().equals(name)) {
				return player;
			}
		}
		return null;
	}

	public boolean removeNPC(NPC npc) {
		return npcsToRemove.add(npc);
	}

	public boolean onActionAdd(RPObject caster, RPAction action,
			List<RPAction> actionList) {
		return true;
	}

	public void execute(RPObject caster, RPAction action) {
		CommandCenter.execute(caster, action);
	}

	public int getTurn() {
		return rpman.getTurn();
	}

	/** Notify it when a new turn happens */
	public synchronized void beginTurn() {
		long start = System.nanoTime();

		debugOutput();

		try {
			// We keep the number of players logged.
			Statistics.getStatistics().set("Players logged", players.size());

			/*
			 * TODO: Refactor. Entities should care about really dying
			 * themselves. This is here because there is a split between last
			 * hit and the moment a entity die so that the last hit is visible
			 * on client.
			 */
			// In order for the last hit to be visible dead happens at two
			// steps.
			for (Pair<RPEntity, Entity> entry : entityToKill) {
				try {
					entry.first().onDead(entry.second());
				} catch (Exception e) {
					logger.error("Player has logout before dead", e);
				}
			}
			entityToKill.clear();

			/*
			 * TODO: Refactor NPC should be stored on zones instead of
			 * duplicating that info.
			 */
			// Done this way because a problem with comodification... :(
			npcs.removeAll(npcsToRemove);
			npcs.addAll(npcsToAdd);
			npcsToAdd.clear();
			npcsToRemove.clear();

			/*
			 * TODO: Refactor Use RPEvent that is the correct way and it is
			 * handled by marauroa itself.
			 */

			/*
			 * TODO: Refactor May be done by the zone itself.
			 */
			for (Player player : players) {
				try {
					player.logic();
				} catch (Exception e) {
					logger.error("Error in player logic", e);
				}
			}

			// SpeakerNPC logic
			NPCList npcList = NPCList.get();
			for (SpeakerNPC npc : npcList) {
				npc.preLogic();
			}

			/*
			 * TODO: Refactor Removable once RPEvent for chat is added.
			 */
			for (Player player : playersRmText) {
				if (player.has("text")) {
					player.remove("text");
					player.notifyWorldAboutChanges();
				}
			}
			playersRmText.clear();

		} catch (Exception e) {
			logger.error("error in beginTurn", e);
		} finally {
			logger.debug("Begin turn: " + (System.nanoTime() - start)
					/ 1000000.0);
		}
	}

	private void debugOutput() {
		/*
		 * Debug statement for inspecting list of things. Most of our memories
		 * leaks came from list keep adding and adding elements.
		 */
		if (Debug.SHOW_LIST_SIZES && (rpman.getTurn() % 1000 == 0)) {

			int objects = 0;

			for (IRPZone zone : StendhalRPWorld.get()) {
				objects += zone.size();
			}

			StringBuffer os = new StringBuffer();
			os.append("entityToKill: " + entityToKill.size() + "\n");
			os.append("npcs: " + npcs.size() + "\n");
			os.append("npcsToAdd: " + npcsToAdd.size() + "\n");
			os.append("npcsToRemove: " + npcsToRemove.size() + "\n");
			os.append("players: " + players.size() + "\n");
			os.append("playersRmText: " + playersRmText.size() + "\n");

			os.append("objects: " + objects + "\n");
			logger.info(os);
		}
	}

	public synchronized void endTurn() {
		long start = System.nanoTime();
		int currentTurn = getTurn();
		try {

			/*
			 * TODO: Refactor Definitively must be done by the zone itself.
			 */
			for (NPC npc : npcs) {
				try {
					npc.logic();
				} catch (Exception e) {
					logger.error("Error in npc logic", e);
				}
			}

			// Registeres classes for this turn
			TurnNotifier.get().logic(currentTurn);

		} catch (Exception e) {
			logger.error("error in endTurn", e);
		} finally {
			logger.debug("End turn: " + (System.nanoTime() - start) / 1000000.0
					+ " (" + (currentTurn % 5) + ")");
		}
	}

	public synchronized boolean onInit(RPObject object) {
		try {
			PlayerEntry entry = PlayerEntryContainer.getContainer().get(object);

			/*
			 * TODO: This is a hack, it should use instead RPObjectFactory.
			 */
			Player player = Player.create(object);
			entry.object = player;

			/*
			 * TODO: Removable once RPEvent chat is added.
			 */
			playersRmText.add(player);

			players.add(player);

			/*
			 * TODO: Hide implementation
			 */
			if (!player.isGhost()) {
				// Notify other players about this event
				for (Player p : getPlayers()) {
					p.notifyOnline(player.getName());
				}
			}
			addGameEvent(player.getName(), "login");
			LoginNotifier.get().onPlayerLoggedIn(player);
			TutorialNotifier.login(player);

			database.setOnlineStatus(player, true);

			return true;
		} catch (Exception e) {
			logger.error("There has been a severe problem loading player "
					+ object.get("#db_id"), e);
			return false;
		}
	}

	public synchronized boolean onExit(RPObject object) {
		try {
			Player player = (Player) object;
			if (wasKilled(player)) {
				logger.info("Logged out shortly before death: Killing it now :)");
				player.onDead(killerOf(player));
			}

			/*
			 * TODO: Hide implementation.
			 */
			// Notify other players about this event
			for (Player p : getPlayers()) {
				p.notifyOffline(player.getName());
			}

			Player.destroy(player);
			players.remove(player);

			addGameEvent(player.getName(), "logout");
			logger.debug("removed player " + player);

			return true;
		} catch (Exception e) {
			logger.error("error in onExit", e);
			return true;
		}
	}

	public synchronized void onTimeout(RPObject object) {
		/*
		 * TODO: Refactor Check new syntax of onTimeout. It is expected to
		 * kickout the player, it can't fail.
		 */
		onExit(object);
	}

	public AccountResult createAccount(String username, String password,
			String email) {
		AccountCreator creator = new AccountCreator(username, password, email);
		return creator.create();
	}

	public CharacterResult createCharacter(String username, String character,
			RPObject template) {
		CharacterCreator creator = new CharacterCreator(username, character,
				template);
		return creator.create();
	}

	public RPServerManager getRPManager() {
		return rpman;
	}

	/**
	 * Tell this message all players.
	 * 
	 * @param message
	 *            Message to tell all players
	 */
	public void tellAllPlayers(final String message) {
		for (Player player : getPlayers()) {
			player.sendPrivateText(message);
			player.notifyWorldAboutChanges();
		}
	}
}
