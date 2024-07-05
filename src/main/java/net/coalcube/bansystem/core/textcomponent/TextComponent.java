package net.coalcube.bansystem.core.textcomponent;

import net.coalcube.bansystem.core.util.User;

public interface TextComponent {
    void sendLogsFooter(User user, int page, int maxPage);
}
