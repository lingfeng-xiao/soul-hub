package com.lingfeng.sprite.sensor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.PerceptionSystem.ContextType;
import com.lingfeng.sprite.PerceptionSystem.EnvironmentPerception;
import com.lingfeng.sprite.PerceptionSystem.Perception;
import com.lingfeng.sprite.PerceptionSystem.Sensor;

/**
 * S5-2: 真实环境传感器增强
 *
 * 基于时间、系统信息和位置推断环境状态
 */
public class RealEnvironmentSensor implements Sensor {

    // 时区
    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public Perception perceive() {
        Instant now = Instant.now();
        int hour = now.atZone(TIMEZONE).getHour();
        int dayOfWeek = now.atZone(TIMEZONE).getDayOfWeek().getValue();

        // 基于时间的上下文推断
        ContextType context = inferContext(hour, dayOfWeek);

        return new Perception(
                now,
                null,
                null,
                new EnvironmentPerception(
                        now,
                        hour,
                        dayOfWeek,
                        context
                ),
                null, null, null
        );
    }

    /**
     * S5-2: 基于时间、星期和月份推断上下文
     */
    private ContextType inferContext(int hour, int dayOfWeek) {
        Instant now = Instant.now();
        int month = now.atZone(TIMEZONE).getMonthValue();
        boolean isWeekend = dayOfWeek == 6 || dayOfWeek == 7;
        boolean isHoliday = isHoliday(now);

        // 工作日模式
        if (!isWeekend && !isHoliday) {
            return inferWorkdayContext(hour);
        }

        // 周末/假期模式
        return inferLeisureContext(hour);
    }

    /**
     * S5-2: 工作日上下文推断
     */
    private ContextType inferWorkdayContext(int hour) {
        if (hour >= 6 && hour <= 7) {
            return ContextType.RITUAL;  // 晨间习惯
        }
        if (hour >= 8 && hour <= 9) {
            return ContextType.COMMUTE;  // 通勤
        }
        if (hour >= 10 && hour <= 12) {
            return ContextType.WORK;  // 高效工作时段
        }
        if (hour >= 12 && hour <= 13) {
            return ContextType.MEAL;  // 午餐
        }
        if (hour >= 14 && hour <= 17) {
            return ContextType.WORK;  // 下午工作
        }
        if (hour >= 18 && hour <= 19) {
            return ContextType.COMMUTE;  // 晚通勤
        }
        if (hour >= 20 && hour <= 22) {
            return ContextType.LEISURE;  // 晚间休闲
        }
        return ContextType.SLEEP;  // 睡眠
    }

    /**
     * S5-2: 周末/假期上下文推断
     */
    private ContextType inferLeisureContext(int hour) {
        if (hour >= 7 && hour <= 9) {
            return ContextType.RITUAL;  // 晨间习惯
        }
        if (hour >= 9 && hour <= 11) {
            return ContextType.LEISURE;  // 上午休闲
        }
        if (hour >= 11 && hour <= 13) {
            return ContextType.MEAL;  // 午餐
        }
        if (hour >= 13 && hour <= 17) {
            return ContextType.LEISURE;  // 下午活动
        }
        if (hour >= 17 && hour <= 19) {
            return ContextType.MEAL;  // 晚餐
        }
        if (hour >= 19 && hour <= 22) {
            return ContextType.LEISURE;  // 晚间休闲
        }
        return ContextType.SLEEP;  // 睡眠
    }

    /**
     * S5-2: 判断是否是节假日（中国法定节日简单判断）
     */
    private boolean isHoliday(Instant date) {
        int month = date.atZone(TIMEZONE).getMonthValue();
        int day = date.atZone(TIMEZONE).getDayOfMonth();

        // 简单节假日判断（实际应用中应该使用更精确的节假日数据）
        // 元旦
        if (month == 1 && day == 1) return true;
        // 春节（简单判断：1月21-27日范围）
        if (month == 1 && day >= 21 && day <= 27) return true;
        // 清明
        if (month == 4 && day >= 4 && day <= 6) return true;
        // 劳动节
        if (month == 5 && day >= 1 && day <= 3) return true;
        // 国庆节
        if (month == 10 && day >= 1 && day <= 7) return true;

        // 端午节（简单判断）
        if (month == 6 && day >= 22 && day <= 24) return true;

        // 中秋节（简单判断）
        if (month == 9 && day >= 29 && day <= 30) return true;

        return false;
    }

    @Override
    public String name() {
        return "RealEnvironmentSensor";
    }
}
