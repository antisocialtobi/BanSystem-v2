package net.coalcube.bansystem.core.util;

import net.md_5.bungee.api.chat.ClickEvent;

public class TextComponentmd5 implements TextComponent {
    private final ConfigurationUtil configurationUtil;

    public TextComponentmd5(ConfigurationUtil configurationUtil) {
        this.configurationUtil = configurationUtil;
    }


    @Override
    public void sendLogsFooter(User user, int page, int maxPage) {
        String rawFooter = configurationUtil.getMessage("bansystem.logs.show.footer");
        net.md_5.bungee.api.chat.TextComponent footer = new net.md_5.bungee.api.chat.TextComponent();
        net.md_5.bungee.api.chat.TextComponent next = new net.md_5.bungee.api.chat.TextComponent();
        net.md_5.bungee.api.chat.TextComponent previous = new net.md_5.bungee.api.chat.TextComponent();

        next.setText(configurationUtil.getMessage("bansystem.logs.show.buttons.next"));
        next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bansys logs show " + (page + 1)));

        previous.setText(configurationUtil.getMessage("bansystem.logs.show.buttons.previous"));
        previous.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bansys logs show " + (page - 1)));

        rawFooter = rawFooter
                .replaceAll("%curpage%", String.valueOf(page))
                .replaceAll("%maxpage%", String.valueOf(maxPage));

        String[] splitFooter = rawFooter.split("%");

        for (String s : splitFooter) {
            if (s.equalsIgnoreCase("next")) {
                if (page < maxPage)
                    footer.addExtra(next);
            } else if (s.equalsIgnoreCase("previous")) {
                if (page != 1)
                    footer.addExtra(previous);
            } else {
                footer.addExtra(s);
            }
        }

        user.sendMessage(footer);

    }
}
