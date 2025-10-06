package ch.ksrminecraft.kSROITC.utils;

import ch.ksrminecraft.kSROITC.models.SimpleLocation;
import org.bukkit.Location;

public class LocationUtil {
    public static SimpleLocation toSimple(Location l) {
        return new SimpleLocation(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }
}
