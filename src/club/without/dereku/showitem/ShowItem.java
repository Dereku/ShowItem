/*
 * The MIT License
 *
 * Copyright 2015 Dereku.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package club.without.dereku.showitem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.MinecraftKey;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.apache.commons.io.FileUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Dereku
 */
public class ShowItem extends JavaPlugin {
    public final Properties locale = new Properties();
    private String message;
    private File messageFile, localeFile;
    
    @Override
    public void onEnable() {
        this.messageFile = new File(this.getDataFolder().getAbsolutePath(), "message.json");
        this.localeFile = new File(this.getDataFolder().getAbsolutePath(), "locale.properties");
        
        //Check for messages.json
        if (!this.messageFile.exists()) {
            try {
                FileUtils.copyInputStreamToFile(this.getResource("message.json"), this.messageFile);
            } catch (IOException ex) {
                this.getLogger().log(Level.WARNING, "Failed to save default message.", ex);
                this.getLogger().log(Level.WARNING, "Extract \"message.json\" from ShowItem.jar to \"ShowItem\" folder.");
                this.setEnabled(false);
                return;
            }
        }
        
        //Load messages.json
        try {
            this.message = new String(Files.readAllBytes(Paths.get(this.messageFile.toURI())), Charset.forName("UTF-8"));
        } catch (IOException ex) {
            this.getLogger().log(Level.WARNING, "Failed to load message.json", ex);
            this.setEnabled(false);
        }
        
        //Check for locale.properties
        if (!this.localeFile.exists()) {
            try {
                FileUtils.copyInputStreamToFile(this.getResource("locale.properties"), this.localeFile);
            } catch (IOException ex) {
                this.getLogger().log(Level.WARNING, "Failed to save locale.", ex);
                this.getLogger().log(Level.WARNING, "Using inbuilt locale file.");
            }
        }
        
        //Load locale.properties
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(this.localeFile), Charset.forName("UTF-8"))) {
            this.locale.load(isr);
        } catch (IOException ex) {
            this.getLogger().log(Level.WARNING, "Failed to load locale file.", ex);
            try {
                this.locale.load(this.getResource("locale.properties"));
            } catch (IOException ex1) {
                this.getLogger().log(Level.WARNING, "Failed to load inbuilt locale.", ex1);
                this.getLogger().log(Level.WARNING, "Epic fail.");
            }
        }
        
        //DeluxeChat listener. 
        if (this.getServer().getPluginManager().isPluginEnabled("DeluxeChat")) {
            this.getServer().getPluginManager().registerEvents(new DeluxeChatListener(this), this);
        }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player plr = (Player) sender;
        ItemStack is = plr.getItemInHand();
        if (is.getType().equals(Material.AIR)) {
            plr.sendMessage(this.locale.getProperty("SHOW_AIR", "SHOW_AIR"));
            return true;
        }

        this.broadcast(this.message
                .replace("%player%", plr.getName())
                .replace("%itemName%", this.getItemStackName(is))
                .replace("%amount%", is.getAmount() > 1 ? "x" + is.getAmount() : "")
                .replace("%itemStack%", this.parseItemStack(is)));
        return true;
    }

    //Easy way? Where?
    public String parseItemStack(ItemStack is) {
        StringBuilder sb = new StringBuilder();
        sb.append("{id:")
                .append(this.getTrueMaterialName(is))
                .append(",Damage:")
                .append(is.getDurability());

        if (is.getItemMeta().hasEnchants() || is.getItemMeta().hasLore() || is.getItemMeta().hasDisplayName()) {
            sb.append(",tag:{");

            //Check for enchantments
            if (is.getItemMeta().hasEnchants()) {
                sb.append("ench:[");
                for (Iterator<Map.Entry<Enchantment, Integer>> it = is.getItemMeta().getEnchants().entrySet().iterator(); it.hasNext();) {
                    Map.Entry<Enchantment, Integer> e = it.next();
                    sb.append("{id:")
                            .append(e.getKey().getId())
                            .append(",lvl:")
                            .append(e.getValue())
                            .append("}");

                    if (it.hasNext()) {
                        sb.append(",");
                    }
                }
                sb.append("]");
            }

            //Check for name / lore
            if (is.getItemMeta().hasLore() || is.getItemMeta().hasDisplayName()) {
                if (is.getItemMeta().hasEnchants()) {
                    sb.append(",");
                }

                sb.append("display:{");

                //This item has name?
                if (is.getItemMeta().hasDisplayName()) {
                    sb.append("Name:\\\"")
                            .append(is.getItemMeta().getDisplayName())
                            .append("\\\"");
                    //Lore?
                    if (is.getItemMeta().hasLore()) {
                        sb.append(",");
                    }
                }

                //This item has lore?
                if (is.getItemMeta().hasLore()) {
                    sb.append("Lore:[");

                    for (Iterator<String> it = is.getItemMeta().getLore().iterator(); it.hasNext();) {
                        String str = it.next();
                        sb.append("\\\"")
                                .append(str)
                                .append("\\\"");
                        if (it.hasNext()) {
                            sb.append(",");
                        }
                    }
                    sb.append("]");
                }
                sb.append("}");
            }
            sb.append("}");
        }

        sb.append("}");
        return sb.toString();
    }

    public String getItemStackName(ItemStack is) {
        if (is.getType().equals(Material.BANNER)) {
            BannerMeta bm = (BannerMeta) is.getItemMeta();
            return "item.banner." + bm.getBaseColor().toString().toLowerCase().replace("light_blue", "lightBlue") + ".name";
        }
        return CraftItemStack.asNMSCopy(is).a() + ".name";
    }
    
    public String getTrueMaterialName(ItemStack is) {
        net.minecraft.server.v1_8_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(is);
        MinecraftKey key = net.minecraft.server.v1_8_R3.Item.REGISTRY.c(nmsItemStack.getItem());
        return key.toString();
    }

    private void broadcast(String json) {
        for (Player player : this.getServer().getOnlinePlayers()) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(json), (byte) 1));
        }
    }
}