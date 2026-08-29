package com.simpleclock.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val ALARM_DEFAULT_COLOR = 0xFFFF5252L

val ALARM_COLORS: List<Long> = listOf(
    0xFFFF5252L, // 亮紅
    0xFFFF6E40L, // 亮橙
    0xFFFFAB00L, // 琥珀黃
    0xFFFFD600L, // 鮮黃
    0xFFAEEA00L, // 萊姆綠
    0xFF64DD17L, // 鮮綠
    0xFF00C853L, // 翠綠
    0xFF00BFA5L, // 碧綠
    0xFF00B0FFL, // 水藍
    0xFF2979FFL, // 亮藍
    0xFF304FFEL, // 寶藍
    0xFF651FFFL, // 靛紫
    0xFFAA00FFL, // 亮紫
    0xFFD500F9L, // 洋紅
    0xFFFF1744L, // 緋紅
    0xFFFF4081L, // 桃粉
    0xFFFF80ABL, // 櫻花粉
    0xFF8D6E63L, // 摩卡褐
    0xFF607D8BL, // 石板藍
    0xFF455A64L, // 深灰藍
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Int = 0,
    val enabled: Boolean = true,
    val color: Long = ALARM_DEFAULT_COLOR,
)

