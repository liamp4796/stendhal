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
package games.stendhal.server.entity.npc;

import games.stendhal.common.Rand;
import games.stendhal.server.entity.Entity;
import games.stendhal.server.entity.RPEntity;
import games.stendhal.server.entity.item.Corpse;
import games.stendhal.server.pathfinder.FixedPath;
import games.stendhal.server.pathfinder.Node;
import games.stendhal.server.pathfinder.Path;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import marauroa.common.game.Definition;
import marauroa.common.game.RPClass;
import marauroa.common.game.RPObject;
import marauroa.common.game.SyntaxException;
import marauroa.common.game.Definition.Type;

public abstract class NPC extends RPEntity {

	/** the logger instance. */
	private static final Logger logger = Logger.getLogger(NPC.class);

	/**
	 * The NPC's current idea/thought.
	 */
	private String idea;

	public static void generateRPClass() {
		try {
			RPClass npc = new RPClass("npc");
			npc.isA("rpentity");
			npc.addAttribute("class", Type.STRING);
			npc.addAttribute("subclass", Type.STRING);
			npc.addAttribute("text", Type.LONG_STRING, Definition.VOLATILE);
			npc.addAttribute("idea", Type.STRING, Definition.VOLATILE);
			npc.addAttribute("outfit", Type.INT);
		} catch (SyntaxException e) {
			logger.error("cannot generate RPClass", e);
		}
	}

	public NPC(RPObject object) {
		super(object);
		setRPClass("npc");
		update();
	}

	public NPC() {
		setRPClass("npc");
		put("type", "npc");
	}

	/**
	 * Set the NPC's idea/thought.
	 * 
	 * @param idea
	 *            The idea mnemonic, or <code>null</code>.
	 */
	public void setIdea(String idea) {
		if (idea != null) {
			if (!idea.equals(this.idea)) {
				put("idea", idea);
			}
		} else if (has("idea")) {
			remove("idea");
		}

		this.idea = idea;
	}

	/**
	 * Get the NPC's idea/thought.
	 * 
	 * @return The idea mnemonic, or <code>null</code>.
	 */
	public String getIdea() {
		return idea;
	}

	// TODO NPC.setOutfit() function seems not to be used anywhere, so it could
	// be removed.
	public void setOutfit(String outfit) {
		put("outfit", outfit);
	}

	public void say(String text) {
		put("text", text);
	}

	/**
	 * moves to the given entity. When the distance to the destination is
	 * between <code>min</code> and <code>max</code> and this entity does
	 * not have a path already one is searched and saved.
	 * <p>
	 * <b>Note:</b> When the distance to the destination is less than
	 * <code>min</code> the path is removed. <b>Warning:</b> The pathfinder
	 * is not asynchronous, so this thread is blocked until a path is found.
	 * 
	 * @param destEntity
	 *            the destination entity
	 * @param min
	 *            minimum distance to the destination entity
	 * @param max
	 *            maximum distance to the destination entity
	 * @param maxPathRadius
	 *            the maximum radius in which a path is searched
	 */
	public void setMovement(Entity destEntity, double min, double max,
			double maxPathRadius) {
		if (nextTo(destEntity.getX(), destEntity.getY(), min)) {
			stop();

			if (hasPath()) {
				logger.debug("Removing path because nextto("
						+ destEntity.getX() + "," + destEntity.getY() + ","
						+ min + ") of (" + getX() + "," + getY() + ")");
				clearPath();
			}
		} else if ((squaredDistance(destEntity.getX(), destEntity.getY()) > max)) {
			logger.debug("Creating path because (" + getX() + "," + getY()
					+ ") distance(" + destEntity.getX() + ","
					+ destEntity.getY() + ")>" + max);
			List<Node> path = Path.searchPath(this, destEntity, maxPathRadius);
			setPath(new FixedPath(path, false));
		}
	}

	/**
	 * Set a random destination as a path.
	 * 
	 * @param distance
	 *            The maximum axis distance to move.
	 * @param x
	 *            The origin X coordinate for placement.
	 * @param y
	 *            The origin Y coordinate for placement.
	 */
	public void setRandomPathFrom(final int x, final int y, final int distance) {
		int dist2_1 = distance + distance + 1;
		int dx = Rand.rand(dist2_1) - distance;
		int dy = Rand.rand(dist2_1) - distance;

		List<Node> path = new ArrayList<Node>(1);
		path.add(new Node(x + dx, y + dy));

		setPath(new FixedPath(path, false));
	}

	//
	// RPEntity
	//

	/**
	 * Returns true if this RPEntity is attackable
	 */
	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	protected void dropItemsOn(Corpse corpse) {
		// sub classes can implement this method
	}

	@Override
	public void logic() {
		// sub classes can implement this method
	}

}
