package dev.mr3n.werewolf3

import dev.mr3n.werewolf3.protocol.DeadBody
import dev.mr3n.werewolf3.protocol.MetadataPacketUtil
import dev.mr3n.werewolf3.sidebar.DeathSidebar
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.WaitingSidebar
import dev.mr3n.werewolf3.utils.*
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object PlayerListener: Listener {
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

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        event.player.will = event.message
        event.format = languages("chat", "%name%" to event.player.displayName, "%message%" to event.message)
    }

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

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if(!WereWolf3.PLAYERS.contains(event.player)) { return }
        event.isCancelled = true
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        WereWolf3.PLAYERS.remove(event.player)
        WereWolf3.PLAYER_BY_ENTITY_ID.remove(event.player.entityId)
        DeadBody.DEAD_BODY_BY_UUID[event.player.uniqueId]?.destroy()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        WereWolf3.PLAYER_BY_ENTITY_ID[player.entityId] = player
        // 参加メッセージを"人狼に参加しました"に変更
        event.joinMessage = languages("messages.player_joined", "%player%" to player.name).asPrefixed()
        // ゲームが実行中かどうか
        if(WereWolf3.running) {
            // if:実行中だった場合
            // プレイヤーが途中抜けしていたかどうか
            if(player.gameId==null||player.gameId!=WereWolf3.GAME_ID) {
                // if:新規参加っだった場合
                // ｷﾗﾘｰﾝを鳴らす
                player.playSound(player,Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f)
                // 実行中であるため最後まで提起する必要であるという旨を表示
                player.sendTitle(languages("name"), languages("messages.please_wait_for_end"), 0, 100, 20)
                // スペクテイターに
                player.gameMode = GameMode.SPECTATOR
            } else {
                // if:途中抜けだった場合
                // 全員に復帰した旨を知らせる
                event.joinMessage = languages("messages.player_rejoined", "%player%" to player.name).asPrefixed()
            }
        } else {
            // if:実行中ではない場合
            // プレイヤーに待機中のボスバーを表示
            WereWolf3.BOSSBAR.addPlayer(player)
            // プレイヤーにサイドバーを表示
            player.sidebar = WaitingSidebar()
            WereWolf3.PLAYERS.forEach { p ->
                val sidebar = p.sidebar
                if(sidebar !is WaitingSidebar) { return@forEach }
                sidebar.players(WereWolf3.PLAYERS.size)
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