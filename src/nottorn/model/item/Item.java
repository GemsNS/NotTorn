package nottorn.model.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract root for all item definitions loaded from items.json.
 *
 * Jackson uses the "type" field as a discriminator to instantiate the
 * correct concrete subclass (WEAPON → Weapon, ARMOR → Armor,
 * CONSUMABLE → Consumable).
 *
 * Item objects are TEMPLATES — they describe what an item is.
 * Quantity tracking lives in the player's Inventory (Map<id, qty>).
 * @author  Joel - Student at NSCC
 * @see     <a href="https://gemsns.github.io">Portfolio — gemsns.github.io</a>
 * @see     <a href="https://github.com/gemsns">GitHub — github.com/gemsns</a>
 */
@JsonTypeInfo(
    use     = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible  = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Weapon.class,     name = "WEAPON"),
    @JsonSubTypes.Type(value = Armor.class,      name = "ARMOR"),
    @JsonSubTypes.Type(value = Consumable.class, name = "CONSUMABLE")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Item {

    private int        id;
    private String     name;
    private String     description;
    private ItemRarity rarity;
    private long       basePrice;
    private String     type;  // kept so Jackson can serialize it back

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int        getId()              { return id; }
    public void       setId(int v)         { this.id = v; }

    public String     getName()            { return name; }
    public void       setName(String v)    { this.name = v; }

    public String     getDescription()     { return description; }
    public void       setDescription(String v) { this.description = v; }

    public ItemRarity getRarity()          { return rarity; }
    public void       setRarity(ItemRarity v) { this.rarity = v; }

    public long       getBasePrice()       { return basePrice; }
    public void       setBasePrice(long v) { this.basePrice = v; }

    public String     getType()            { return type; }
    public void       setType(String v)    { this.type = v; }

    @Override
    public String toString() {
        return String.format("[%s] %s ($%,d)", rarity, name, basePrice);
    }
}
