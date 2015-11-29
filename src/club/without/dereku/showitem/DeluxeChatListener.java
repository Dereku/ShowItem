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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import me.clip.deluxechat.events.DeluxeChatJSONEvent;
import org.apache.commons.io.IOUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Dereku
 */
public class DeluxeChatListener implements Listener {
    private String toReplace = "%i%";
    private final ShowItem plugin;
    private String replacement;
    
    public DeluxeChatListener(ShowItem aThis) {
        this.plugin = aThis;
        InputStream is = aThis.getResource("chatReplacement.raw");
        try {
            this.replacement = IOUtils.toString(is, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            aThis.getLogger().log(Level.SEVERE, "Whoops.", ex);
            this.replacement = toReplace;
        }
    }
    
    @EventHandler
    public void onDeluxeChatJSONEvent(DeluxeChatJSONEvent event) {
        //I don't like this method. But it works.
        if (event.getRawChatMessage().toLowerCase().contains(toReplace) && event.getPlayer().hasPermission("showitem.chat")) {
            if (event.getPlayer().getItemInHand().getType().equals(Material.AIR)) {
                event.setJSONChatMessage(event.getJSONChatMessage().replace(toReplace, this.plugin.locale.getProperty("AIR_MESSAGE", "AIR")));
                return;
            }
            
            ItemStack is = event.getPlayer().getItemInHand();
            String s = event.getJSONChatMessage()
                    .replace(toReplace, replacement)
                    .replace("%itemName%", this.plugin.getItemStackName(is))
                    .replace("%amount%", is.getAmount() > 1 ? " x" + is.getAmount() : "")
                    .replace("%itemStack%", this.plugin.parseItemStack(is))
                    .replace("%dc%", event.getRawChatMessage().substring(0, 2));
            event.setJSONChatMessage(s);
        }
    }
}
