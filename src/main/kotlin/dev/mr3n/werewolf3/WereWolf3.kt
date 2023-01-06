package dev.mr3n.werewolf3

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import dev.moru3.minepie.config.Config
import dev.mr3n.werewolf3.Status.*
import dev.mr3n.werewolf3.commands.EndCommand
import dev.mr3n.werewolf3.commands.ShopCommand
import dev.mr3n.werewolf3.commands.StartCommand
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.protocol.DeadBody
import dev.mr3n.werewolf3.protocol.SpectatorPacketUtil
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.RunningSidebar
import dev.mr3n.werewolf3.sidebar.StartingSidebar
import dev.mr3n.werewolf3.sidebar.WaitingSidebar
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.parseTime
import dev.mr3n.werewolf3.utils.role
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
            setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
            setGameRule(GameRule.DO_MOB_SPAWNING, false)
        }
        // いつもの
        Bukkit.getPluginManager().registerEvents(PlayerListener,this)
        // すでにサーバーにいるプレイヤーのjoin eventを発生させる(初期化用)
        Bukkit.getOnlinePlayers().forEach { PlayerListener.onJoin(PlayerJoinEvent(it,null)) }
        this.getCommand("start")?.also {
            it.setExecutor(StartCommand)
            it.tabCompleter = StartCommand
        }
        this.getCommand("end")?.also {
            it.setExecutor(EndCommand)
            it.tabCompleter = EndCommand
        }
        this.getCommand("shop")?.also {
            it.setExecutor(ShopCommand)
            it.tabCompleter = ShopCommand
        }
        // >>> クラスの初期化 >>>
        IShopItem.ShopItem.ITEMS
        SpectatorPacketUtil.init()
        GameTerminator.init()
        Role.ROLES
        DeadBody.DEAD_BODIES
        Time.DAY
        // <<< クラスの初期化 <<<
        this.getCommand("debug")?.also {
            it.setExecutor { sender, _, _, args ->
                if(sender !is Player) { return@setExecutor true  }
                when(args.getOrNull(0)) {
                    "test1" -> {
                        sender.inventory.addItem(IShopItem.ShopItem.ITEMS_BY_ID[args.getOrNull(1)]?.itemStack)
                    }
                    "test2" -> {
                        val players = args.getOrNull(1)?.toIntOrNull()?:return@setExecutor true
                        val result = Role.values().associateWith { it.calc(players) }
                        val wolfTeams = result.filter { it.key.team==Role.Team.WOLF }
                        sender.sendMessage(result.mapKeys { it.key.displayName }.toString())
                        sender.sendMessage("人狼陣営: ${wolfTeams.map { it.value }.sum()}, 村人陣営: ${players - wolfTeams.map { it.value }.sum()}")
                    }
                }
                true
            }
        }

        // 毎tickループ
        TickTask.task { loopCount ->
            when(STATUS) {
                // 待機中にループする処理
                WAITING -> {
                    // 点滅速度
                    if(loopCount % Constants.POINT_FLUSH_SPEED!=0) {
                        // ...の.の数を計算
                        val loadingDots = ".".repeat((loopCount%(Constants.POINT_FLUSH_SPEED*4))/ Constants.POINT_FLUSH_SPEED)
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
                    GameRunner.running(loopCount = loopCount)
                    // 生きているプレイヤー一覧(スペクテイターじゃないプレイヤー)
                    val alivePlayers = PLAYERS.filter { p->p.gameMode!=GameMode.SPECTATOR }
                    if(alivePlayers.count { p->p.role?.team==Role.Team.WOLF }<=0) {
                        // 人狼陣営の数が0になった場合ゲームを終了
                        GameTerminator.end(Role.Team.VILLAGER, languages("title.win.reason.anni", "%role%" to Role.Team.WOLF.displayName))
                    } else if(alivePlayers.count { p->p.role?.team==Role.Team.VILLAGER }<=0) {
                        // 村人陣営の数が0になった場合ゲームを終了
                        GameTerminator.end(Role.Team.WOLF, languages("title.win.reason.anni", "%role%" to Role.Team.VILLAGER.displayName))
                    }
                }
                ENDING -> {}
            }
        }
    }

    override fun onDisable() {
        // ゲームが起動中の場合停止
        if(running) {
            GameTerminator.run(true)
        }
        // ボスバーを削除
        BOSSBAR.removeAll()
        // すべての死体を削除
        DeadBody.DEAD_BODIES.forEach { it.destroy() }
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
        var PLAYERS_EST = 0
            set(value) {
                PLAYERS.forEach { player ->
                    val sidebar = player.sidebar
                    if(sidebar is RunningSidebar) { sidebar.playersEst(value) }
                }
                field = value
            }
        var TIME_LENGTH = 0
        var DAY: Int = 0
        val PLAYERS = mutableListOf<Player>()
        val PLAYER_BY_ENTITY_ID: MutableMap<Int, Player> = mutableMapOf()
        val isPlugmanLoaded: Boolean by lazy { Bukkit.getPluginManager().isPluginEnabled("PlugManX") }

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