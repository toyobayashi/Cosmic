package api.model.dto;

public class GiveResourceDTO {
    private String type;
    private Integer characterId;
    private String characterName;
    private Boolean global;
    private Integer quantity;
    private Integer itemId;
    private Float rate;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getCharacterId() { return characterId; }
    public void setCharacterId(Integer characterId) { this.characterId = characterId; }
    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }
    public Boolean getGlobal() { return global; }
    public void setGlobal(Boolean global) { this.global = global; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public Float getRate() { return rate; }
    public void setRate(Float rate) { this.rate = rate; }
}
