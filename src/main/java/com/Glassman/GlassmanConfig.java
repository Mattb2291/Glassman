package com.Glassman;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

@ConfigGroup(GlassmanConfig.CONFIGGROUP)
public interface GlassmanConfig extends Config
{
	String CONFIGGROUP = "Glassman";

	String validPlayer = "Glassman";
	String timePlayer = "TimePlayer";
}
