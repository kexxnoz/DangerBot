package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Администраторская команда переименования команды
 *
 * @version   09.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamRenameCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member changer = event.getMember();

        if(!contentRaw.startsWith("!adminteam rename")) return;
        if(!messageChannel.equals(MTHD.getInstance().adminChannel.channel)) return;
        if(changer == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите текущее название команды!").queue();
            return;
        }

        if(command.length == 3) {
            message.reply("Ошибка! Укажите новое название команды!").queue();
            return;
        }

        if(command.length > 4) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!changer.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!changer.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int changerId = MTHD.getInstance().database.getUserId(changer.getId());
        if(changerId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String currentTeamName = command[2];
        String newTeamName = command[3];

        int teamId = MTHD.getInstance().database.getTeamIdByExactName(currentTeamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        boolean isRenamed = renameTeam(teamId, currentTeamName, newTeamName, changerId);
        if(!isRenamed) {
            message.reply("Критическая ошибка при переименовании команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(currentTeamName, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        try {
            teamRoles.get(0).getManager().setName(newTeamName).submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            message.reply("Критическая ошибка при переименовании роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Команда успешно переименована! Новое название команды: " + newTeamName).queue();
    }

    /**
     * Переименовывает команду
     * @param teamId Id команды
     * @param fromName Текущее название команды
     * @param toName Новое название команды
     * @param changerId Id изменяющего
     * @return Значение, переименована ли команда
     */
    private boolean renameTeam(int teamId, String fromName, String toName, int changerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement updateStatement = connection.prepareStatement(
                    "UPDATE teams SET name = ? WHERE id = ?;");
            updateStatement.setString(1, toName);
            updateStatement.setInt(2, teamId);
            updateStatement.executeUpdate();
            updateStatement.close();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_names_rename_history (team_id, from_name, to_name, changer_id, changed_at) VALUES (?, ?, ?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setString(2, fromName);
            historyStatement.setString(3, toName);
            historyStatement.setInt(4, changerId);
            historyStatement.setTimestamp(5, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();
            historyStatement.close();

            // Вернуть значение, что команда успешно переименована
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
