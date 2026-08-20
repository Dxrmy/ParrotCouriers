package com.parrotcouriers.command;

import com.parrotcouriers.ParrotCouriersPlugin;
import com.parrotcouriers.model.CourierData;
import com.parrotcouriers.model.CourierState;
import com.parrotcouriers.model.DeliveryRecord;
import com.parrotcouriers.model.TargetType;
import com.parrotcouriers.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for /courier commands with history, perch priority, and recall features.
 */
public class CourierCommand implements CommandExecutor, TabCompleter {

    private final ParrotCouriersPlugin plugin;

    public CourierCommand(ParrotCouriersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be executed by players.");
                return true;
            }
            if (!player.hasPermission("parrotcouriers.use")) {
                TextUtil.sendMessage(player, "<red>You do not have permission to use parrot couriers.</red>");
                return true;
            }
            listCouriers(player);
            return true;
        }

        if (sub.equals("history") || sub.equals("log")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be executed by players.");
                return true;
            }
            if (!player.hasPermission("parrotcouriers.history") && !player.hasPermission("parrotcouriers.use")) {
                TextUtil.sendMessage(player, "<red>You do not have permission to view delivery history.</red>");
                return true;
            }
            showHistory(player);
            return true;
        }

        if (sub.equals("perch")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be executed by players.");
                return true;
            }
            if (!player.hasPermission("parrotcouriers.perch") && !player.hasPermission("parrotcouriers.use")) {
                TextUtil.sendMessage(player, "<red>You do not have permission to manage delivery perches.</red>");
                return true;
            }
            handlePerch(player, args);
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("parrotcouriers.admin")) {
                if (sender instanceof Player player) {
                    TextUtil.sendMessage(player, "<red>You do not have permission to reload the configuration.</red>");
                } else {
                    sender.sendMessage("You do not have permission to reload the configuration.");
                }
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(TextUtil.parse("<green>✔ ParrotCouriers configuration, particles, and settings reloaded successfully.</green>"));
            return true;
        }

        if (sub.equals("cancel") || sub.equals("recall")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be executed by players.");
                return true;
            }
            if (!player.hasPermission("parrotcouriers.recall") && !player.hasPermission("parrotcouriers.use")) {
                TextUtil.sendMessage(player, "<red>You do not have permission to recall couriers.</red>");
                return true;
            }
            cancelCourier(player);
            return true;
        }

        if (sub.equals("claim")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be executed by players.");
                return true;
            }
            if (!player.hasPermission("parrotcouriers.claim") && !player.hasPermission("parrotcouriers.use")) {
                TextUtil.sendMessage(player, "<red>You do not have permission to claim returned courier items.</red>");
                return true;
            }
            claimReturnedCourier(player);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void listCouriers(Player player) {
        List<CourierData> list = plugin.getCourierManager().getCouriersByOwner(player.getUniqueId());
        if (list.isEmpty()) {
            TextUtil.sendMessage(player, "<yellow>You have no active parrot couriers right now.</yellow>");
            return;
        }

        TextUtil.sendMessage(player, "<gold><b>Active Couriers:</b></gold>");
        for (CourierData data : list) {
            String target = (data.getTargetType() == TargetType.COORDINATES)
                    ? String.format("%.0f, %.0f, %.0f", data.getTargetX(), data.getTargetY(), data.getTargetZ())
                    : data.getTargetPlayerName();
            TextUtil.sendMessage(player, "<gray>• Target: <yellow>" + target + "</yellow> • State: <green>" + data.getState() + "</green></gray>");
        }
    }

    private void showHistory(Player player) {
        List<DeliveryRecord> records = plugin.getCourierManager().getHistoryForPlayer(player.getName());
        if (records.isEmpty()) {
            TextUtil.sendMessage(player, "<yellow>No past delivery history found.</yellow>");
            return;
        }

        TextUtil.sendMessage(player, "<gold><b>=== Courier Delivery Ledger ===</b></gold>");
        int count = 0;
        for (DeliveryRecord r : records) {
            if (++count > 8) break;
            boolean isSender = r.senderName().equalsIgnoreCase(player.getName());
            String arrow = isSender ? "<green>Sent to</green> <yellow>" + r.recipientName() + "</yellow>" : "<yellow>Received from</yellow> <gold>" + r.senderName() + "</gold>";
            TextUtil.sendMessage(player, "<gray>[" + r.getFormattedDate() + "] " + arrow + " • <white>" + r.payloadSummary() + "</white></gray>");
        }
    }

    private void handlePerch(Player player, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("set")) {
            Location loc = player.getLocation().getBlock().getLocation();
            plugin.getCourierManager().setPlayerPerch(player.getUniqueId(), loc);
            TextUtil.sendMessage(player, "<green>✔ Delivery Perch set at your current location! Couriers will land here instead of crowding you.</green>");
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
            plugin.getCourierManager().setPlayerPerch(player.getUniqueId(), null);
            TextUtil.sendMessage(player, "<yellow>Delivery Perch removed.</yellow>");
            return;
        }

        if (args.length > 1 && (args[1].equalsIgnoreCase("prioritize") || args[1].equalsIgnoreCase("priority") || args[1].equalsIgnoreCase("toggle"))) {
            boolean enable;
            if (args.length > 2) {
                String val = args[2].toLowerCase();
                enable = val.equals("on") || val.equals("true") || val.equals("enable");
            } else {
                enable = !plugin.getCourierManager().isPerchPrioritized(player.getUniqueId());
            }
            plugin.getCourierManager().setPerchPriority(player.getUniqueId(), enable);
            String status = enable ? "<green>ENABLED</green> (Couriers will land on your Perch)" : "<yellow>DISABLED</yellow> (Couriers will fly directly to you)";
            TextUtil.sendMessage(player, "<gold>Perch Prioritization is now " + status + ".</gold>");
            return;
        }

        Location current = plugin.getCourierManager().getPlayerPerch(player.getUniqueId());
        boolean isPrioritized = plugin.getCourierManager().isPerchPrioritized(player.getUniqueId());
        if (current != null) {
            String priorityStr = isPrioritized ? "<green>Active (Landing at Perch)</green>" : "<yellow>Inactive (Landing at Player)</yellow>";
            TextUtil.sendMessage(player, "<gold>Delivery Perch: <yellow>" + current.getBlockX() + ", " + current.getBlockY() + ", " + current.getBlockZ() + "</yellow> in <yellow>" + current.getWorld().getName() + "</yellow> • Priority: " + priorityStr + "</gold>");
            TextUtil.sendMessage(player, "<gray>Commands: <white>/courier perch set</white> | <white>/courier perch prioritize [on|off]</white> | <white>/courier perch remove</white></gray>");
        } else {
            TextUtil.sendMessage(player, "<yellow>You don't have a Perch set. Stand where you want couriers to land and type <white>/courier perch set</white>.</yellow>");
        }
    }

    private void cancelCourier(Player player) {
        List<CourierData> list = plugin.getCourierManager().getCouriersByOwner(player.getUniqueId());
        if (list.isEmpty()) {
            TextUtil.sendMessage(player, "<red>You have no active couriers to recall.</red>");
            return;
        }

        CourierData data = list.get(0);
        Parrot parrot = plugin.getCourierManager().findParrotEntity(data.getCourierUuid());

        // Check if trade GUI is currently open by recipient
        if (data.isGuiOpen()) {
            TextUtil.sendMessage(player, "<red>Cannot recall courier while the recipient has the trade window open!</red>");
            return;
        }

        // Case 1: Setup phase (before flight) -> immediately revert & refund
        if (data.getState() == CourierState.AWAITING_PAYLOAD || data.getState() == CourierState.AWAITING_PAYMENT) {
            if (data.getPayloadItem() != null) {
                player.getInventory().addItem(data.getPayloadItem());
            }
            if (data.getLetterItem() != null) {
                player.getInventory().addItem(data.getLetterItem());
            }
            plugin.getCourierManager().finalizeAndRevert(parrot, data);
            TextUtil.sendMessage(player, "<green>Courier setup cancelled. Items returned to your inventory.</green>");
            return;
        }

        // Case 2: Mid-flight, waiting at destination, or in unloaded chunks -> recall back to owner!
        data.setState(CourierState.IN_TRANSIT_TO_OWNER);
        data.setTransitStartTime(System.currentTimeMillis());
        data.setStuckTicks(0);
        data.setRetryCount(0);

        if (parrot != null && parrot.isValid()) {
            data.saveToPdc(parrot.getPersistentDataContainer(), plugin);
            if (plugin.getConfigManager().isShowNametags()) {
                parrot.customName(TextUtil.parse("<yellow>[Returning Courier]</yellow> <gold>" + data.getOwnerName() + "</gold>"));
                parrot.setCustomNameVisible(true);
            }
            TextUtil.sendMessage(player, "<yellow>Courier delivery recalled! The parrot is now flying back to you with your package.</yellow>");
        } else {
            // Unloaded chunk recovery: Restore courier with exact feather color/variant and package directly at owner
            Parrot rescuedParrot = player.getWorld().spawn(player.getLocation().add(0, 0.2, 0), Parrot.class, p -> {
                if (data.getParrotVariant() != null) {
                    try {
                        p.setVariant(Parrot.Variant.valueOf(data.getParrotVariant()));
                    } catch (Exception ignored) {}
                }
                p.setTamed(true);
                p.setOwner(player);
            });
            plugin.getCourierManager().finalizeAndRevert(rescuedParrot, data);

            if (data.getPayloadItem() != null) {
                player.getInventory().addItem(data.getPayloadItem());
            }
            if (data.getLetterItem() != null) {
                player.getInventory().addItem(data.getLetterItem());
            }
            rescuedParrot.getWorld().spawnParticle(Particle.PORTAL, rescuedParrot.getLocation().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.05);
            rescuedParrot.getWorld().playSound(rescuedParrot.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            TextUtil.sendMessage(player, "<green>✔ Courier recalled from distant unloaded lands safely back to your side!</green>");
        }

        plugin.getCourierManager().saveAll();
    }

    private void claimReturnedCourier(Player player) {
        List<CourierData> list = plugin.getCourierManager().getCouriersByOwner(player.getUniqueId());
        for (CourierData data : list) {
            if (data.getState() == CourierState.WAITING_FOR_OWNER) {
                Parrot parrot = plugin.getCourierManager().findParrotEntity(data.getCourierUuid());
                plugin.getGuiManager().openClaimGui(player, data, parrot);
                return;
            }
        }
        TextUtil.sendMessage(player, "<red>No returned courier is currently waiting for collection.</red>");
    }

    private void sendHelp(CommandSender sender) {
        boolean reqPrefix = plugin.getConfigManager().isRequirePrefix();
        String pfx = reqPrefix ? plugin.getConfigManager().getNamePrefix() : "";
        sender.sendMessage(TextUtil.parse("<gold><b>=== Parrot Couriers Guide ===</b></gold>"));
        sender.sendMessage(TextUtil.parse("<yellow>1. Tame a parrot with seeds.</yellow>"));
        sender.sendMessage(TextUtil.parse("<yellow>2. Name it with a Name Tag (e.g. '<gold>" + pfx + "Dormy</gold>' or '<gold>" + pfx + "100 64 -200</gold>').</yellow>"));
        sender.sendMessage(TextUtil.parse("<yellow>3. (Optional) Feed a Sweet Berry for speed or Glow Berry for night glowing!</yellow>"));
        sender.sendMessage(TextUtil.parse("<yellow>4. (Optional) Right-click with a Book/Note to attach a letter.</yellow>"));
        sender.sendMessage(TextUtil.parse("<yellow>5. Right-click with package to send, then right-click with payment.</yellow>"));
        sender.sendMessage(TextUtil.parse("<yellow>6. Recipient sneaks near courier to accept trade; courier flies back with payment!</yellow>"));
        sender.sendMessage(TextUtil.parse("<gray>Commands: /courier list | /courier recall | /courier claim | /courier history | /courier perch [set|prioritize|remove] | /courier reload</gray>"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("help", "list", "recall", "claim", "history", "perch"));
            if (sender.hasPermission("parrotcouriers.admin")) {
                completions.add("reload");
            }
            return completions.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("perch")) {
            return List.of("set", "prioritize", "remove");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("perch") && args[1].equalsIgnoreCase("prioritize")) {
            return List.of("on", "off");
        }
        return List.of();
    }
}
