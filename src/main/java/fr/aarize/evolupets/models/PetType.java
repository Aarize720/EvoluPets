package fr.aarize.evolupets.models;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Enum listant les pets disponibles.
 * key = identifiant, displayName = nom pour GUI, entityType = entité spawnée, icon = icone GUI, price = coût en monnaie.
 */
public enum PetType {
    BEE("bee", "Abeille", EntityType.BEE, Material.HONEYCOMB, 500.0),
    WOLF("wolf", "Loup", EntityType.WOLF, Material.BONE, 800.0),
    GOLEM("golem", "Golem", EntityType.IRON_GOLEM, Material.IRON_BLOCK, 1500.0),
    HORSE("horse", "Cheval", EntityType.HORSE, Material.SADDLE, 1000.0),
    PHANTOM("phantom", "Phantom", EntityType.PHANTOM, Material.PHANTOM_MEMBRANE, 1200.0),
    CAT("cat", "Chat", EntityType.CAT, Material.COD, 700.0),
    FOX("fox", "Renard", EntityType.FOX, Material.SWEET_BERRIES, 700.0),
    PARROT("parrot", "Perroquet", EntityType.PARROT, Material.SALMON, 600.0),
    RAVAGER("ravager", "Ravageur", EntityType.RAVAGER, Material.SADDLE, 2000.0),
    ENDERMAN("enderman", "Enderman", EntityType.ENDERMAN, Material.ENDER_PEARL, 1400.0);

    private final String key;
    private final String displayName;
    private final EntityType entityType;
    private final Material icon;
    private final double price;

    PetType(String key, String displayName, EntityType entityType, Material icon, double price) {
        this.key = key;
        this.displayName = displayName;
        this.entityType = entityType;
        this.icon = icon;
        this.price = price;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public EntityType getEntityType() { return entityType; }
    public Material getIcon() { return icon; }
    public double getPrice() { return price; }

    public static PetType fromKey(String key) {
        if (key == null) return null;
        for (PetType pt : values()) if (pt.key.equalsIgnoreCase(key)) return pt;
        return null;
    }
}
