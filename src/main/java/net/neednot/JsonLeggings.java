package net.neednot;

import org.bukkit.enchantments.Enchantment;

import java.util.Map;

public class JsonLeggings {

    public String name = "None";
    public String type = "None";
    public String damage = "None";
    public String[] enchants = "".split("");


    public void setName(String name) {
        this.name = name;
    }
    public void setType(String type) {
        this.type = type;
    }
    public void setEnchants(String[] enchants) {
        this.enchants = enchants;
    }
    public void setDamage(String damage) {
        this.damage = damage;
    }
}