package com.jingyu233.bluetoothhook.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.jingyu233.bluetoothhook.data.model.VirtualDevice

/**
 * Room数据库
 */
@Database(
    entities = [VirtualDevice::class],
    version = 2,  // 版本升级到2
    exportSchema = false
)
abstract class VirtualDeviceDatabase : RoomDatabase() {

    abstract fun virtualDeviceDao(): VirtualDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: VirtualDeviceDatabase? = null

        /**
         * 数据库迁移：v1 -> v2
         * 添加 scanResponseHex 和 useExtendedAdvertising 字段
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 scanResponseHex 列，默认为空字符串
                database.execSQL(
                    "ALTER TABLE virtual_devices ADD COLUMN scanResponseHex TEXT NOT NULL DEFAULT ''"
                )

                // 添加 useExtendedAdvertising 列，默认为0 (false)
                database.execSQL(
                    "ALTER TABLE virtual_devices ADD COLUMN useExtendedAdvertising INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): VirtualDeviceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VirtualDeviceDatabase::class.java,
                    "bluetooth_hook.db"
                )
                    .addMigrations(MIGRATION_1_2)  // 添加迁移
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
