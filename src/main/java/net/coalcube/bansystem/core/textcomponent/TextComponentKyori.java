package net.coalcube.bansystem.core.textcomponent;

import com.velocitypowered.api.proxy.Player;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import net.coalcube.bansystem.core.util.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

public class TextComponentKyori implements TextComponent {
    private final ConfigurationUtil configurationUtil;

    public TextComponentKyori(ConfigurationUtil configurationUtil) {
        this.configurationUtil = configurationUtil;
    }

    @Override
    public void sendLogsFooter(User user, int page, int maxPage) {
        String rawFooter = configurationUtil.getMessage("bansystem.logs.show.footer");

        rawFooter = rawFooter
                .replaceAll("%curpage%", String.valueOf(page))
                .replaceAll("%maxpage%", String.valueOf(maxPage));

        net.kyori.adventure.text.TextComponent footer = Component.empty();
        net.kyori.adventure.text.TextComponent next = Component
                .text(configurationUtil.getMessage("bansystem.logs.show.buttons.next"));
        net.kyori.adventure.text.TextComponent previous = Component
                .text(configurationUtil.getMessage("bansystem.logs.show.buttons.previous"));

        next = next.clickEvent(ClickEvent.clickEvent(net.kyori.adventure.text.event.ClickEvent.Action.RUN_COMMAND,
                        "/bansys logs show "+ (page+1)));

        previous = previous.clickEvent(ClickEvent.clickEvent(net.kyori.adventure.text.event.ClickEvent.Action.RUN_COMMAND,
                        "/bansys logs show "+ (page-1)));

        String[] splitFooter = rawFooter.split("%");

        for(String s : splitFooter) {
            if(s.equalsIgnoreCase("next")) {
                if(page < maxPage)
                    footer = footer.append(next);
            } else if(s.equalsIgnoreCase("previous")) {
                if(page != 1)
                    footer = footer.append(previous);
            } else {
                footer = footer.append(Component.text(s));
            }
        }
        if(user.getUniqueId() == null) {
            user.sendMessage(footer.toString());
        } else
            ((Player) user.getRawUser()).sendMessage(footer);
    }
}
