package dev.mr3n.werewolf3

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import dev.moru3.minepie.config.Config
import dev.mr3n.werewolf3.Status.*
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.commands.Start
import dev.mr3n.werewolf3.items.*
import dev.mr3n.werewolf3.items.doctor.DoctorSword
import dev.mr3n.werewolf3.items.seer.SeerItem
import dev.mr3n.werewolf3.items.wolf.BombBall
import dev.mr3n.werewolf3.items.wolf.LightningRod
import dev.mr3n.werewolf3.items.wolf.WolfAxe
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.RunningSidebar
import dev.mr3n.werewolf3.sidebar.StartingSidebar
import dev.mr3n.werewolf3.sidebar.WaitingSidebar
import dev.mr3n.werewolf3.utils.*
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class WereWolf3: JavaPlugin() {
    override fun onEnable() {
        CONFIG.saveDefaultConfig()
        LANGUAGES.saveDefaultConfig()
        // ゲームルールを設定
        Bukkit.getWorlds()[0].apply {
            setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
            setGameRule(GameRule.DO_WEATHER_CYCLE, false)
            setGameRule(GameRule.KEEP_INVENTORY, false)
        }
        // いつもの
        Bukkit.getPluginManager().registerEvents(PlayerListener,this)
        // すでにサーバーにいるプレイヤーのjoin eventを発生させる(初期化用)
        Bukkit.getOnlinePlayers().forEach { PlayerListener.onJoin(PlayerJoinEvent(it,null)) }
        this.getCommand("start")?.also {
            it.setExecutor(Start)
            it.tabCompleter = Start
        }

        this.getCommand("debug")?.also {
            it.setExecutor { sender, command, label, args ->
                if(sender !is Player) { return@setExecutor true  }
                when(args.getOrNull(0)) {
                    "test1" -> {
                        sender.inventory.addItem(DoctorSword.itemStack)
                    }
                    "test2" -> {
                        sender.inventory.addItem(SeerItem.itemStack)
                    }
                    "test3" -> {
                        sender.inventory.addItem(BombBall.itemStack)
                    }
                    "test4" -> {
                        sender.inventory.addItem(WolfAxe.itemStack)
                    }
                    "test5" -> {
                        sender.inventory.addItem(StanBall.itemStack)
                    }
                    "test6" -> {
                        sender.inventory.addItem(GlowInk.itemStack)
                    }
                    "test7" -> {
                        sender.inventory.addItem(LightningRod.itemStack)
                    }
                }
                true
            }
        }



        // 毎tickループ
        TickTask.task {
            when(STATUS) {
                // 待機中にループする処理
                WAITING -> {
                    // 点滅速度
                    if(it% Constants.POINT_FLUSH_SPEED!=0) {
                        // ...の.の数を計算
                        val loadingDots = ".".repeat((it%(Constants.POINT_FLUSH_SPEED*4))/ Constants.POINT_FLUSH_SPEED)
                        // bossbarに...のアニメーションを追加
                        BOSSBAR.setTitle(languages("messages.please_wait_for_start") +loadingDots)
                        PLAYERS.forEach { player ->
                            val sidebar = player.sidebar
                            // プレイヤーのサイドバーがWaitingSidebarの場合
                            if(sidebar is WaitingSidebar) {
                                // 待機中l...の..にアニメーションを付与
                                sidebar.status(languages("sidebar.global.status.waiting") +loadingDots)
                            }
                        }
                    }
                }
                STARTING -> {
                    // 残り時間を減らす
                    TIME_LEFT--
                    PLAYERS.forEach { player ->
                        val sidebar = player.sidebar
                        // プレイヤーのサイドバーがStartingSidebarではない場合はreturn
                        if(sidebar !is StartingSidebar) { return@forEach }
                        // サイドバーの推定プレイヤー数を更新
                        sidebar.players(PLAYERS.size)
                        // サイドバーの残り時間を更新
                        sidebar.time(TIME_LEFT/20)
                    }
                    // 準備時間が終わったらゲーム開始
                    if(TIME_LEFT<=0) { GameInitializer.run() }
                }
                RUNNING -> {
                    // 残り時間を減らす
                    TIME_LEFT--
                    // 時間が来たら朝/夜反転
                    if(TIME_LEFT<=0) { TIME = TIME.next() }
                    // ボスバーの進行度を現在の残り時間に合わせる
                    BOSSBAR.progress = TIME_LEFT * (1.0 / TIME_LENGTH)
                    // ボスバーのタイトルにタイマーを表示
                    BOSSBAR.setTitle(languages("bossbar.title","%time%" to TIME.displayName, "%emoji%" to TIME.emoji, "%time_left%" to (TIME_LEFT / 20).parseTime()))
                    PLAYERS.forEach { player ->
                        // プレイヤーのヘルメットを取得
                        val helmet = player.inventory.helmet
                        // ヘルメットのCoの役職を取得。nullだった場合はreturn
                        val coRole = helmet?.getContainerValue(Role.HELMET_ROLE_TAG_KEY, Role.RoleTagType)
                        // まだCoしていない役職だった場合
                        if(player.co!=coRole) {
                            if(coRole==null) {
                                player.setDisplayName(player.name)
                                player.setPlayerListName(player.name)
                                // 何をcoしたかをほぞん
                                player.co = null
                            } else {
                                // すべてのプレイヤーにCoした旨を伝える。
                                PLAYERS.forEach { it.sendMessage(languages("messages.coming_out", "%color%" to coRole.color, "%player%" to player.name, "%role%" to coRole.displayName)) }
                                // プレイヤーのprefixにCoした役職を表示
                                player.setDisplayName("${coRole.color}[${coRole.displayName}Co]${player.name}")
                                player.setPlayerListName("${coRole.color}[${coRole.displayName}Co]${player.name}")
                                // 何をcoしたかをほぞん
                                player.co = coRole
                            }
                        }
                        val sidebar = player.sidebar
                        if(sidebar is RunningSidebar) {
                            sidebar.players(REMAINING_PLAYER_PRED)
                            sidebar.money(player.money)
                        }
                        if(it%(20*30)==0) {
                            player.money += Constants.ADD_MONEY
                        }
                    }
                    // 生きているプレイヤー一覧(スペクテイターじゃないプレイヤー)
                    val alivePlayers = WereWolf3.PLAYERS.filter { p->p.gameMode!=GameMode.SPECTATOR }
                    if(alivePlayers.count { p->p.role?.faction==Role.Faction.WOLF }<=0) {
                        // 人狼陣営の数が0になった場合ゲームを終了
                        GameTerminator.end(Role.Faction.VILLAGER, languages("title.win.reason.anni", "%role%" to Role.Faction.WOLF.displayName))
                    } else if(alivePlayers.count { p->p.role?.faction==Role.Faction.VILLAGER }<=0) {
                        // 村人陣営の数が0になった場合ゲームを終了
                        GameTerminator.end(Role.Faction.WOLF, languages("title.win.reason.anni", "%role%" to Role.Faction.VILLAGER.displayName))
                    }
                }
                ENDING -> {}
            }
        }
    }

    override fun onDisable() {
        // ボスバーを削除
        BOSSBAR.removeAll()
        // すべての死体を削除
        DeadBody.DEAD_BODIES.forEach { it.destroy() }
        // ゲームが起動中の場合停止
        if(running) {
            GameTerminator.run()
        }
    }

    // インスタンスを公開変数に保存する
    init { INSTANCE = this }

    companion object {
        // 上に常時表示しているボスバー
        val BOSSBAR: BossBar by lazy { Bukkit.createBossBar(languages("messages.please_wait_for_start"), BarColor.RED, BarStyle.SOLID) }
        // 現在実行中のゲームID
        var GAME_ID: String? = null
        // 現在のゲームステータス
        var STATUS: Status = WAITING
        // 残り時間
        var TIME_LEFT = 0
        // 残り時間の長さ(カウントが減らされる前の長さ)
        var REMAINING_PLAYER_PRED = 0
        var TIME_LENGTH = 0
        var DAY: Int = 0
        val PLAYERS = mutableListOf<Player>()

        // 現在の時刻(朝/夜)
        var TIME: Time = Time.DAY
            set(time) {
                field = time
                // 朝/夜の変更の処理を実行
                time()
            }

        // PROTOCOL_LIBのマネージャー
        val PROTOCOL_MANAGER: ProtocolManager by lazy { ProtocolLibrary.getProtocolManager() }

        // ゲームが実行中かどうかを true/falseで
        val running: Boolean
            get() = STATUS != WAITING
        // WereWolf3のインスタンス
        lateinit var INSTANCE: WereWolf3
            private set
        // languages.ymlファイル
        val LANGUAGES: Config by lazy { Config(INSTANCE,"languages.yml") }
        // config.ymlファイル
        val CONFIG: Config by lazy { Config(INSTANCE,"config.yml") }

        /**
         * 人狼を開始する関数です。
         * locationはスポーン地点。
         */
        fun start(location: Location) {
            GameInitializer.start(location)
        }
    }
}