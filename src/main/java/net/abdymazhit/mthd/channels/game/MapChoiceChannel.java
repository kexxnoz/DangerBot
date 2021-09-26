package net.abdymazhit.mthd.channels.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.Rating;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора карты
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class MapChoiceChannel extends Channel {

    /** Категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Список доступных карт */
    public final List<GameMap> gameMaps;

    /** Роль текущей банющей команды */
    public Role currentBannerTeamRole;

    /** Текущий банющий капитан */
    public Member currentBannerCaptain;

    /** Id сообщения о картах */
    public String channelMapsMessageId;

    /** Таймер обратного отсчета */
    public Timer timer;

    /** Значение, отправляется ли сообщение о доступных картах */
    public boolean isMapsMessageSending;

    /** Время каждого раунда бана */
    private static final int roundTime = 20;

    /**
     * Инициализирует канал выбора карты
     * @param gameCategoryManager Категория игры
     */
    public MapChoiceChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;
        gameMaps = new ArrayList<>();
        isMapsMessageSending = true;

        Category category = MTHD.getInstance().guild.getCategoryById(gameCategoryManager.categoryId);
        if(category == null) {
            System.out.println("Критическая ошибка! Категория Game не существует!");
            return;
        }

        List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(gameCategoryManager.game.firstTeamCaptain.discordId,
            gameCategoryManager.game.secondTeamCaptain.discordId).get();
        Member firstTeamCaptain = members.get(0);
        Member secondTeamCaptain = members.get(1);
        if(firstTeamCaptain == null || secondTeamCaptain == null) {
            System.out.println("Критическая ошибка! Не удалось получить роли начавших игру первой и второй команды!");
            return;
        }

        if(gameCategoryManager.game.format.equals("4x2")) {
            Collections.addAll(gameMaps, GameMap.values4x2());
        } else if(gameCategoryManager.game.format.equals("6x2")) {
            Collections.addAll(gameMaps, GameMap.values6x2());
        }

        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            MTHD.getInstance().guild.retrieveMemberById(gameCategoryManager.game.assistantAccount.discordId).queue(assistant ->
                    category.createTextChannel("map-choice").setPosition(2)
                            .addPermissionOverride(firstTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE), null)
                            .addPermissionOverride(secondTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE), null)
                            .addPermissionOverride(gameCategoryManager.firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                            .addPermissionOverride(gameCategoryManager.secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                            .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                            .addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL), null)
                            .queue(textChannel -> {
                                channelId = textChannel.getId();
                                EmbedBuilder embedBuilder = new EmbedBuilder();
                                embedBuilder.setTitle("Вторая стадия игры - Выбор карты");
                                embedBuilder.setColor(3092790);
                                embedBuilder.setDescription("""
                        Начавшие поиск игры команд (%first_team% и %second_team%) должны решить какую карту будут играть!
                        
                        Обратите внимание, если Вы не успеете заблокировать карту за отведенное время, тогда будет заблокирована случайная карта.
          
                        Заблокировать карту
                        `!ban <ПОРЯДКОВЫЙ НОМЕР> или <НАЗВАНИЕ КАРТЫ>`"""
                                        .replace("%first_team%", gameCategoryManager.firstTeamRole.getAsMention())
                                        .replace("%second_team%", gameCategoryManager.secondTeamRole.getAsMention()));
                                textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                                embedBuilder.clear();
                                updateMapsMessage(textChannel);
                            })
            );
        } else {
            MTHD.getInstance().guild.retrieveMemberById(gameCategoryManager.game.assistantAccount.discordId).queue(assistant -> {
                ChannelAction<TextChannel> createAction = category.createTextChannel("map-choice").setPosition(2)
                        .addPermissionOverride(firstTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                        .addPermissionOverride(secondTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                        .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                        .addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL), null);

                for(Member member : gameCategoryManager.players) {
                    createAction = createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null);
                }

                createAction.queue(textChannel -> {
                    channelId = textChannel.getId();
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("Вторая стадия игры - Выбор карты");
                    embedBuilder.setColor(3092790);
                    embedBuilder.setDescription("""
                        Капитаны команд (%first_captain% и %second_captain%) должны решить какую карту будут играть!
          
                        Заблокировать карту
                        `!ban <ПОРЯДКОВЫЙ НОМЕР> или <НАЗВАНИЕ КАРТЫ>`"""
                            .replace("%first_captain%", gameCategoryManager.game.firstTeamCaptainMember.getAsMention())
                            .replace("%second_captain%", gameCategoryManager.game.secondTeamCaptainMember.getAsMention()));
                    textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                    embedBuilder.clear();
                    updateMapsMessage(textChannel);
                });
            });
        }
    }

    /**
     * Банит карту
     * @param gameMap Карта
     */
    public void banMap(GameMap gameMap) {
        gameMaps.remove(gameMap);

        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал map-choice не существует!");
            return;
        }

        updateMapsMessage(textChannel);
    }

    /**
     * Обновляет сообщение о доступных картах
     */
    private void updateMapsMessage(TextChannel textChannel) {
        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            if(currentBannerTeamRole == null) {
                currentBannerTeamRole = gameCategoryManager.firstTeamRole;
            } else {
                if(currentBannerTeamRole.equals(gameCategoryManager.firstTeamRole)) {
                    currentBannerTeamRole = gameCategoryManager.secondTeamRole;
                } else {
                    currentBannerTeamRole = gameCategoryManager.firstTeamRole;
                }
            }
        } else {
            if(currentBannerCaptain == null) {
                currentBannerCaptain = gameCategoryManager.game.firstTeamCaptainMember;
            } else {
                if(currentBannerCaptain.equals(gameCategoryManager.game.firstTeamCaptainMember)) {
                    currentBannerCaptain = gameCategoryManager.game.secondTeamCaptainMember;
                } else {
                    currentBannerCaptain = gameCategoryManager.game.firstTeamCaptainMember;
                }
            }
        }

        GameMap[] maps = new GameMap[0];
        if(gameCategoryManager.game.format.equals("4x2")) {
            maps = GameMap.values4x2();
        } else if(gameCategoryManager.game.format.equals("6x2")) {
            maps = GameMap.values6x2();
        }

        Map<GameMap, BufferedImage> images = new HashMap<>();

        if(gameMaps.size() == 1) {
            images.put(gameMaps.get(0), gameMaps.get(0).getPickImage());
            for(GameMap gameMap : maps) {
                if(!images.containsKey(gameMap)) {
                    images.put(gameMap, gameMap.getBanImage());
                }
            }
        } else {
            for(GameMap gameMap : maps) {
                if(gameMaps.contains(gameMap)) {
                    images.put(gameMap, gameMap.getNormalImage());
                } else {
                    images.put(gameMap, gameMap.getBanImage());
                }
            }
        }

        BufferedImage image = new BufferedImage(((maps.length / 2) + 1) * 710,624 * 2, BufferedImage.TYPE_INT_ARGB);
        int x = 0;
        boolean isSecondLine = false;
        for(int i = 0; i < maps.length; i++) {
            int index = (maps.length / 2) + 1;
            if(i < index) {
                image.getGraphics().drawImage(images.get(maps[i]), x, 0, null);
            } else {
                if(!isSecondLine) {
                    x = 0;
                }
                isSecondLine = true;
                image.getGraphics().drawImage(images.get(maps[i]), x, 624, null);
            }
            x += 710;
        }

        try {
            if(timer != null) {
                timer.cancel();
            }

            File file = new File("./maps/image.png");
            ImageIO.write(image, "png", file);
            createCountdownTask(textChannel, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таймер обратного отсчета
     * @param file Файл изображения
     */
    private void createCountdownTask(TextChannel textChannel, File file) {
        if(gameMaps.size() == 1) {
            gameCategoryManager.setGameState(GameState.GAME_CREATION);
            gameCategoryManager.setGameMap(gameMaps.get(0));

            isMapsMessageSending = true;
            textChannel.editMessageById(channelMapsMessageId, """
                    Карта игры успешно выбрана! Название карты: %map_name%. Переход к созданию игры..."""
                .replace("%map_name%", gameMaps.get(0).getName()))
                .retainFiles(new ArrayList<>()).addFile(file).queue(message -> {
                    isMapsMessageSending = false;
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            gameCategoryManager.createGameChannel();
                        }
                    }, 7000);
                });
        } else {
            if(timer != null) {
                timer.cancel();
                timer = null;
            }

            AtomicBoolean canStart = new AtomicBoolean(false);
            if(channelMapsMessageId == null) {
                isMapsMessageSending = true;
                textChannel.sendMessageEmbeds(getBanPickMessage(roundTime))
                    .addFile(file).queue(message -> {
                    channelMapsMessageId = message.getId();
                    isMapsMessageSending = false;
                    canStart.set(true);
                });
            } else {
                isMapsMessageSending = true;
                textChannel.editMessageEmbedsById(channelMapsMessageId, getBanPickMessage(roundTime))
                    .retainFiles(new ArrayList<>()).addFile(file).queue(message -> {
                    isMapsMessageSending = false;
                    canStart.set(true);
                });
            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if(canStart.get()) {
                        AtomicInteger time =  new AtomicInteger(roundTime);
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if(time.get() % 2 == 0) {
                                    textChannel.editMessageEmbedsById(channelMapsMessageId, getBanPickMessage(time.get())).queue();
                                }

                                if(time.get() <= 0) {
                                    banMap(gameMaps.get(new Random().nextInt(gameMaps.size())));
                                    cancel();
                                }
                                time.getAndDecrement();
                            }
                        }, 0, 1000);
                        cancel();
                    }
                }
            }, 0, 1000);
        }
    }

    /**
     * Получает сообщение о бане
     * @param time Время до бана
     * @return Сообщение о бане
     */
    private MessageEmbed getBanPickMessage(int time) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(3092790);

        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            embedBuilder.setTitle("Команда %team% должна забанить карту!"
                    .replace("%team%", currentBannerTeamRole.getName()));
        } else {
            if(currentBannerCaptain.getNickname() != null) {
                embedBuilder.setTitle("Капитан %captain% должен забанить карту!"
                        .replace("%captain%", currentBannerCaptain.getNickname()));
            } else {
                embedBuilder.setTitle("Капитан %captain% должен забанить карту!"
                        .replace("%captain%", currentBannerCaptain.getEffectiveName()));
            }
        }

        embedBuilder.setDescription("Оставшееся время для бана карты: `%time% сек.`"
                .replace("%time%", String.valueOf(time)));

        StringBuilder maps1String = new StringBuilder();
        StringBuilder maps2String = new StringBuilder();

        GameMap[] maps = new GameMap[0];
        if(gameCategoryManager.game.format.equals("4x2")) {
            maps = GameMap.values4x2();
        } else if(gameCategoryManager.game.format.equals("6x2")) {
            maps = GameMap.values6x2();
        }

        for(int i = 0; i < maps.length; i++) {
            int index = (maps.length / 2) + 1;
            GameMap gameMap = maps[i];
            if(i < index) {
                if(gameMaps.contains(gameMap)) {
                    maps1String.append(gameMap.getId()).append(". ").append(gameMap.getName()).append("\n");
                }
            } else {
                if(gameMaps.contains(gameMap)) {
                    maps2String.append(gameMap.getId()).append(". ").append(gameMap.getName()).append("\n");
                }
            }
        }

        embedBuilder.addField("Первая строка", maps1String.toString(), true);
        embedBuilder.addField("Вторая строка", maps2String.toString(), true);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}