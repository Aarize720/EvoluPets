package fr.aarize.evolupets.models;

public class PlayerPet {
    private final PetType type;
    private int level;
    private int xp;

    public PlayerPet(PetType type, int level, int xp) {
        this.type = type;
        this.level = Math.max(1, level);
        this.xp = Math.max(0, xp);
    }

    public PetType getType() { return type; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = Math.max(0, xp); }
    public void addXp(int amount) { this.xp = Math.max(0, this.xp + Math.max(0, amount)); }
}
