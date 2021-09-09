package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
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
 * Администраторская команда добавления участника в команду
 *
 * @version   09.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamAddCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member adder = event.getMember();

        if(!contentRaw.startsWith("!adminteam add")) return;
        if(!messageChannel.equals(MTHD.getInstance().adminChannel.channel)) return;
        if(adder == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
            return;
        }

        if(command.length == 3) {
            message.reply("Ошибка! Укажите участника команды!").queue();
            return;
        }

        if(command.length > 4) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!adder.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!adder.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int adderId = MTHD.getInstance().database.getUserId(adder.getId());
        if(adderId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String teamName = command[2];
        String memberName = command[3];

        UserAccount memberAccount = MTHD.getInstance().database.getUserIdAndDiscordId(memberName);
        if(memberAccount == null) {
            message.reply("Ошибка! Участник не зарегистрирован на сервере!").queue();
            return;
        }

        int memberTeamId = MTHD.getInstance().database.getUserTeamId(memberAccount.getId());
        if(memberTeamId > 0) {
            message.reply("Ошибка! Участник уже состоит в команде!").queue();
            return;
        }

        int teamId = MTHD.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        boolean isMemberAdded = addTeamMember(teamId, memberAccount.getId(), adderId);
        if(!isMemberAdded) {
            message.reply("Критическая ошибка при добавлении участника в команду! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(teamName, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        try {
            MTHD.getInstance().guild.addRoleToMember(memberAccount.getDiscordId(), teamRoles.get(0)).submit().get();
            MTHD.getInstance().guild.addRoleToMember(memberAccount.getDiscordId(), UserRole.MEMBER.getRole()).queue();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            message.reply("Критическая ошибка при добавлении участнику роли команды и роли участника! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Участник успешно добавлен в команду! Название команды: " + teamName + ", ник участника: " + memberName).queue();
    }

    /**
     * Добавляет участника в команду
     * @param teamId Id команды
     * @param memberId Id участника
     * @param adderId Id добавляющего
     * @return Значение, добавлен ли участник в команду
     */
    private boolean addTeamMember(int teamId, int memberId, int adderId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement addStatement = connection.prepareStatement(
                    "INSERT INTO teams_members (team_id, member_id) VALUES (?, ?);");
            addStatement.setInt(1, teamId);
            addStatement.setInt(2, memberId);
            addStatement.executeUpdate();
            addStatement.close();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_members_addition_history (team_id, member_id, adder_id, added_at) VALUES (?, ?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, memberId);
            historyStatement.setInt(3, adderId);
            historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();
            historyStatement.close();

            // Вернуть значение, что участник успешно добавлен
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
