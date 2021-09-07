package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;

import java.util.List;

/**
 * Канал администрации
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminChannel extends Channel {

    /**
     * Инициализирует канал администрации
     */
    public AdminChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Staff", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "admin");
            createChannel(category, "admin", 0);
            sendChannelMessage();
        }
    }

    /**
     * Отправляет сообщение канала администрации
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Команды администратора");
        embedBuilder.setColor(0xFF58B9FF);
        embedBuilder.addField("Создание команды",
                "Для создания команды введите `!adminteam create <TEAM_NAME> <LEADER_NAME>`", false);
        embedBuilder.addField("Добавление участника в команду",
                "Для добавления участника в команду введите `!adminteam add <TEAM_NAME> <MEMBER_NAME>`", false);
        embedBuilder.addField("Удаление участника из команды",
                "Для удаления участника из команды введите `!adminteam delete <TEAM_NAME> <MEMBER_NAME>`", false);
        channel.sendMessageEmbeds(embedBuilder.build()).queue();
        embedBuilder.clear();
    }
}
