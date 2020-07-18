package org.y20k.escapepod.database

import androidx.room.*

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episode")
    fun getAll(): List<EpisodeEntity>

    @Query("SELECT * FROM episode WHERE eid IN (:eids)")
    fun loadAllByIds(eids: IntArray): List<EpisodeEntity>

    @Query("SELECT * FROM episode WHERE episode_title LIKE :title LIMIT 1")
    fun findByTitle(title: String): EpisodeEntity

    @Query("SELECT * FROM episode WHERE episode_guid IS :guid LIMIT 1")
    fun findByGuid(guid: String): EpisodeEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(episode: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg episodes: EpisodeEntity)

    @Update
    fun add(episode: EpisodeEntity)

    @Delete
    fun delete(episode: EpisodeEntity)
}