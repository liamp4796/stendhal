package games.stendhal.server.maps.kotoch;

import games.stendhal.common.Direction;
import games.stendhal.server.StendhalRPZone;
import games.stendhal.server.config.ZoneConfigurator;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.SpeakerNPC;

import java.util.Arrays;
import java.util.Map;

public class SmithNPC implements ZoneConfigurator {
	/**
	 * Configure a zone.
	 * 
	 * @param zone
	 *            The zone to be configured.
	 * @param attributes
	 *            Configuration attributes.
	 */
	public void configureZone(StendhalRPZone zone,
			Map<String, String> attributes) {
		buildKotochSmitherArea(zone);
	}

	private void buildKotochSmitherArea(StendhalRPZone zone) {
		SpeakerNPC smith = new SpeakerNPC("Vulcanus") {

			@Override
			// he doesn't move.
			protected void createPath() {
				setPath(null);
			}

			@Override
			protected void createDialog() {
				addGreeting("Chairetismata! I am Vulcanus the smither.");
				addGoodbye("Farewell");
				addHelp("I may help you to get a very #special item for only a few others...");
				addJob("I used to forge weapons for the King of Faiumoni, but this was long ago, since now the way is blocked.");

				add(
						ConversationStates.ATTENDING,
						Arrays.asList("special"),
						null,
						ConversationStates.ATTENDING,
						"Who told you that!?! *cough* Anyway, yes, I can forge a very special item for you. But you will need to complete a #quest",
						null);
			}
		};

		smith.setDescription("You see Vulcanus. You feel a strange sensation near him.");
		smith.setEntityClass("smithnpc");
		smith.setPosition(62, 115);
		smith.setDirection(Direction.DOWN);
		smith.initHP(100);
		zone.add(smith);
	}
}
