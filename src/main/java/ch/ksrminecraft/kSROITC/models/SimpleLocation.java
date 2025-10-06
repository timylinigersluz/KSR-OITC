package ch.ksrminecraft.kSROITC.models;

import org.bukkit.Location;
import org.bukkit.World;

public class SimpleLocation {
    private double x, y, z;
    private float yaw, pitch;

    public SimpleLocation() {}
    public SimpleLocation(double x, double y, double z, float yaw, float pitch) {
        this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
    }

    public Location toBukkit(World world) { return new Location(world, x, y, z, yaw, pitch); }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }
}
