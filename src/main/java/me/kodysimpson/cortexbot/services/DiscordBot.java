package me.kodysimpson.cortexbot.services;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mikka.cortexbot.commands.help.menu.HelpingMessageContextMenu;
import dev.mikka.cortexbot.commands.help.menu.ReportHelpContextMenu;
import dev.mikka.cortexbot.commands.points.menu.*;
import dev.mikka.cortexbot.listeners.InteractionListener;
import lombok.RequiredArgsConstructor;
import me.kodysimpson.cortexbot.commands.CodeBlockCommand;
import me.kodysimpson.cortexbot.commands.JavaTutCommand;
import me.kodysimpson.cortexbot.commands.LeaderboardCommand;
import me.kodysimpson.cortexbot.commands.SuggestionCommand;
import me.kodysimpson.cortexbot.commands.bounty.DeleteBountyCommand;
import me.kodysimpson.cortexbot.commands.bounty.DoneCommand;
import me.kodysimpson.cortexbot.commands.challenges.ChallengeCommand;
import me.kodysimpson.cortexbot.commands.etc.VeteranCommand;
import me.kodysimpson.cortexbot.commands.jokes.JokeCommand;
import me.kodysimpson.cortexbot.commands.points.*;
import me.kodysimpson.cortexbot.config.DiscordConfiguration;
import me.kodysimpson.cortexbot.listeners.ButtonClickListener;
import me.kodysimpson.cortexbot.listeners.MessageListeners;
import me.kodysimpson.cortexbot.listeners.ModalListener;
import me.kodysimpson.cortexbot.listeners.NewMemberListener;
import me.kodysimpson.cortexbot.model.CortexMember;
import me.kodysimpson.cortexbot.repositories.ChallengeRepository;
import me.kodysimpson.cortexbot.repositories.CortexMemberRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Lazy(false)
public class DiscordBot {

    private final CortexMemberRepository cortexMemberRepository;
    private final DiscordConfiguration discordConfiguration;
    private final VeteranCommand veteranCommand;
    private final GivePointsCommand givePointsCommand;
    private final PointsCommand pointsCommand;
    private final PayCommand payCommand;
    private final DeleteBountyCommand deleteBountyCommand;
    private final MessageListeners messageListeners;
    private final ModalListener modalListener;
    private final NewMemberListener newMemberListener;
    private final DoneCommand doneCommand;
    private final SuggestionCommand suggestionCommand;
    private final JavaTutCommand javaTutCommand;
    private final CodeBlockCommand codeBlockCommand;
    private final LeaderboardCommand leaderboardCommand;
    private final TakePointsCommand takePointsCommand;
    private final SetPointsCommand setPointsCommand;
    private final ThankCommand thankCommand;
    private final ButtonClickListener buttonClickListener;
    private final ChallengeCommand challengeCommand;
    private final JokeCommand jokeCommand;
    private final HelpingMessageContextMenu helpingMessageContextMenu;
    private final ReportHelpContextMenu reportHelpContextMenu;
    private final GivePointsContextMenu givePointsContextMenu;
    private final PayPointsContextMenu payPointsContextMenu;
    private final SetPointsContextMenu setPointsContextMenu;
    private final TakePointsContextMenu takePointsContextMenu;
    private final ThankContextMenu thankContextMenu;
    private final InteractionListener interactionListener;
    private final ChallengeRepository challengeRepository;

    private static JDA api;

