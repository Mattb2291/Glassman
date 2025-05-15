package com.glassman;

import lombok.Getter;
import net.runelite.api.SpriteID;


@Getter
enum SpriteOverride
{
    MINIMAP_ORB_HITPOINTS(SpriteID.MINIMAP_ORB_HITPOINTS),
    MINIMAP_ORB_HITPOINTS_ICON(SpriteID.MINIMAP_ORB_HITPOINTS_ICON),
    SKILL_HITPOINTS(SpriteID.SKILL_HITPOINTS);

    private final int spriteID;

    SpriteOverride(int spriteID)
    {
        this.spriteID = spriteID;
    }
}
