package games.stendhal.server.maps.athor.ship;

import games.stendhal.common.Direction;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.Sentence;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.SpeakerNPCFactory;
import games.stendhal.server.entity.npc.SpeakerNPC.ChatAction;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.maps.athor.ship.AthorFerry.Status;

/** Factory for the captain of Athor Ferry */
public class CaptainNPC extends SpeakerNPCFactory {

	Status ferrystate;

	@Override
	protected SpeakerNPC instantiate(String name) {
		// The NPC is defined as a ferry announcer because he notifies
		// passengers when the ferry arrives or departs.
		SpeakerNPC npc = new SpeakerNPC(name) {

			@Override
			protected void onGoodbye(Player player) {
				// Turn back to the wheel
				setDirection(Direction.DOWN);
			}
		};
		return npc;
	}

	@Override
	public void createDialog(final SpeakerNPC npc) {
		npc.addGreeting("Yo-ho-ho, me bucko!");
		npc.addGoodbye("So long...");
		// if you can make up a help message that is more helpful to the,
		// player, feel free to replace this one.
		npc.addHelp("Never look up when a sea gull is flying over ye head!");
		npc.addJob("I'm th' captain of me boat.");

		npc.add(ConversationStates.ATTENDING, "status", null,
				ConversationStates.ATTENDING, null, new ChatAction() {
					@Override
					public void fire(Player player, Sentence sentence,
							SpeakerNPC npc) {
						npc.say(ferrystate.toString());
						// .getCurrentDescription());
					}
				});

		new AthorFerry.FerryListener() {

			@Override
			public void onNewFerryState(Status status) {
				ferrystate = status;
				switch (status) {
				case ANCHORED_AT_MAINLAND:
				case ANCHORED_AT_ISLAND:
					// capital letters symbolize shouting
					npc.say("LET GO ANCHOR!");
					break;

				default:
					npc.say("ANCHORS AWEIGH! SET SAIL!");
					break;
				}
				// Turn back to the wheel
				npc.setDirection(Direction.DOWN);

			}
		};
	}
}
