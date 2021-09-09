package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Команда удалить команду
 *
 * @version   09.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamDisbandCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member deleter = event.getMember();

        if(!contentRaw.startsWith("!team disband")) return;
        if(!messageChannel.equals(MTHD.getInstance().myTeamChannel.channel)) return;
        if(deleter == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length > 2) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!deleter.getRoles().contains(UserRole.LEADER.getRole())) {
            message.reply("Ошибка! Команда доступна только для лидеров команд!").queue();
            return;
        }

        if(!deleter.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int deleterId = MTHD.getInstance().database.getUserId(deleter.getId());
        if(deleterId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        Team team = MTHD.getInstance().database.getLeaderTeam(deleterId);
        if(team == null) {
            message.reply("Ошибка! Вы не являетесь лидером какой-либо команды!").queue();
            return;
        }

        String errorMessage = disbandTeam(team.id, deleterId);
        if(errorMessage != null) {
            message.reply(errorMessage).queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(team.name, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        try {
            teamRoles.get(0).delete().submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            message.reply("Критическая ошибка при удалении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Вы успешно удалили команду!").queue();
    }

    /**
     * Удаляет команду
     * @param teamId Id команды
     * @param deleterId Id удаляющего
     * @return Текст ошибки удаления команды
     */
    private String disbandTeam(int teamId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement updateStatement = connection.prepareStatement(
                    "UPDATE teams SET is_deleted = true WHERE id = ? RETURNING (SELECT member_id FROM users WHERE users.id = teams.leader_id);");
            updateStatement.setInt(1, teamId);
            ResultSet updateResultSet = updateStatement.executeQuery();
            updateStatement.close();
            if(updateResultSet.next()) {
                try {
                    MTHD.getInstance().guild.removeRoleFromMember(updateResultSet.getString("member_id"), UserRole.LEADER.getRole()).submit().get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return "Критическая ошибка при удалении у лидера команды роли лидера! Свяжитесь с разработчиком бота!";
                }
            }

            PreparedStatement membersStatement = connection.prepareStatement(
                    "DELETE FROM teams_members WHERE team_id = ? RETURNING (SELECT member_id FROM users WHERE users.id = teams_members.member_id);");
            membersStatement.setInt(1, teamId);
            ResultSet membersResultSet = membersStatement.executeQuery();
            membersStatement.close();
            while(membersResultSet.next()) {
                try {
                    MTHD.getInstance().guild.removeRoleFromMember(membersResultSet.getString("member_id"), UserRole.MEMBER.getRole()).submit().get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return "Критическая ошибка при удалении у участников команды роли участника! Свяжитесь с разработчиком бота!";
                }
            }

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_deletion_history (team_id, deleter_id, deleted_at) VALUES (?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, deleterId);
            historyStatement.setTimestamp(3, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();
            historyStatement.close();

            // Вернуть значение, что команда успешно удалена
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении команды! Свяжитесь с разработчиком бота!";
        }
    }
}
