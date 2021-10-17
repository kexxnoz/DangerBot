package net.abdymazhit.mthd.database;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Config;
import net.abdymazhit.mthd.customs.Player;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameResult;
import net.abdymazhit.mthd.enums.UserRole;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Отвечает за работу с базой данных
 *
 * @version   17.10.2021
 * @author    Islam Abdymazhit
 */
public class Database {

    /** Подключение к базе данных */
    private Connection connection;

    /**
     * Подключается к базе данных
     */
    public Database() {
        Config.MySQL config = MTHD.getInstance().config.mySQL;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(config.url, config.username, config.password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Критическая ошибка! JDBC драйвер не найден!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Критическая ошибка! Неверный SQL запрос!");
        }

        System.out.println("Удочное подключение к базе данных!");

//        Создать таблицы, только при необходимости
        new DatabaseTables(connection);
    }

    /**
     * Получает доступных помощников
     * @return Доступные помощники
     */
    public List<String> getAvailableAssistants() {
        List<String> assistants = new ArrayList<>();
        try {
            ResultSet resultSet = connection.createStatement().executeQuery(
                "SELECT username FROM users INNER JOIN available_assistants ON available_assistants.assistant_id = users.id;");
            while(resultSet.next()) {
                assistants.add(resultSet.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return assistants;
    }

    /**
     * Получает имена игроков команды
     * @param teamId Id команды
     * @return Имена игроков команды
     */
    public List<String> getTeamPlayersNames(int teamId) {
        List<String> playersNames = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT u.username as username FROM users as u
                    INNER JOIN team_live_games_players as lgp ON lgp.team_id = ? AND u.id = lgp.player_id;""");
            preparedStatement.setInt(1, teamId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                playersNames.add(resultSet.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playersNames;
    }

    /**
     * Проверяет, является ли помощником помощником этой игры
     * @param liveGameId Id игры
     * @param assistantId Id помощника
     * @return Значение, является ли помощник помощником этой игры
     */
    public boolean isAssistant(int liveGameId, int assistantId) {
        try {
            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT 1 FROM single_live_games WHERE id = ? AND assistant_id = ?;""");
            preparedStatement.setInt(1, liveGameId);
            preparedStatement.setInt(2, assistantId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getSinglePlayersNames(int liveGameId, int teamId) {
        List<String> playersNames = new ArrayList<>();
        try {
            if(teamId == 0) {
                PreparedStatement captainStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT u.username as username FROM users as u
                    INNER JOIN single_live_games as slg ON u.id = slg.first_team_captain_id AND slg.id = ?;""");
                captainStatement.setInt(1, liveGameId);
                ResultSet captainResultSet = captainStatement.executeQuery();
                while(captainResultSet.next()) {
                    playersNames.add(captainResultSet.getString("username"));
                }
            } else {
                PreparedStatement captainStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT u.username as username FROM users as u
                    INNER JOIN single_live_games as slg ON u.id = slg.second_team_captain_id AND slg.id = ?;""");
                captainStatement.setInt(1, liveGameId);
                ResultSet captainResultSet = captainStatement.executeQuery();
                while(captainResultSet.next()) {
                    playersNames.add(captainResultSet.getString("username"));
                }
            }

            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT u.username as username FROM users as u
                    INNER JOIN single_live_games_players as slgp ON slgp.team_id = ? AND u.id = slgp.player_id AND slgp.live_game_id = ?;""");
            preparedStatement.setInt(1, teamId);
            preparedStatement.setInt(2, liveGameId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                playersNames.add(resultSet.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playersNames;
    }

    /**
     * Получает id пользователя
     * @param discordId Discord id пользователя
     * @return Id пользователя
     */
    public int getUserId(String discordId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT id FROM users WHERE discord_id = ?;");
            preparedStatement.setString(1, discordId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает discord id пользователя
     * @param userId Id пользователя
     * @return Discord id пользователя
     */
    public String getUserDiscordId(int userId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT discord_id FROM users WHERE id = ?;");
            preparedStatement.setInt(1, userId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getString("discord_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает id и discord id пользователя
     * @param username Ник пользователя
     * @return Id и discord id пользователя
     */
    public UserAccount getUserIdAndDiscordId(String username) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT id, discord_id FROM users WHERE username LIKE ?;");
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                UserAccount userAccount = new UserAccount(resultSet.getInt(1));
                userAccount.discordId = resultSet.getString("discord_id");
                return userAccount;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает id пользователя
     * @param username Имя пользователя
     * @return Id пользователя
     */
    public int getUserIdByUsername(String username) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT id FROM users WHERE username LIKE ?;");
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает имя пользователя
     * @param userId Id пользователя
     * @return Имя пользователя
     */
    public String getUserName(int userId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT username FROM users WHERE id = ?;");
            preparedStatement.setInt(1, userId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает id команды пользователя
     * @param userId Id пользователя
     * @return Id команды пользователя
     */
    public int getUserTeamId(int userId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                SELECT id team_id FROM teams WHERE leader_id = ? AND is_deleted IS NULL
                UNION SELECT team_id FROM teams_members WHERE member_id = ?;""");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, userId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getInt("team_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает id команды
     * @param teamName Название команды
     * @return Id команды
     */
    public int getTeamId(String teamName) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT id FROM teams WHERE name LIKE ? AND is_deleted is null;");
            preparedStatement.setString(1, teamName);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает количество очков команды
     * @param teamId Id команды
     * @return Количество очков команды
     */
    public int getTeamPoints(int teamId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT points FROM teams WHERE id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, teamId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getInt("points");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает название команды
     * @param teamId Id команды
     * @return Название команды
     */
    public String getTeamName(int teamId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT name FROM teams WHERE id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, teamId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Проверяет, является ли пользователь лидером команды
     * @param userId Id пользователя
     * @return Значение, является ли пользователь лидером команды
     */
    public boolean isUserTeamLeader(int userId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM teams WHERE leader_id = ? AND is_deleted is null);");
            preparedStatement.setInt(1, userId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Проверяет, является ли пользователь лидером команды
     * @param userId Id пользователя
     * @param teamId Id команды
     * @return Значение, является ли пользователь лидером команды
     */
    public boolean isUserTeamLeader(int userId, int teamId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT EXISTS(SELECT 1 FROM teams WHERE leader_id = ? AND id = ? AND is_deleted is null);");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, teamId);

            ResultSet resultSet = preparedStatement.executeQuery();
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
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT EXISTS(SELECT 1 FROM teams_members WHERE member_id = ? AND team_id = ?);");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, teamId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Получает информацию о игроке Single Rating
     * @param playerId Id игрока
     * @return Информация о игроке Single Rating
     */
    public Player getSinglePlayerInfo(int playerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT u.username, p.points, p.games, p.wins FROM users as u
                    INNER JOIN players as p ON p.player_id = ? AND is_deleted is null AND u.id = ?;""");
            preparedStatement.setInt(1, playerId);
            preparedStatement.setInt(2, playerId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                String username = resultSet.getString("username");
                int points = resultSet.getInt("points");
                int games = resultSet.getInt("games");
                int wins = resultSet.getInt("wins");

                Map<Timestamp, GameResult> lastGameResultsWithTime = new HashMap<>();

                PreparedStatement firstCaptainLastGamesResults = connection.prepareStatement("""
                    SELECT winner_team_id, finished_at FROM single_finished_games_history WHERE first_team_captain_id = ?;""");
                firstCaptainLastGamesResults.setInt(1, playerId);
                ResultSet firstCaptainLastGamesResultsResultSet = firstCaptainLastGamesResults.executeQuery();
                while(firstCaptainLastGamesResultsResultSet.next()) {
                    Timestamp finishedAt = firstCaptainLastGamesResultsResultSet.getTimestamp("finished_at");
                    int winnerTeamId = firstCaptainLastGamesResultsResultSet.getInt("winner_team_id");
                    if(winnerTeamId == 0) {
                        lastGameResultsWithTime.put(finishedAt, GameResult.WIN);
                    } else {
                        lastGameResultsWithTime.put(finishedAt, GameResult.LOSE);
                    }
                }

                PreparedStatement secondCaptainLastGamesResults = connection.prepareStatement("""
                    SELECT winner_team_id, finished_at FROM single_finished_games_history WHERE second_team_captain_id = ?;""");
                secondCaptainLastGamesResults.setInt(1, playerId);
                ResultSet secondCaptainLastGamesResultsResultSet = secondCaptainLastGamesResults.executeQuery();
                while(secondCaptainLastGamesResultsResultSet.next()) {
                    Timestamp finishedAt = secondCaptainLastGamesResultsResultSet.getTimestamp("finished_at");
                    int winnerTeamId = secondCaptainLastGamesResultsResultSet.getInt("winner_team_id");
                    if(winnerTeamId == 1) {
                        lastGameResultsWithTime.put(finishedAt, GameResult.WIN);
                    } else {
                        lastGameResultsWithTime.put(finishedAt, GameResult.LOSE);
                    }
                }

                PreparedStatement playersLastGamesResults = connection.prepareStatement("""
                    SELECT finished_game_id, team_id FROM single_finished_games_players_history WHERE player_id = ?;""");
                playersLastGamesResults.setInt(1, playerId);
                ResultSet playersLastGamesResultsResultSet = playersLastGamesResults.executeQuery();
                while(playersLastGamesResultsResultSet.next()) {
                    int finishedGameId = playersLastGamesResultsResultSet.getInt("finished_game_id");
                    int teamId = playersLastGamesResultsResultSet.getInt("team_id");

                    PreparedStatement playerGameInfo = connection.prepareStatement("""
                        SELECT winner_team_id, finished_at FROM single_finished_games_history WHERE id = ?;""");
                    playerGameInfo.setInt(1, finishedGameId);
                    ResultSet playerGameInfoResultSet = playerGameInfo.executeQuery();
                    while(playerGameInfoResultSet.next()) {
                        Timestamp finishedAt = playerGameInfoResultSet.getTimestamp("finished_at");
                        int winnerTeamId = playerGameInfoResultSet.getInt("winner_team_id");
                        if(winnerTeamId == teamId) {
                            lastGameResultsWithTime.put(finishedAt, GameResult.WIN);
                        } else {
                            lastGameResultsWithTime.put(finishedAt, GameResult.LOSE);
                        }
                    }
                }

                List<Map.Entry<Timestamp, GameResult>> list = new LinkedList<>(lastGameResultsWithTime.entrySet());
                list.sort(Map.Entry.comparingByKey());

                Timestamp latestTime = null;
                List<GameResult> lastGameResults = new ArrayList<>();
                if(list.size() >= 5) {
                    latestTime = list.get(list.size() - 1).getKey();
                    for(int i = list.size() - 1; i >= list.size() - 5; i--) {
                        lastGameResults.add(list.get(i).getValue());
                    }
                } else {
                    if(!list.isEmpty()) {
                        latestTime = list.get(list.size() - 1).getKey();
                        for(int i = list.size() - 1; i >= 0; i--) {
                            lastGameResults.add(list.get(i).getValue());
                        }
                    }
                }

                return new Player(playerId, username, points, games, wins, lastGameResults, latestTime);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean getSingleGamePlayer(int playerId, int liveGameId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT 1 FROM single_live_games_players WHERE team_id is null AND player_id = ? AND live_game_id = ?;""");
            preparedStatement.setInt(1, playerId);
            preparedStatement.setInt(2, liveGameId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getSingleTeamPoints(int liveGameId, int teamId) {
        int points = 0;
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("""
                        SELECT points FROM players as p
                        INNER JOIN single_live_games_players as slgp ON slgp.live_game_id = ?
                        AND p.player_id = slgp.player_id AND p.is_deleted is null AND slgp.team_id = ?;""");
            preparedStatement.setInt(1, liveGameId);
            preparedStatement.setInt(2, teamId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                points += resultSet.getInt("points");
            }

            PreparedStatement preparedStatement2;
            if(teamId == 0) {
                preparedStatement2 = connection.prepareStatement("""
                        SELECT points FROM players as p
                        INNER JOIN single_live_games as slg ON slg.id = ?
                        AND p.player_id = slg.first_team_captain_id AND p.is_deleted is null;""");
            } else {
                preparedStatement2 = connection.prepareStatement("""
                        SELECT points FROM players as p
                        INNER JOIN single_live_games as slg ON slg.id = ?
                        AND p.player_id = slg.second_team_captain_id AND p.is_deleted is null;""");
            }
            preparedStatement2.setInt(1, liveGameId);
            ResultSet resultSet2 = preparedStatement2.executeQuery();
            if(resultSet2.next()) {
                points += resultSet2.getInt("points");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return points;
    }

    /**
     * Добавляет игрока в список участвующих в игре игроков
     * @param teamId Id команды
     * @param playerId Id игрока
     * @return Текст ошибки добавления
     */
    public String addPlayerToTeam(int teamId, int playerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement createStatement = connection.prepareStatement(
                    "UPDATE single_live_games_players SET team_id = ? WHERE player_id = ?;");
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, playerId);
            createStatement.executeUpdate();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении Вас в список участвующих в игре игроков! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Проверят, владеет ли игрок Single Rating
     * @param playerId Id игрока
     * @return Значение, владеет ли игрок Single Rating
     */
    public boolean hasNotSingleRating(int playerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT 1 FROM players WHERE player_id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, playerId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return false;
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
    public int getTeamIdByExactName(String teamName) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT id FROM teams WHERE name = ? AND is_deleted is null;");
            preparedStatement.setString(1, teamName);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает команду лидера
     * @param leaderId Id лидера
     * @return Команда лидера
     */
    public Team getLeaderTeam(int leaderId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT id, name FROM teams WHERE leader_id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, leaderId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                Team team = new Team(resultSet.getInt(1));
                team.name = resultSet.getString("name");
                return team;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает команду участника
     * @param memberId Id участника
     * @return Команда участника
     */
    public Team getMemberTeam(int memberId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT id, name FROM teams WHERE id = (SELECT team_id FROM teams_members WHERE member_id = ?) AND is_deleted is null;");
            preparedStatement.setInt(1, memberId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                Team team = new Team(resultSet.getInt(1));
                team.name = resultSet.getString("name");
                return team;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Добавляет помощника в таблицу доступных помощников
     * @param assistantId Id помощника
     * @return Текст ошибки добавления
     */
    public String setReady(int assistantId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                INSERT INTO available_assistants (assistant_id) SELECT ?
                WHERE NOT EXISTS (SELECT assistant_id FROM available_assistants WHERE assistant_id = ?)""", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, assistantId);
            preparedStatement.setInt(2, assistantId);
            preparedStatement.executeUpdate();

            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if(resultSet.next()) {
                // Вернуть значение, что помощник успешно добавлен в таблицу доступных помощников
                return null;
            } else {
                return "Ошибка! Вы уже в списке доступных помощников!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении в таблицу доступных помощников! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Удаляет помощника из таблицы доступных помощников
     * @param assistantId Id помощника
     * @return Текст ошибки удаления
     */
    public String setUnready(int assistantId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "DELETE FROM available_assistants WHERE assistant_id = ?;");
            preparedStatement.setInt(1, assistantId);
            preparedStatement.executeUpdate();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении из таблицы доступных помощников! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Блокирует игрока
     * @param bannerId Id блокирующего
     * @param playerId Id игрока
     * @param minutes Время бана в минутах
     */
    public void banPlayer(@Nullable Integer bannerId, int playerId, String discordId, int minutes) {
        try {
            Timestamp timestamp = Timestamp.from(Instant.now().plusSeconds(minutes * 60L));

            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                   INSERT INTO players_bans (player_id, discord_id, banner_id, finished_at) SELECT ?, ?, ?, ?
                   WHERE NOT EXISTS (SELECT player_id FROM players_bans WHERE player_id = ?)""", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, playerId);
            preparedStatement.setString(2, discordId);
            if(bannerId == null) {
                preparedStatement.setString(3, null);
            } else {
                preparedStatement.setInt(3, bannerId);
            }
            preparedStatement.setTimestamp(4, timestamp);
            preparedStatement.setInt(5, playerId);
            preparedStatement.executeUpdate();

            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if(!resultSet.next()) {
                PreparedStatement updateStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    UPDATE players_bans SET finished_at = ? WHERE player_id = ?""", Statement.RETURN_GENERATED_KEYS);
                updateStatement.setTimestamp(1, timestamp);
                updateStatement.setInt(2, playerId);
                updateStatement.executeUpdate();
            }

            if(discordId != null) {
                MTHD.getInstance().guild.addRoleToMember(discordId, UserRole.BANNED.getRole()).queue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает подключение к базе данных
     * @return Подключение к базе данных
     */
    public Connection getConnection() {
        return connection;
    }
}