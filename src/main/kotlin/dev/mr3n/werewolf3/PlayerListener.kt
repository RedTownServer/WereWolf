package dev.mr3n.werewolf3

import dev.mr3n.werewolf3.protocol.DeadBody
import dev.mr3n.werewolf3.protocol.MetadataPacketUtil
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.DeathSidebar
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.WaitingSidebar
import dev.mr3n.werewolf3.utils.*
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object PlayerListener: Listener {

    /**
     * tellコマンドなどその他のメッセージコマンドを無効化
     */
    @EventHandler
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        if(event.player.gameMode == GameMode.SPECTATOR && Constants.MESSAGE_COMMANDS.contains(event.message.split(" ").firstOrNull())) {
            event.isCancelled = true
        }
    }

    /**
     * 死んだ際にそのプレイヤーを死体にしてその他死亡時の処理を行う
     */
    @EventHandler
    fun onDead(event: PlayerDeathEvent) {
        val player = event.entity
        val killer = player.killer
        if(!WereWolf3.PLAYERS.contains(player)) { return }
        if(!WereWolf3.PLAYERS.contains(killer)) { return }
        if(killer!=null) {
            killer.playSound(killer, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f)
            killer.addKill(player)
            player.sendTitle(languages("title.you_are_dead.title"),languages("title.you_are_dead.subtitle_with_killer", "%killer%" to killer.name), 0, 100, 20)
            if(player.role?.team==Role.Team.VILLAGER&&killer.role?.team==Role.Team.VILLAGER) {
                WereWolf3.PLAYERS.filter { it.role == Role.WOLF }.forEach { wolf ->
                    wolf.money += Constants.TEAM_KILL_BONUS
                    wolf.sendMessage(languages("team_kill_bonus", "%money%" to "${Constants.TEAM_KILL_BONUS}${Constants.MONEY_UNIT}").asPrefixed())
                    wolf.playSound(wolf, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                }
            }
        } else {
            player.sendTitle(languages("title.you_are_dead.title"),languages("title.you_are_dead.subtitle"), 0, 100, 20)
        }
        // ゲームモードをスペクテイターに設定 注意: 絶対にインベントリを削除する前にスペクテイターに変更してください。
        player.gameMode = GameMode.SPECTATOR
        // 死体を生成 注意: 絶対にインベントリを削除する前に死体を生成してください。
        DeadBody(player)
        // インベントリを削除
        player.inventory.clear()
        // 体力を満タンに設定
        player.health = player.healthScale
        player.sidebar = DeathSidebar(player)
        MetadataPacketUtil.removeAllInvisible(player)
        // 死んだ人にタイトルを表示
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS,20,1,false,false,false))
        // 死亡メッセージを削除
        event.deathMessage = null
        // 血を流す
        player.world.spawnParticle(Particle.BLOCK_CRACK,player.location.clone().add(0.0,1.5,0.0),100,0.5,.5,0.5, Material.REDSTONE_BLOCK.createBlockData())
    }

    /**
     * 夜は近くの人としか喋れない、また死亡者同士の死亡者チャットなどのの処理
     */
    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        if(event.player.gameMode==GameMode.SPECTATOR) {
            event.isCancelled = true
            val format = languages("chat_format", "%name%" to event.player.name, "%message%" to event.message)
            Bukkit.getOnlinePlayers().filter { it.gameMode == GameMode.SPECTATOR }.forEach { player ->
                player.sendMessage(format)
            }
        } else {
            // 遺言を設定
            event.player.will = event.message
            // チャットのフォーマットを設定
            val format = languages("chat_format", "%name%" to event.player.displayName, "%message%" to event.message)
            if (WereWolf3.TIME == Time.DAY) {
                // 朝は全員に送信
                event.format = format
            } else {
                // 夜は特定の人にのみ送信
                event.isCancelled = true
                // スペクテイター、もしくは会話可能範囲内のプレイヤーにチャットを送信
                Bukkit.getOnlinePlayers()
                    .associateWith { it.location.distance(event.player.location) }
                    .filter { it.key.gameMode == GameMode.SPECTATOR || it.value < Constants.CONVERSATION_DISTANCE }
                    .forEach { (player, _) -> player.sendMessage(format) }
                event.player.playSound(event.player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                event.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *TextComponent.fromLegacyText(languages("send_message_at_night", "%distance%" to Constants.CONVERSATION_DISTANCE)))
            }
        }
    }

    /**
     * 体力の自然回復を無効化。
     */
    @EventHandler
    fun onRegainHealth(event: EntityRegainHealthEvent) {
        val player = event.entity
        if(player !is Player) { return }
        if(!WereWolf3.PLAYERS.contains(player)) { return }
        when(event.regainReason) {
            EntityRegainHealthEvent.RegainReason.MAGIC, EntityRegainHealthEvent.RegainReason.MAGIC_REGEN -> {}
            else -> {
                event.isCancelled = true
            }
        }
    }

    /**
     * アイテムをドロップできないようにする
     * TODO 特定のアイテムのみドロップできないようにする
     */
    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if(!WereWolf3.PLAYERS.contains(event.player)) { return }
        event.isCancelled = true
    }

    /**
     * プレイヤーが途中抜けした際にそのプレイヤーを死体にする
     */
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if(WereWolf3.PLAYERS.contains(event.player)&&event.player.gameMode!=GameMode.SPECTATOR) {
            // 途中抜けしたプレイヤーの下を生成し、その上発見させる。
            DeadBody(event.player).found(event.player)
            // ゲームモードをスペクテイターに
            event.player.gameMode = GameMode.SPECTATOR
        }
        WereWolf3.PLAYERS.remove(event.player)
        WereWolf3.PLAYER_BY_ENTITY_ID.remove(event.player.entityId)
        DeadBody.CARRYING.remove(event.player)
    }

    /**
     * プレイヤーが参加した際に実行中だった場合スペクテイターにし、大気中だった場合はボスバーやサイドバーを表示する
     */
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        WereWolf3.PLAYER_BY_ENTITY_ID[player.entityId] = player
        // 参加メッセージを"人狼に参加しました"に変更
        event.joinMessage = languages("messages.player_joined", "%player%" to player.name).asPrefixed()
        WereWolf3.BOSSBAR.addPlayer(player)
        // ゲームが実行中かどうか
        if(WereWolf3.running) {
            // if:実行中だった場合
            // ｷﾗﾘｰﾝを鳴らす
            player.playSound(player,Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f)
            // 実行中であるため最後まで提起する必要であるという旨を表示
            player.sendTitle(languages("name"), languages("messages.please_wait_for_end"), 0, 100, 20)
            // スペクテイターに
            player.gameMode = GameMode.SPECTATOR
            // なめに取り消し線
            player.setDisplayName("${ChatColor.STRIKETHROUGH}${player.name}")
            // プレイヤーにサイドバーを表示
            player.sidebar = DeathSidebar(player)
        } else {
            event.player.gameMode = GameMode.ADVENTURE
            // if:実行中ではない場合
            // プレイヤーにサイドバーを表示
            player.sidebar = WaitingSidebar()
            Bukkit.getOnlinePlayers().forEach { p ->
                val sidebar = p.sidebar
                if(sidebar !is WaitingSidebar) { return@forEach }
                sidebar.players(Bukkit.getOnlinePlayers().size)
            }
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity
        if(player !is Player) { return }
        if(player.gameMode==GameMode.SPECTATOR) {
            event.isCancelled = true
        } else {
            if (WereWolf3.STATUS == Status.RUNNING) { return }
            event.isCancelled = true
        }
    }
}