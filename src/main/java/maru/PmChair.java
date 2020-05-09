package maru;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockStairs;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.network.protocol.*;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.File;
import java.util.HashSet;
import java.util.Map;

public class PmChair extends PluginBase implements Listener {

    private final Long2LongOpenHashMap onChair = new Long2LongOpenHashMap();
    private final Long2LongOpenHashMap doubleTap = new Long2LongOpenHashMap();
    private final Long2LongOpenHashMap tagblock = new Long2LongOpenHashMap();

    private final Long2ObjectOpenHashMap<HashSet<DataPacket>> chairPackets = new Long2ObjectOpenHashMap<>();

    private Map<String, Object> messages;
    private final LongOpenHashSet disabled = new LongOpenHashSet();

    private static final int[] faces = new int[]{90, 270, 180, 0, 90, 270, 180, 0};

    private static final int m_version = 1;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.loadMessage();
    }

    private void loadMessage() {
        this.saveResource("messages.yml");
        this.messages = new Config(new File(this.getDataFolder(), "messages.yml"), Config.YAML).getAll();
        this.updateMessage();
    }

    private void updateMessage() {
        if ((int) this.messages.get("m_version") < m_version) {
            this.saveResource("messages.yml", true);
            new Config(new File(this.getDataFolder(), "messages.yml"), Config.YAML).getAll().forEach((k, v) -> this.messages.put(k, v));
        }
    }

    private String get(String m) {
        return (String) this.messages.get(this.messages.get("default-language") + "-" + m);
    }

    @EventHandler
    private void onTouch(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        long id = player.getId();

        if (player.isSneaking() || event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || disabled.contains(id)) {
            return;
        }

        Block block = event.getBlock();

        if (!this.onChair.containsKey(id)) {
            if (block instanceof BlockStairs) {
                if (!this.doubleTap.containsKey(id)) {
                    this.doubleTap.put(id, System.currentTimeMillis());
                    player.sendPopup(TextFormat.RED + this.get("touch-popup"));
                    return;
                }

                if (System.currentTimeMillis() - this.doubleTap.get(id) < 1000) {
                    AddEntityPacket addTagblockPacket = new AddEntityPacket();
                    long eid = Entity.entityCount++;
                    this.tagblock.put(id, eid);
                    addTagblockPacket.entityRuntimeId = eid;
                    addTagblockPacket.entityUniqueId = eid;
                    addTagblockPacket.speedX = 0;
                    addTagblockPacket.speedY = 0;
                    addTagblockPacket.speedZ = 0;
                    addTagblockPacket.pitch = 0;
                    addTagblockPacket.yaw = 0;
                    addTagblockPacket.x = (float) (block.getX() + 0.5);
                    addTagblockPacket.y = (float) (block.getY() + 0.3);
                    addTagblockPacket.z = (float) (block.getZ() + 0.5);
                    addTagblockPacket.type = 84;

                    long flags = 0;
                    flags |= 1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG;
                    flags |= 1 << Entity.DATA_FLAG_INVISIBLE;
                    flags |= 1 << Entity.DATA_FLAG_NO_AI;
                    flags |= 1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG;

                    addTagblockPacket.metadata = new EntityMetadata()
                            .putLong(Entity.DATA_FLAGS, flags)
                            .putShort(Entity.DATA_AIR, 400)
                            .putShort(Entity.DATA_MAX_AIR, 400)
                            .putString(Entity.DATA_NAMETAG, TextFormat.AQUA + this.get("tagblock-message"))
                            .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                            .putFloat(Entity.DATA_SCALE, 0.0001f);

                    MoveEntityAbsolutePacket moveTagblockPacket = new MoveEntityAbsolutePacket();
                    moveTagblockPacket.eid = eid;
                    moveTagblockPacket.x = (float) (block.getX() + 0.5);
                    moveTagblockPacket.y = (float) (block.getY() + 0.7);
                    moveTagblockPacket.z = (float) (block.getZ() + 0.5);

                    AddEntityPacket addEntityPacket = new AddEntityPacket();
                    eid = Entity.entityCount++;
                    this.onChair.put(id, eid);
                    addEntityPacket.entityRuntimeId = eid;
                    addEntityPacket.entityUniqueId = eid;
                    addEntityPacket.speedX = 0;
                    addEntityPacket.speedY = 0;
                    addEntityPacket.speedZ = 0;
                    addEntityPacket.pitch = 0;
                    addEntityPacket.yaw = faces[event.getBlock().getDamage()];
                    addEntityPacket.x = (float) (block.getX() + 0.5);
                    addEntityPacket.y = (float) (block.getY() + 1.6);
                    addEntityPacket.z = (float) (block.getZ() + 0.5);
                    addEntityPacket.type = 84;

                    flags = 0;
                    flags |= 1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG;
                    flags |= 1 << Entity.DATA_FLAG_INVISIBLE;
                    flags |= 1 << Entity.DATA_FLAG_NO_AI;
                    flags |= 1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG;

                    addEntityPacket.metadata = new EntityMetadata()
                            .putLong(Entity.DATA_FLAGS, flags)
                            .putShort(Entity.DATA_AIR, 400)
                            .putShort(Entity.DATA_MAX_AIR, 400)
                            .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                            .putFloat(Entity.DATA_SCALE, 0.0001f);

                    MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
                    moveEntityPacket.eid = eid;
                    moveEntityPacket.x = (float) (block.getX() + 0.5);
                    moveEntityPacket.y = (float) (block.getY() + 1.6);
                    moveEntityPacket.z = (float) (block.getZ() + 0.5);
                    moveEntityPacket.yaw = faces[event.getBlock().getDamage()];
                    moveEntityPacket.headYaw = faces[event.getBlock().getDamage()];
                    moveEntityPacket.pitch = 0;

                    SetEntityLinkPacket setEntityLinkPacket = new SetEntityLinkPacket();
                    setEntityLinkPacket.vehicleUniqueId = eid;
                    setEntityLinkPacket.riderUniqueId = player.getId();
                    setEntityLinkPacket.type = SetEntityLinkPacket.TYPE_PASSENGER;

                    this.getServer().getOnlinePlayers().values().forEach((target) -> {
                        target.dataPacket(addEntityPacket);
                        target.dataPacket(moveEntityPacket);
                        target.dataPacket(addTagblockPacket);
                        target.dataPacket(moveTagblockPacket);
                        target.dataPacket(setEntityLinkPacket);
                    });
                    HashSet<DataPacket> packets = new HashSet<>();
                    packets.add(addEntityPacket);
                    packets.add(moveEntityPacket);
                    packets.add(addTagblockPacket);
                    packets.add(moveTagblockPacket);
                    packets.add(setEntityLinkPacket);
                    chairPackets.put(id, packets);

                    //player.setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_RIDING, true); Not really needed?
                    this.doubleTap.remove(id);
                } else {
                    this.doubleTap.put(id, System.currentTimeMillis());
                    player.sendPopup(TextFormat.RED + this.get("touch-popup"));
                }
            }
        } else {
            RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
            removeEntityPacket.eid = this.onChair.remove(id);
            RemoveEntityPacket removeTagblockPacket = new RemoveEntityPacket();
            removeTagblockPacket.eid = this.tagblock.remove(id);
            this.getServer().getOnlinePlayers().values().forEach((p) -> {
                p.dataPacket(removeEntityPacket);
                p.dataPacket(removeTagblockPacket);
            });
            chairPackets.remove(id);
        }
    }

    @EventHandler
    private void onJump(DataPacketReceiveEvent event) {
        if (event.getPacket().pid() == ProtocolInfo.PLAYER_ACTION_PACKET) {
            PlayerActionPacket packet = (PlayerActionPacket) event.getPacket();
            long id = event.getPlayer().getId();
            if (packet.action == PlayerActionPacket.ACTION_JUMP || packet.action == PlayerActionPacket.ACTION_START_SNEAK) { //This one is for the windows users and their bug
                if (this.onChair.containsKey(id)) {
                    RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
                    removeEntityPacket.eid = this.onChair.remove(id);
                    RemoveEntityPacket removeTagblockPacket = new RemoveEntityPacket();
                    removeTagblockPacket.eid = this.tagblock.remove(id);
                    chairPackets.remove(id);
                    this.getServer().getOnlinePlayers().values().forEach((p) -> {
                        p.dataPacket(removeEntityPacket);
                        p.dataPacket(removeTagblockPacket);
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onJoin(PlayerJoinEvent event) {
        this.getServer().getScheduler().scheduleDelayedTask(() ->
                        chairPackets.values().forEach(set -> set.forEach(event.getPlayer()::dataPacket))
                , 40); //Needs delay to be detected
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        long id = event.getPlayer().getId();
        disabled.remove(id);
        if (!this.onChair.containsKey(id)) {
            return;
        }
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.eid = this.onChair.remove(id);
        RemoveEntityPacket removeTagblockPacket = new RemoveEntityPacket();
        removeTagblockPacket.eid = this.tagblock.remove(id);
        this.getServer().getOnlinePlayers().values().forEach((p) -> {
            p.dataPacket(removeEntityPacket);
            p.dataPacket(removeTagblockPacket);
        });
        chairPackets.remove(id);
    }

    @EventHandler
    private void onTeleport(PlayerTeleportEvent event) {
        long id = event.getPlayer().getId();
        if (onChair.containsKey(id)) {
            RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
            removeEntityPacket.eid = this.onChair.remove(id);
            RemoveEntityPacket removeTagblockPacket = new RemoveEntityPacket();
            removeTagblockPacket.eid = this.tagblock.remove(id);
            this.getServer().getOnlinePlayers().values().forEach((p) -> {
                p.dataPacket(removeEntityPacket);
                p.dataPacket(removeTagblockPacket);
            });
            chairPackets.remove(id);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chair")) {
            if (!(sender instanceof Player)) {
                return true;
            }

            if (args.length == 0) {
                return false;
            }

            long id = ((Player) sender).getId();
            if ("off".equals(args[0])) {
                disabled.add(id);
                sender.sendMessage("\u00A7aDisabled");
            } else if ("on".equals(args[0])) {
                disabled.remove(id);
                sender.sendMessage("\u00A7aEnabled");
            } else {
                return false;
            }

            return true;
        }

        return false;
    }
}
