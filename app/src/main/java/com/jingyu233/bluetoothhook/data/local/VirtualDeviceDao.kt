package com.jingyu233.bluetoothhook.data.local

import androidx.room.*
import com.jingyu233.bluetoothhook.data.model.VirtualDevice
import kotlinx.coroutines.flow.Flow

/**
 * 虚拟设备数据访问对象
 */
@Dao
interface VirtualDeviceDao {

    @Query("SELECT * FROM virtual_devices ORDER BY createdAt DESC")
    fun getAllDevices(): Flow<List<VirtualDevice>>

    @Query("SELECT * FROM virtual_devices ORDER BY createdAt DESC")
    suspend fun getAllDevicesSnapshot(): List<VirtualDevice>

    @Query("SELECT * FROM virtual_devices WHERE enabled = 1 ORDER BY createdAt DESC")
    fun getEnabledDevices(): Flow<List<VirtualDevice>>

    @Query("SELECT * FROM virtual_devices WHERE id = :id")
    suspend fun getDeviceById(id: String): VirtualDevice?

    @Query("SELECT COUNT(*) FROM virtual_devices")
    suspend fun getDeviceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: VirtualDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<VirtualDevice>)

    @Update
    suspend fun updateDevice(device: VirtualDevice)

    @Delete
    suspend fun deleteDevice(device: VirtualDevice)

    @Query("DELETE FROM virtual_devices")
    suspend fun deleteAllDevices()

    @Query("DELETE FROM virtual_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: String)

    @Query("UPDATE virtual_devices SET enabled = :enabled WHERE id = :id")
    suspend fun updateDeviceEnabled(id: String, enabled: Boolean)
}
