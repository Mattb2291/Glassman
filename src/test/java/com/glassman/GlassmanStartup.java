package com.glassman;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GlassmanStartup
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GlassmanPlugin.class);
		RuneLite.main(args);
	}
}