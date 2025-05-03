package com.Glassman;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.Skill;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "Glassman",
	description = "Play the game as a Glassman (One Health Point / Nightmare mode)",
	tags = {"glass", "man", "1", "hp", "nightmare", "mode", "damage", "hit", "health", "heart", "fragile"}
)

public class GlassmanPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private GlassmanConfig config;

	@Inject
	private ConfigManager configManager;

	private boolean playerIsFragile = true;
	private final HashSet<WorldArea> tutorialIslandWorldArea = new HashSet<WorldArea>();

	@Provides
	GlassmanConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GlassmanConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		getTutorialIslandWorldAreas();
	}

	@Override
	protected void shutDown() throws Exception
	{
		tutorialIslandWorldArea.clear();
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned p) {
		if (p.getPlayer() == client.getLocalPlayer())
		{
			overrideSprites();

			if (locationIsOnTutorialIsland(client.getLocalPlayer().getWorldLocation()))
			{
				if (getCombatExperience() == 0) {
					if (!isPlayerFragile())
					{
						sendGamemodeMessage("You begin to feel fragile... as though with just one hit your journey" +
								" will be over and your heart will shatter. How far will you get?",	Color.MAGENTA);
						playerIsFragile = true;
						setPlayerConfig(GlassmanConfig.timePlayer,Instant.now().toString() );
						setPlayerConfig(GlassmanConfig.validPlayer,Boolean.toString(playerIsFragile));
					}
				}
				else
				{
					sendGamemodeMessage("You have previously entered combat and are ineligible to be a Glassman.",
							Color.RED);
					restoreGame();
				}
			}
			else
			{
				playerIsFragile = isPlayerFragile();
				if (!playerIsFragile)
				{
					sendGamemodeMessage("You are not eligible to be a Glassman.",
							Color.RED);
					restoreGame();
				}
			}
		}
	}

	private long getCombatExperience()
	{
		return client.getSkillExperience(Skill.ATTACK) + client.getSkillExperience(Skill.STRENGTH) +
				client.getSkillExperience(Skill.DEFENCE) + client.getSkillExperience(Skill.MAGIC) +
				client.getSkillExperience(Skill.RANGED);
	}

	private boolean isPlayerFragile()
	{
		String playerString = getPlayerConfig(GlassmanConfig.validPlayer);
		if (playerString == null) {return false;}
		return Boolean.parseBoolean(playerString);
	}

	private void removePlayerFromFragileMode()
	{
		setPlayerConfig(GlassmanConfig.validPlayer,null);
	}

	private void setPlayerConfig(String key, Object value)
	{
		if (value != null)
		{
			configManager.setRSProfileConfiguration(GlassmanConfig.CONFIGGROUP, key, value);
		}
		else
		{
			configManager.unsetRSProfileConfiguration(GlassmanConfig.CONFIGGROUP, key);
		}
	}
	private String getPlayerConfig(String key)
	{
		return configManager.getRSProfileConfiguration(GlassmanConfig.CONFIGGROUP, key);
	}

	private void sendGamemodeMessage(String msg, Color formatColor)
	{
		String message = ColorUtil.wrapWithColorTag(String.format(msg),formatColor);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	private void getTutorialIslandWorldAreas()
	{
		if (tutorialIslandWorldArea.isEmpty())
		{
			// RegionIDs: 12080, 12336 and 12592
			tutorialIslandWorldArea.add(new WorldArea(3053, 3072,103,64,0));
			// RegionIDs: 12079 and 12335
			tutorialIslandWorldArea.add(new WorldArea(3059, 3051,77,21,0));
			// RegionID: 12436 - The basement/dungeon where combat is learned
			tutorialIslandWorldArea.add(new WorldArea(3072, 9493,45,41,0));
		}
	}

	private boolean locationIsOnTutorialIsland(WorldPoint playerLocation){
		for (WorldArea worldArea : tutorialIslandWorldArea)
		{
			if (worldArea.contains2D(playerLocation))
			{
				return true;
			}
		}
		return false;
	}

	private void restoreGame()
	{
		playerIsFragile = false;
		removePlayerFromFragileMode();
		setHPOrbText(client.getBoostedSkillLevel(Skill.HITPOINTS));
		setHPStatText(client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS), false);
		restoreSprites();
	}

	@Subscribe
	public void onGameTick(GameTick t)
	{
		if (playerIsFragile)
		{
			setHPOrbText(1);
			setHPStatText(1,1, true);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied damage)
	{
		if (damage.getActor() != client.getLocalPlayer()) {return;}

		if (damage.getHitsplat().getAmount() > 0 && playerIsFragile)
		{

			sendGamemodeMessage("And with one blow, your fragile heart shatters. Having taken damage, you are no" +
					" longer worthy of Glassman status...", Color.RED);

			sendGamemodeMessage("You were a Glassman for a duration of: " +
					getTimeFragile() + " without taking damage!", Color.RED);

			restoreGame();
		}
	}

	private String getTimeFragile()
	{
		Duration durationFragile = Duration.between(Instant.parse(getPlayerConfig(GlassmanConfig.timePlayer)), Instant.now());
		return durationFragile.toDaysPart() + " days and " +
				durationFragile.toHoursPart() + " hours and " +
				durationFragile.toMinutesPart() + " minutes.";
	}

	private void overrideSprites()
	{
		for (SpriteOverride spriteOverride : SpriteOverride.values())
		{
			try
			{
				final String spriteFile = "../../" + spriteOverride.getSpriteID() + ".png";
				BufferedImage spriteImage = ImageUtil.getResourceStreamFromClass(getClass(), spriteFile);
				SpritePixels spritePixels = ImageUtil.getImageSpritePixels(spriteImage, client);
				client.getSpriteOverrides().put(spriteOverride.getSpriteID(), spritePixels);
			}
			catch (RuntimeException ex)
			{
				log.debug("Unable to load sprite: ", ex);
			}
		}
	}

	private void restoreSprites()
	{
		client.getWidgetSpriteCache().reset();
		for (SpriteOverride spriteOverride : SpriteOverride.values())
		{
			client.getSpriteOverrides().remove(spriteOverride.getSpriteID());
		}
	}

	private void setHPOrbText(int levelToDisplay)
	{
		Widget HPTextWidget = client.getWidget(InterfaceID.Orbs.HEALTH_TEXT);
		if (HPTextWidget != null)
		{
			HPTextWidget.setText(Integer.toString(levelToDisplay));
		}
	}

	private void setHPStatText(int topStatLevel, int bottomStatLevel, boolean disableHitpointsListener)
	{
		Widget HPStatWidget = client.getWidget(net.runelite.api.widgets.InterfaceID.SKILLS, 9);
		if (HPStatWidget != null) {
			HPStatWidget.setHasListener(disableHitpointsListener);
			Widget[] HPStatWidgetComponents = HPStatWidget.getDynamicChildren();
			HPStatWidgetComponents[2].getSpriteId();
			HPStatWidgetComponents[3].setText(Integer.toString(topStatLevel));
			HPStatWidgetComponents[4].setText(Integer.toString(bottomStatLevel));
		}
	}
}