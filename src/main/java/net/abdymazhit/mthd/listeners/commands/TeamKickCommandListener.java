package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Команда исключить участника из команды
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamKickCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!contentRaw.startsWith("!team kick")) return;
        if(!messageChannel.equals(MTHD.getInstance().myTeamChannel.channel)) return;
        if(member == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите участника команды!").queue();
            return;
        }

        if(command.length > 3) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.LEADER.getRole())) {
            message.reply("Ошибка! Команда доступна только для лидеров команд!").queue();
            return;
        }

        String deleterName;
        if(member.getNickname() == null) {
            deleterName = member.getEffectiveName();
        } else {
            deleterName = member.getNickname();
        }

        int deleterId = MTHD.getInstance().database.getUserId(deleterName);
        if(deleterId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String memberName = command[2];

        int memberId = MTHD.getInstance().database.getUserId(memberName);
        if(memberId < 0) {
            message.reply("Ошибка! Участник не зарегистрирован на сервере!").queue();
            return;
        }

        int teamId = MTHD.getInstance().database.getLeaderTeamId(deleterId);
        if(teamId < 0) {
            message.reply("Ошибка! Вы не являетесь лидером какой-либо команды!").queue();
            return;
        }


        boolean isUserTeamMember = MTHD.getInstance().database.isUserTeamMember(memberId, teamId);
        if(!isUserTeamMember) {
            message.reply("Ошибка! Участник не является участником вашей команды!").queue();
            return;
        }

        boolean isMemberDeleted = deleteTeamMember(teamId, memberId, deleterId);
        if(!isMemberDeleted) {
            message.reply("Ошибка! По неизвестной причине Вы не смогли выгнать участника из команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Вы успешно выгнали участника из команды!").queue();
    }

    /**
     * Удаляет участника из команды
     * @param teamId Id команды
     * @param memberId Id участника
     * @param deleterId Id удаляющего
     * @return Значение, удален ли участник из команды
     */
    private boolean deleteTeamMember(int teamId, int memberId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement deleteStatement = connection.prepareStatement(
                    "DELETE FROM teams_members WHERE team_id = ? AND member_id = ?;");
            deleteStatement.setInt(1, teamId);
            deleteStatement.setInt(2, memberId);
            deleteStatement.executeUpdate();
            deleteStatement.close();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_members_deletion_history (team_id, member_id, deleter_id, deleted_at) VALUES (?, ?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, memberId);
            historyStatement.setInt(3, deleterId);
            historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();
            historyStatement.close();

            // Вернуть значение, что участник удален
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