    @PostConstruct
    public void init() {

        try {
            CommandClientBuilder commandClient = new CommandClientBuilder()
                    .setPrefix("/")
                    .setOwnerId(discordConfiguration.getOwnerId())
                    .setHelpWord("help")
                    .setActivity(Activity.listening("Pootin cant code"))
                    //Add commands
                    .addSlashCommand(leaderboardCommand)
                    .addSlashCommand(suggestionCommand)
                    .addSlashCommand(codeBlockCommand)
                    .addSlashCommand(javaTutCommand)
                    .addSlashCommand(pointsCommand)
                    .addSlashCommand(givePointsCommand)
                    .addSlashCommand(payCommand)
                    .addSlashCommand(deleteBountyCommand)
                    .addSlashCommand(doneCommand)
                    .addSlashCommand(takePointsCommand)
                    .addSlashCommand(setPointsCommand)
                    .addSlashCommand(thankCommand)
                    .addSlashCommand(veteranCommand)
                    .addSlashCommand(challengeCommand).forceGuildOnly("503656531665879063")
                    .addSlashCommand(jokeCommand).forceGuildOnly("503656531665879063")
                    .addContextMenu(helpingMessageContextMenu)
                    .addContextMenu(reportHelpContextMenu)
                    .addContextMenu(givePointsContextMenu)
                    .addContextMenu(payPointsContextMenu)
                    .addContextMenu(setPointsContextMenu)
                    .addContextMenu(takePointsContextMenu)
                    .addContextMenu(thankContextMenu);

            api = JDABuilder.create(List.of(GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS))
                    .setToken(discordConfiguration.getBotToken())
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS)
                    .addEventListeners(commandClient.build())
                    .addEventListeners(messageListeners)
                    .addEventListeners(newMemberListener)
                    .addEventListeners(buttonClickListener)
                    .addEventListeners(interactionListener)
                    .addEventListeners(modalListener)
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .build().awaitReady();

            System.out.println("BOT STARTED SUCCESSFULLY");
            System.out.flush();

        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static JDA getApi() {
        return api;
    }

    public static Guild getGuild() {
        return getApi().getGuildById("503656531665879063");
    }

    public void addRoleToMember(net.dv8tion.jda.api.entities.Member member, long roleId) {
        try {
            Role role = member.getGuild().getRoleById(roleId);

            if (role != null) {
                getGuild().addRoleToMember(member, role).queueAfter(1, TimeUnit.MINUTES);
            }
        } catch (IllegalArgumentException | InsufficientPermissionException | HierarchyException e) {
            System.out.println(member.getUser().getAsTag() + " did not get the role on join");
            System.out.println(e);
        }
    }

    public static void addRoleToMember(net.dv8tion.jda.api.entities.Member member, long roleId, Consumer<Void> successResponse) {
        try {
            Role role = member.getGuild().getRoleById(roleId);

            if (role != null) {
                getGuild().addRoleToMember(member, role).queueAfter(1, TimeUnit.MINUTES, successResponse);
            }
        } catch (IllegalArgumentException | InsufficientPermissionException | HierarchyException e) {
            System.out.println(member.getUser().getAsTag() + " did not get the role on join");
            System.out.println(e);
        }
    }

    /**
     * Will give the Regular role to top 20 on the leaderboard every 1 hour
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 5000L)
    public void applyRegularRoles() {

        ArrayList<String> topTwenty = (ArrayList<String>) cortexMemberRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(CortexMember::getPoints).reversed())
                .limit(20)
                .map(CortexMember::getUserID)
                .collect(Collectors.toList());

        //Remove the regular role from the current members if they are not top ten
        getGuild().getMembers()
                .forEach(member -> {
                    if (member.getRoles().contains(getApi().getRoleById(discordConfiguration.getRegularRoleId())) && !topTwenty.contains(member.getId())) {
                        getGuild().removeRoleFromMember(member, getApi().getRoleById(discordConfiguration.getRegularRoleId())).queue();
                    }
                });

        //apply it to the top 20 members
        topTwenty.stream()
                .map(id -> getGuild().getMemberById(id))
                .filter(Objects::nonNull)
                .forEach(member -> addRoleToMember(member, Long.valueOf(discordConfiguration.getRegularRoleId())));

    }

    /**
     * Will give the Veteran Coder role to top 5 on the leaderboard every 1 hour
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 5000L)
    public void applyVeteranRoles() {

        ArrayList<String> topFive = (ArrayList<String>) cortexMemberRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(CortexMember::getPoints).reversed())
                .limit(5)
                .map(CortexMember::getUserID)
                .collect(Collectors.toList());

        //Remove the regular role from the current members if they are not top 5
        getGuild().getMembers()
                .forEach(member -> {
                    if (member.getRoles().contains(getApi().getRoleById(discordConfiguration.getRegularRoleId())) && !topFive.contains(member.getId())) {
                        getGuild().removeRoleFromMember(member, getApi().getRoleById(discordConfiguration.getVeteranRoleId())).queue();
                    }
                });

        //apply it to the top 5 members
        topFive.stream()
                .map(id -> getGuild().getMemberById(id))
                .filter(Objects::nonNull)
                .forEach(member -> addRoleToMember(member, Long.valueOf(discordConfiguration.getVeteranRoleId())));

    }

    public static String getUsernameFromUserID(String userId){
        return getApi().retrieveUserById(userId).complete().getAsTag();
    }

}
