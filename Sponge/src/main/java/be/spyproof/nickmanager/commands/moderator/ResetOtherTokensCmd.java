package be.spyproof.nickmanager.commands.moderator;

import be.spyproof.nickmanager.commands.AbstractCmd;
import be.spyproof.nickmanager.commands.argument.PlayerDataArg;
import be.spyproof.nickmanager.commands.checks.IArgumentChecker;
import be.spyproof.nickmanager.controller.ISpongeNicknameController;
import be.spyproof.nickmanager.controller.MessageController;
import be.spyproof.nickmanager.model.NicknameData;
import be.spyproof.nickmanager.util.Reference;
import be.spyproof.nickmanager.util.TemplateUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Optional;

/**
 * Created by Spyproof on 01/11/2016.
 */
public class ResetOtherTokensCmd extends AbstractCmd implements IArgumentChecker {

  private static final String ARG = "player";

  private ResetOtherTokensCmd(MessageController messageController, ISpongeNicknameController playerController) {
    super(messageController, playerController);
  }

  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
    NicknameData nicknameData = getArgument(args, ARG);

    nicknameData.setTokensRemaining(0);
    this.getPlayerController().savePlayer(nicknameData);

    Optional<Player> player = Sponge.getServer().getPlayer(nicknameData.getUuid());
    player.ifPresent(value -> value.sendMessage(this.getMessageController().getMessage(Reference.SuccessMessages.ADMIN_NICK_RESET_TOKENS_RECEIVED).toText()));

    src.sendMessage(this.getMessageController().getMessage(Reference.SuccessMessages.ADMIN_NICK_RESET_TOKENS).apply(TemplateUtils.getParameters(nicknameData)).build());
    return CommandResult.success();
  }

  public static CommandSpec getCommandSpec(MessageController messageController, ISpongeNicknameController playerController) {
    return CommandSpec.builder()
                      .arguments(new PlayerDataArg(ARG, playerController))
                      .executor(new ResetOtherTokensCmd(messageController, playerController))
                      .permission(Reference.Permissions.ADMIN_RESET)
                      .build();
  }

}
