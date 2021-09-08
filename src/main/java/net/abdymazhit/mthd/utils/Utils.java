package net.abdymazhit.mthd.utils;

import com.google.gson.JsonArray;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.customs.UserAccount;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Date;

/**
 * Представляет собой инструменты для упрощения работы
 *
 * @version   08.09.2021
 * @author    Islam Abdymazhit
 */
public class Utils {

    /** Объект, отвечает за создание embed сообщений */
    private final EmbedBuilder embedBuilder;

    /**
     * Инициализирует инструменты
     */
    public Utils() {
        embedBuilder = new EmbedBuilder();
    }

    /**
     * Отправляет GET запрос по URL
     * @param url URL
     * @return Результат запроса в типе String
     */
    public String sendGetRequest(String url) {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Отправляет POST запрос по URL
     * @param url URL
     * @param jsonArray JSON массив
     * @return Результат запроса в типе String
     */
    public String sendPostRequest(String url, JsonArray jsonArray) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(jsonArray.toString(), ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает embed сообщение о успешной авторизации
     * @param username Имя игрока
     * @param level Уровень игрока
     * @param percent Проценты игрока
     * @param rank Ранг игрока
     * @return Embed сообщение о успешной авторизации
     */
    public MessageEmbed getAuthInfoMessageEmbed(String username, String level, String percent, String rank) {
        embedBuilder.setColor(3092790);
        embedBuilder.setTitle("Успешная авторизация!");
        embedBuilder.setDescription("""
                **Ваш ник:** `%username%`
                **Уровень:** `%level% [%percent%%]`
                **Статус:** `%rank%`
                """
                .replace("%username%", username)
                .replace("%level%", level)
                .replace("%percent%", String.valueOf((int) (Double.parseDouble(percent) * 100)))
                .replace("%rank%", rank));
        embedBuilder.setThumbnail("http://skin.vimeworld.ru/head/3d/" + username +".png");
        embedBuilder.setTimestamp(new Date().toInstant());

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }

    /**
     * Получает информационное embed сообщение команды
     * @param team Команда
     * @return Информационное embed сообщение команды
     */
    public MessageEmbed getTeamInfoMessageEmbed(Team team) {
        StringBuilder membersString = new StringBuilder();
        if(team.getLeader().isVimeOnline()) {
            membersString.append("<:emote:884826184729366538> ");
        } else {
            membersString.append("<:emote:884826184641294346> ");
        }

        if(team.getLeader().isDiscordOnline()) {
            membersString.append("<:emote:884825784857010196> ");
        } else {
            membersString.append("<:emote:884825362863910962> ");
        }
        membersString.append("`").append(team.getLeader().getUsername()).append("`").append("\n");

        for(UserAccount user : team.getMembers()) {
            if(user.isVimeOnline()) {
                membersString.append("<:emote:884826184729366538> ");
            } else {
                membersString.append("<:emote:884826184641294346> ");
            }

            if(user.isDiscordOnline()) {
                membersString.append("<:emote:884825784857010196> ");
            } else {
                membersString.append("<:emote:884825362863910962> ");
            }
            membersString.append("`").append(user.getUsername()).append("`").append("\n");
        }

        embedBuilder.setColor(3092790);
        embedBuilder.addField("Игроки", ">>> " + membersString, false);
        embedBuilder.addField("Рейтинг", """
                >>> ```
                %points%
                ```
                """.replace("%points%", String.valueOf(team.getPoints())), true);
        embedBuilder.addField("Побед", """
                >>> ```
                %wins%
                ```
                """.replace("%wins%", String.valueOf(team.getWins())), true);
        embedBuilder.addField("Всего игр", """
                >>> ```
                %games%
                ```
                """.replace("%games%", String.valueOf(team.getGames())), true);
        embedBuilder.setDescription("""
                ```
                \040\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040%team_name%\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040
                ```
                """.replace("%team_name%", team.getName()));
        embedBuilder.setImage("https://media.discordapp.net/attachments/880669658116673606/884176299562762300/line.gif?width=324&height=4");

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}