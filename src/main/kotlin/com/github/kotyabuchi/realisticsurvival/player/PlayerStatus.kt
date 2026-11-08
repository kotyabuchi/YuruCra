package com.github.kotyabuchi.realisticsurvival.player

import com.github.kotyabuchi.realisticsurvival.job.JobType
import org.bukkit.Material
import java.util.EnumMap
import java.util.UUID

/** プレイヤーごとの保存データ。メインスレッドからだけ触ること */
class PlayerStatus(
    val uuid: UUID,
    jobTotalExp: Map<JobType, Double>,
    val resourceStorage: ResourceStorage,
    homes: List<Home>,
) {
    private val jobs = EnumMap<JobType, JobStatus>(JobType::class.java)
    val homes: MutableList<Home> = homes.toMutableList()

    init {
        jobTotalExp.forEach { (type, exp) -> jobs[type] = JobStatus().apply { setTotalExp(exp) } }
    }

    fun job(type: JobType): JobStatus = jobs.getOrPut(type) { JobStatus() }

    /** DB保存用に、その時点の値をコピーする（保存は別スレッドで行うため） */
    fun snapshot(): PlayerSnapshot = PlayerSnapshot(
        uuid = uuid,
        jobTotalExp = JobType.entries.associateWith { job(it).totalExp },
        storageSlotSize = resourceStorage.slotSize,
        storedResources = resourceStorage.stored.toMap(),
    )
}

data class PlayerSnapshot(
    val uuid: UUID,
    val jobTotalExp: Map<JobType, Double>,
    val storageSlotSize: Int,
    val storedResources: Map<Material, Int>,
)
