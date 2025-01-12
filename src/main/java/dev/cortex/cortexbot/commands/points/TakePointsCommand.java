package dev.cortex.cortexbot.commands.points;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import dev.cortex.cortexbot.repositories.CortexMemberRepository;
import dev.cortex.cortexbot.services.LoggingService;
import dev.cortex.cortexbot.config.DiscordConfiguration;
import dev.cortex.cortexbot.model.CortexMember;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TakePointsCommand extends SlashCommand {

    private CortexMemberRepository cortexMemberRepository;
    private DiscordConfiguration discordConfiguration;
    private LoggingService loggingService;

    public TakePointsCommand() {
        this.name = "take-points";
        this.arguments = "<user id | name | tag> <# of points>";
        this.help = "take points from a member";

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.USER, "user", "The person involved", true));
        options.add(new OptionData(OptionType.INTEGER, "amount", "amount of points to give", true));
        options.add(new OptionData(OptionType.STRING, "reason", "reason", false));
        this.options = options;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        event.deferReply().queue();

        if (event.getMember().isOwner() || event.getMember().getRoles().contains(event.getJDA().getRoleById(discordConfiguration.getStaffRole()))) {

            if (event.getOptions().isEmpty()) {
                event.getHook().sendMessage("Provide a person to take points from. Ex: $take-points 250856681724968960 100").queue();
            } else {

                if (event.getOptions().size() == 1) {
                    event.getHook().sendMessage("An amount of points must be provided. Ex: $take-points 250856681724968960 100").queue();
                } else {

                    //determine who was provided as an argument to this command
                    User user = event.getOption("user").getAsUser();

                    //see if they are trying to give points to themself
                    if (user.getId().equals(event.getMember().getId()) && !event.getMember().isOwner()) {
                        event.getHook().sendMessage("You can't take points from yourself dummy.").queue();
                        return;
                    }

                    CortexMember cortexMember = cortexMemberRepository.findByUserIDIs(user.getId());

                    if (cortexMember != null) {

                        int points = (int) event.getOption("amount").getAsDouble();
                        if (points <= 0) {
                            event.getHook().sendMessage("You need to provide a positive number of points.").queue();
                            return;
                        }

                        cortexMember.setPoints(cortexMember.getPoints() - points);
                        cortexMemberRepository.save(cortexMember);

                        event.getHook().sendMessage(points + " point(s) have been taken from " + user.getName() + ".").queue();

                        //log the points given
                        loggingService.logPointsTaken(user.getName(), points, event.getMember().getEffectiveName());

                        user.openPrivateChannel().flatMap(channel -> {
                            return channel.sendMessage(points + " points have been taken from you. " +
                                    "You now have a total of " + cortexMember.getPoints() + " community points.");
                        }).queue();


                    } else {
                        event.getHook().sendMessage("The user provided does not exist in our database.").queue();
                    }

                }

            }

        } else {
            event.getHook().sendMessage("You must be staff to execute this command.").queue();
        }

    }

    @Autowired
    public void setLoggingService(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    @Autowired
    public void setMemberRepository(CortexMemberRepository cortexMemberRepository) {
        this.cortexMemberRepository = cortexMemberRepository;
    }

    @Autowired
    public void setDiscordConfiguration(DiscordConfiguration discordConfiguration) {
        this.discordConfiguration = discordConfiguration;
    }

}
