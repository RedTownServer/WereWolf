package dev.mr3n.werewolf3

import dev.moru3.minepie.item.EasyItem
import dev.mr3n.werewolf3.items.quickchat.TrustYou
import dev.mr3n.werewolf3.items.quickchat.WithYou
import dev.mr3n.werewolf3.items.quickchat.YouAreWolf
import dev.mr3n.werewolf3.protocol.DeadBody
import dev.mr3n.werewolf3.protocol.MetadataPacketUtil
import dev.mr3n.werewolf3.protocol.TeamPacketUtil
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.RunningSidebar
import dev.mr3n.werewolf3.sidebar.StartingSidebar
import dev.mr3n.werewolf3.utils.*
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.persistence.PersistentDataType
import java.security.SecureRandom
import java.util.*

object GameInitializer {

    /**
     * ゲームを初期化し、試合を開始します。
     */
    fun start(location: Location) {
        if(WereWolf3.running) { return }
        // 死体を全削除
        DeadBody.DEAD_BODIES.forEach { it.destroy() }
        // ゲームIDを設定。
        WereWolf3.GAME_ID = UUID.randomUUID().toString()
        // ゲームのステータスを設定、
        WereWolf3.STATUS = Status.STARTING
        // 時間を朝に
        location.world!!.time = 0L
        // 日にちを0に設定
        WereWolf3.DAY = 0
        // 参加プレイヤー一覧。
        val players = Bukkit.getOnlinePlayers().toList()
        // プレイヤーにゲームIDを設定。
        players.forEach { player -> player.gameId = WereWolf3.GAME_ID }
        // プレイヤー人数から役職数を推定してリストに格納。 roleList.length == players.length
        val roleList = Role.values().map { role -> MutableList(role.calc(players.size)) { role } }.flatten().shuffled(SecureRandom.getInstance("SHA1PRNG"))
        // TODO カメラアニメーションをつける
        // 役職リストとプレイヤーのリストを合体してfor
        players.zip(roleList).toMap().forEach { (player, role) ->
            // プレイヤーの役職を設定。
            player.role = role
            player.co = null
            // 開始場所にテレポート。
            val tc = (0..100)
            player.teleport(location.clone().add(tc.random()/100.0,0.0,tc.random()/100.0))
            // タイトルに自分の役職を表示。
            player.sendTitle(languages("title.start.title", "%time%" to (Constants.STARTING_TIME / 20), "%role%" to "${role.color}${ChatColor.BOLD}${role.displayName}"),languages("title.start.subtitle", "%time%" to Constants.STARTING_TIME / 20, "%role%" to "${role.color}${ChatColor.BOLD}${role.displayName}"), 0, 100, 20)
            // 怖い音を鳴らす。
            repeat(10) { player.playSound(player,Sound.AMBIENT_NETHER_WASTES_MOOD,1F,1F) }
            player.sidebar = StartingSidebar(player)
            player.flySpeed = 0.2f
            player.walkSpeed = 0.2f
            player.money = Constants.START_MONEY
            TeamPacketUtil.add(player,ChatColor.WHITE,players)
            player.setDisplayName(player.name)
            player.setPlayerListName(player.name)
            player.gameMode = GameMode.ADVENTURE
            player.kills = intArrayOf()
            player.inventory.contents.filterNotNull().forEach { it.amount = 0 }
        }
        // 時間を設定
        WereWolf3.TIME_LEFT = Constants.STARTING_TIME

        val wolfs = players.filter { it.role==Role.WOLF }

        wolfs.forEach { player ->
            wolfs.forEach { wolf ->
                // 人狼チームからは身内が発光しているように
                MetadataPacketUtil.addToGlowing(player,wolf)
            }
            // 人狼チームからは身内が赤く見えるように
            TeamPacketUtil.add(player,ChatColor.DARK_RED,wolfs)
            player.sendMessage(languages("messages.wolfs", "%wolfs%" to wolfs.joinToString(" ") { it.name }).asPrefixed())
        }

        WereWolf3.PLAYERS_EST = players.size

        WereWolf3.PLAYERS.addAll(players)
    }

    /**
     * 役職発表など準備完了後に行う処理
     */
    fun run() {
        val wolfs = WereWolf3.PLAYERS.filter { it.role?.team == Role.Team.WOLF }
        WereWolf3.PLAYERS.forEach {  player ->
            player.inventory.setItem(8,
                EasyItem(Material.AMETHYST_SHARD, languages("item.shop.open.name"), languages("item.shop.open.description").split("\n")).also { item ->
                    item.setContainerValue(Keys.ITEM_TYPE, PersistentDataType.STRING, ShopMenu.SHOP_ID)
                }
            )
            // クイックチャットのアイテムを渡す。
            player.inventory.setItem(5, WithYou.itemStack)
            player.inventory.setItem(6, TrustYou.itemStack)
            player.inventory.setItem(7, YouAreWolf.itemStack)
            // 弓を渡す。
            player.inventory.addItem(EasyItem(Material.BOW).also { itemStack -> itemStack.itemMeta = itemStack.itemMeta?.also { itemMeta ->
                itemMeta.addEnchant(Enchantment.ARROW_INFINITE,1,true)
                itemMeta.isUnbreakable = true
            } })
            // 石の剣を渡す。
            player.inventory.addItem(EasyItem(Material.WOODEN_SWORD).also { itemStack -> itemStack.itemMeta = itemStack.itemMeta?.also { itemMeta -> itemMeta.isUnbreakable = true } })
            // 矢を渡す。
            player.inventory.addItem(EasyItem(Material.ARROW))
            Role.values().forEachIndexed { index, role -> player.inventory.setItem(9+index, role.helmet) }
            player.sendMessage(languages("title.start.messages.info", "%wolf_teams%" to wolfs.size, "%villager_teams%" to WereWolf3.PLAYERS.size - wolfs.size))
            player.sidebar = RunningSidebar(player)
        }
        WereWolf3.STATUS = Status.RUNNING
        WereWolf3.TIME = Time.DAY
    }
}