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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Dereku
 */
public class ShowItem extends JavaPlugin {

    public final Properties locale = new Properties();
    private String message;
    private File messageFile, localeFile;

    //Yay, reflection!
    private Class classNBTTagCompound;
    private Method asNMSCopy;
    private Method saveNMSItemStackToNBTTagCompound;
    private Method getUnlocalizedNameOfNMSItemStack;
    private Method getDataOfNMSItemStack;
    private Method fromInvColorIndex;
    private Method sendMessage;
    private Method jsonToChatComponent;
    private Object playerListInstance;

    @Override
    public void onEnable() {
        try {
            this.initReflect();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            this.getLogger().log(Level.WARNING, "Failed to init reflection", ex);
            this.getPluginLoader().disablePlugin(this);
            return;
        }

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
            this.message = new String(Files.readAllBytes(Paths.get(this.messageFile.toURI())), StandardCharsets.UTF_8);
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
        try (FileInputStream fis = new FileInputStream(this.localeFile);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
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
    }

    private void initReflect() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String pckg = this.getServer().getClass().getPackage().getName();
        String nmsVersion = pckg.substring(pckg.lastIndexOf('.') + 1);
        String nmsPackage = "net.minecraft.server.";

        Class classCraftItemStack = ClassUtils.getClass("org.bukkit.craftbukkit." + nmsVersion + ".inventory.CraftItemStack");
        this.asNMSCopy = classCraftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);

        Class classEnumColor = ClassUtils.getClass(nmsPackage + nmsVersion + ".EnumColor", false);
        this.fromInvColorIndex = classEnumColor.getDeclaredMethod("fromInvColorIndex", int.class);

        Class classCraftServer = ClassUtils.getClass("org.bukkit.craftbukkit." + nmsVersion + ".CraftServer", false);
        Class classMinecraftServer = ClassUtils.getClass(nmsPackage + nmsVersion + ".MinecraftServer", false);
        Class classPlayerList = ClassUtils.getClass(nmsPackage + nmsVersion + ".PlayerList", false);
        Class classIChatBaseComponent = ClassUtils.getClass(nmsPackage + nmsVersion + ".IChatBaseComponent", false);
        Class classChatSerializer = ClassUtils.getClass(nmsPackage + nmsVersion + ".IChatBaseComponent.ChatSerializer", false);

        this.jsonToChatComponent = classChatSerializer.getDeclaredMethod("a", String.class);

        Method getServer = classCraftServer.getDeclaredMethod("getServer", new Class[0]);
        Method getPlayerList = classMinecraftServer.getMethod("getPlayerList", new Class[0]);

        Object minecraftServer = getServer.invoke(this.getServer(), new Object[0]);
        this.playerListInstance = getPlayerList.invoke(minecraftServer, new Object[0]);
        this.sendMessage = classPlayerList.getMethod("sendMessage", classIChatBaseComponent, boolean.class);

        Class classItemStack = ClassUtils.getClass(nmsPackage + nmsVersion + ".ItemStack", false);
        this.classNBTTagCompound = ClassUtils.getClass(nmsPackage + nmsVersion + ".NBTTagCompound", false);

        this.getUnlocalizedNameOfNMSItemStack = classItemStack.getDeclaredMethod("a", new Class[0]);
        this.getDataOfNMSItemStack = classItemStack.getDeclaredMethod("getData", new Class[0]);
        this.saveNMSItemStackToNBTTagCompound = classItemStack.getDeclaredMethod("save", this.classNBTTagCompound);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        ItemStack is;
        try {
            is = player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError ex) {
            is = player.getItemInHand();
        }
        if (is.getType().equals(Material.AIR)) {
            player.sendMessage(this.locale.getProperty("SHOW_AIR", "SHOW_AIR"));
            return true;
        }

        String msg = this.message
                .replace("%player%", player.getName())
                .replace("%itemName%", this.getItemStackName(is))
                .replace("%amount%", is.getAmount() > 1 ? "x" + is.getAmount() : "")
                .replace("%itemStack%", this.parseItemStack(is));
        this.broadcast(msg);
        return true;
    }

    public String parseItemStack(ItemStack is) {
        try {
            Object nmsItemStack = this.asNMSCopy.invoke(null, is);
            Object nbtTagCompound = this.classNBTTagCompound.newInstance();
            this.saveNMSItemStackToNBTTagCompound.invoke(nmsItemStack, nbtTagCompound);
            return nbtTagCompound.toString().replace("\"", "\\\"");
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            this.getLogger().log(Level.WARNING, "Failed to parse ItemStack", ex);
            this.getLogger().log(Level.WARNING, "ItemStack: {0}", is.serialize().toString());
        }
        return "Failed to parse item";
    }

    public String getItemStackName(ItemStack is) {
        try {
            Object nmsItemStack = this.asNMSCopy.invoke(null, is);

            if (is.getType().equals(Material.BANNER)) {
                int data = (int) this.getDataOfNMSItemStack.invoke(nmsItemStack, new Object[0]);
                Object enumcolor = this.fromInvColorIndex.invoke(null, data & 15);
                return "item.banner." + enumcolor.toString() + ".name";
            }

            return this.getUnlocalizedNameOfNMSItemStack.invoke(nmsItemStack, new Object[0]) + ".name";
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            this.getLogger().log(Level.WARNING, "Failed to get name of ItemStack", ex);
            this.getLogger().log(Level.WARNING, "ItemStack: {0}", is.serialize().toString());
        }
        return "An error occurred while getting the name of the item";
    }

    public void broadcast(String json) {
        try {
            Object chatBaseComponent = this.jsonToChatComponent.invoke(null, json);
            this.sendMessage.invoke(this.playerListInstance, chatBaseComponent, false);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            this.getLogger().log(Level.WARNING, "Failed to broadcast message", ex);
        }
    }
}
