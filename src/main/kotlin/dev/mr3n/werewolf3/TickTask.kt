package dev.mr3n.werewolf3

import dev.moru3.minepie.Executor.Companion.runTaskTimer

object TickTask {
    var loopCount = 0
    private val tasks = mutableListOf<(Int)->Unit>()
    // ループタスクを追加
    fun task(task: (Int)->Unit) {
        tasks.add(task)
    }
    init {
        WereWolf3.INSTANCE.runTaskTimer(1,1) {
            loopCount++
            tasks.forEach { it(loopCount) }
        }
    }
}