package games.stendhal.server.maps.quests;

import games.stendhal.server.*;
import games.stendhal.server.maps.*;
import games.stendhal.server.entity.Player;
import games.stendhal.server.entity.item.Item;
import games.stendhal.server.entity.item.StackableItem;
import games.stendhal.server.entity.creature.Sheep;
import games.stendhal.server.entity.npc.Behaviours;
import games.stendhal.server.entity.npc.NPC;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.pathfinder.Path;

import java.util.*;

import marauroa.common.game.IRPZone;

/**
 * QUEST: Speak with Hackim
 * PARTICIPANTS:
 * - Hackim
 *
 * STEPS:
 * - Talk to Hackim to activate the quest and keep speaking with Hackim.
 *
 * REWARD:
 * - 10 XP (check that user's level is lower than 15)
 * - 5 gold coins
 *
 * REPETITIONS:
 * - As much as wanted.
 */
public class MeetHackim implements IQuest
  {
  private StendhalRPWorld world;
  private NPCList npcs;

  private void step_1()
    {
    StendhalRPZone zone=(StendhalRPZone)world.getRPZone(new IRPZone.ID("int_semos_blacksmith"));

    SpeakerNPC npc=npcs.get("Hackim Easso");

    npc.add(1,"yes",null,50,"We don't sell weapons to wandering adventurers like you! We just make them for the glorious Deniran empire army who's fighting bravely Blordrough's dark legion in the South! Your mere presence here is offending!... Shhh Can you get your ear close?",null);

    npc.add(50,"yes",null,51,"Go to the tavern and talk to Xin Blanca... Ask him what he has to #offer, look at what he #sells, and then say #buy #name_of_item, as in #buy #wooden_shield. He can also be interested in buying weapons from you. Do you want to know how to do it?",null);

    npc.add(51,"yes",null,52,"Ask him what he has to #offer but now look at what he #buys and then say #sell #name_of_item, as in #sell #studded_armor. Do you want to know my little secret?",null);

    npc.add(52,"yes",null,0,null,new SpeakerNPC.ChatAction()
      {
      public void fire(Player player, String text, SpeakerNPC engine)
        {

        int level=player.getLevel();
        String answer;
        if(level<15)
          {
          StackableItem money=(StackableItem)world.getRuleManager().getEntityManager().getItem("money");
          money.setQuantity(5);
          player.equip(money);

          player.addXP(10);

          world.modify(player);

          answer="If somebody asks you, you don't know me!";
          }
        else
          {
          answer="You know, Xoderos' child must be crying somewhere missing those toy weapons you're wearing";
          }
        engine.say("If you're smart, by now you must have figured out who provides weapons to Xin Blanca... No? Ok, it's me... However, for now I can only smuggle minor weapons to him to avoid rising suspicions. To get the best weapons you'll have to take them from the creatures' corpses deep in the dungeon.\n "+answer);
        }
      });

    npc.add(new int[]{1,50,51,52},"no",null,1,"Ok, but don't even happen to touch any weapon. They are counted.",null);

    }

 public MeetHackim(StendhalRPWorld w, StendhalRPRuleProcessor rules)
    {
    this.npcs=NPCList.get();
    this.world=w;

    step_1();
    }
  }
