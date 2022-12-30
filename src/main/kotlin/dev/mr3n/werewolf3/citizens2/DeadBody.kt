package dev.mr3n.werewolf3.citizens2

import com.comphenix.protocol.wrappers.WrappedGameProfile
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.trait.SkinTrait
import net.citizensnpcs.trait.SleepTrait
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.concurrent.CopyOnWriteArrayList

class DeadBody(val player: Player) {
    // 死体のNPC
    val npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.name)

    init {
        // プレイヤーを寝た状態にする
        npc.getOrAddTrait(SleepTrait::class.java).setSleeping(player.location.clone())
        // プレイヤーのスキン設定用のtraitを取得
        val skinTrait = npc.getOrAddTrait(SkinTrait::class.java)
        // スキンを取得するためのプレイヤーのゲームプロフィールを取得
        val gameProfile = WrappedGameProfile.fromPlayer(player)
        // スキンが格納されているtextures変数を取得
        val textures = gameProfile.properties["textures"].toList()[0]
        // 死体のスキンをプレイヤーのスキンに設定。
        skinTrait.setSkinPersistent("${player.name}'s skin", textures.signature, textures.value)
        // 死体一覧にしたいを追加
        DEAD_BODIES.add(this)
        // 死体をスポーン
        npc.spawn(player.location)
    }

    /**
     * 死体を消します。
     */
    fun destroy() {
        // 死体を削除
        npc.despawn()
        npc.destroy()
        // 一覧からも削除
        DEAD_BODIES.remove(this)
        CitizensAPI.getNPCRegistry().deregister(npc)
    }

    companion object {
        // 現在存在している死体の一覧です。
        val DEAD_BODIES = CopyOnWriteArrayList<DeadBody>()
    }
}