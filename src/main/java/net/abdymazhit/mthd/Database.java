package net.abdymazhit.mthd;

import net.abdymazhit.mthd.customs.Config;

import java.sql.*;

/**
 * Отвечает за работу с базой данных
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public class Database {

    /** Подключение к базе данных */
    private Connection connection;

    /**
     * Подключается к базе данных
     */
    public Database() {
        Config.PostgreSQL config = MTHD.getInstance().config.postgreSQL;

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try {
            connection = DriverManager.getConnection(config.url, config.username, config.password);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        if(connection == null) {
            throw new IllegalArgumentException("Не удалось подключиться к базе данных");
        }

//        Создать таблицы, только при необходимости
        createTables();
    }

    /**
     * Создает таблицы
     */
    private void createTables() {
        createUsersTable();
        createUsersAuthHistoryTable();

        createSingleRatingTable();
        createTeamRatingTable();

        createTeamsTable();
        createTeamsCreationHistoryTable();
        createTeamsDeletionHistoryTable();

        createTeamsMembersTable();
        createTeamsMembersAdditionHistoryTable();
        createTeamsMembersDeletionHistoryTable();
    }

    /**
     * Создает таблицу пользователей
     */
    private void createUsersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS users (" +
                    "id serial not null constraint users_pk primary key, " +
                    "member_id varchar(50) not null, " +
                    "username varchar(50) not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории авторизации пользователей
     */
    private void createUsersAuthHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS users_auth_history (" +
                    "id serial not null constraint users_auth_history_pk primary key, " +
                    "member_id varchar(50) not null, " +
                    "user_id int not null, " +
                    "authorized_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу обладетелей Single Rating
     */
    private void createSingleRatingTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS single_rating (" +
                    "id serial not null constraint single_rating_pk primary key, " +
                    "user_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу обладетелей Team Rating
     */
    private void createTeamRatingTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS team_rating (" +
                    "id serial not null constraint team_rating_pk primary key, " +
                    "user_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу команд
     */
    private void createTeamsTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams (" +
                    "id serial not null constraint teams_pk primary key, " +
                    "name varchar(50) not null, " +
                    "leader_id int not null, " +
                    "points int default 0 not null, " +
                    "is_deleted boolean);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории создания команд
     */
    private void createTeamsCreationHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_creation_history (" +
                    "id serial not null constraint teams_creation_history_pk primary key, " +
                    "team_id int not null, " +
                    "name varchar(50) not null, " +
                    "leader_id int not null, " +
                    "creator_id int not null, " +
                    "created_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления команд
     */
    private void createTeamsDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_deletion_history (" +
                    "id serial not null constraint teams_deletion_history_pk primary key, " +
                    "team_id int not null, " +
                    "deleter_id int not null, " +
                    "deleted_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу участников команд
     */
    private void createTeamsMembersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_members (" +
                    "id serial not null constraint teams_members_pk primary key, " +
                    "team_id int not null, " +
                    "member_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории добавления участников команд
     */
    private void createTeamsMembersAdditionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_members_addition_history (" +
                    "id serial not null constraint teams_members_addition_history_pk primary key, " +
                    "team_id int not null, " +
                    "member_id int not null, " +
                    "adder_id int not null, " +
                    "added_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления участников команд
     */
    private void createTeamsMembersDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_members_deletion_history (" +
                    "id serial not null constraint teams_members_deletion_history_pk primary key, " +
                    "team_id int not null, " +
                    "member_id int not null, " +
                    "deleter_id int not null, " +
                    "deleted_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает id пользователя
     * @param username Ник пользователя
     * @return Id пользователя
     */
    public int getUserId(String username) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM users WHERE username ILIKE ?;");
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Проверяет, является ли пользователь участником какой-либо команды
     * @param userId Id пользователя
     * @return Значение, является ли пользователь участником какой-либо
     */
    public boolean isUserTeamMember(int userId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams_members WHERE member_id = ?);");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Проверяет, является ли пользователь участником команды
     * @param userId Id пользователя
     * @param teamId Id команды
     * @return Значение, является ли пользователь участником команды
     */
    public boolean isUserTeamMember(int userId, int teamId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams_members WHERE member_id = ? AND team_id = ?);");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, teamId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Проверяет, является ли пользователь лидером какой-либо команды
     * @param userId Id пользователя
     * @return Значение, является ли пользователь лидером какой-либо команды
     */
    public boolean isUserTeamLeader(int userId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams WHERE leader_id = ? AND is_deleted is null);");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Проверяет, существует ли команда по названию
     * @param teamName Название команды
     * @return Значение, существует ли команда
     */
    public boolean isTeamExists(String teamName) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams WHERE name ILIKE ? AND is_deleted is null);");
            preparedStatement.setString(1, teamName);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Получает id команды
     * @param teamName Название команды
     * @return Id команды
     */
    public int getTeamId(String teamName) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM teams WHERE name ILIKE ? AND is_deleted is null;");
            preparedStatement.setString(1, teamName);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Получает подключение к базе данных
     * @return Подключение к базе данных
     */
    public Connection getConnection() {
        return connection;
    }
}