package com.offlinenotifier.app.shift

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ShiftCalculator {

    enum class ShiftType(val code: String, val label: String, val emoji: String) {
        NGAY("NGAY", "Ngày", "☀️"),
        DEM("DEM", "Đêm", "🌙"),
        NGHI("NGHI", "Nghỉ", "😴")
    }

    data class CrewInfo(
        val id: String,
        val name: String,
        val offset: Int
    )

    // 12-day cycle: 4 Day, 2 Off, 4 Night, 2 Off
    private val CYCLE = arrayOf(
        ShiftType.NGAY, ShiftType.NGAY, ShiftType.NGAY, ShiftType.NGAY,
        ShiftType.NGHI, ShiftType.NGHI,
        ShiftType.DEM, ShiftType.DEM, ShiftType.DEM, ShiftType.DEM,
        ShiftType.NGHI, ShiftType.NGHI
    )

    // Anchor date: January 3, 2026
    private val ANCHOR_DATE = LocalDate.of(2026, 1, 3)
    private val ANCHOR_JDN = toJulianDayNumber(ANCHOR_DATE)

    // Crew definitions with offsets (HC removed as per user preference)
    val CREWS = listOf(
        CrewInfo("A", "Kíp A", 0),
        CrewInfo("B", "Kíp B", 8),
        CrewInfo("C", "Kíp C", 4)
    )

    data class ShiftInfo(
        val type: ShiftType,
        val isHoliday: Boolean,
        val holidayName: String?
    )

    data class HOStats(
        val total: Int,
        val remaining: Int
    )

    /**
     * Kiểm tra Thứ Bảy có phải ngày HO hay không
     */
    private fun isSaturdayHO(date: LocalDate): Boolean {
        if (date.dayOfWeek.value != 6) return false
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth

        return when {
            y > 2026 -> false
            y == 2026 -> {
                (m == 1 && (d == 3 || d == 17)) ||
                (m == 2 && d == 14) ||
                (m == 3 && (d == 14 || d == 28)) ||
                (m == 4 && (d == 11 || d == 25)) ||
                (m == 5 && (d == 9 || d == 23)) ||
                (m == 6 && (d == 6 || d == 20)) ||
                (m == 7 && d == 4) ||
                (m == 8 && (d == 15 || d == 29)) ||
                (m == 9 && (d == 12 || d == 26)) ||
                (m == 10 && d == 24) ||
                (m == 11 && d == 7) ||
                (m == 12 && d == 5)
            }
            else -> {
                val nthSaturday = (d - 1) / 7 + 1
                nthSaturday == 1 || nthSaturday == 3
            }
        }
    }

    /**
     * Tính toán thống kê số ngày HO của một kíp trong năm.
     */
    fun getHOStatsForYear(crewId: String, year: Int, today: LocalDate): HOStats {
        var total = 0
        var remaining = 0
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)
        
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            val actualShift = getActualShift(crewId, curr)
            
            val isSunday = curr.dayOfWeek.value == 7
            val isSatWorkHO = isSaturdayHO(curr)

            val isHOReal = (isSunday || isSatWorkHO) && actualShift != ShiftType.NGHI

            if (isHOReal) {
                total++
                if (!curr.isBefore(today)) {
                    remaining++
                }
            }
            curr = curr.plusDays(1)
        }
        return HOStats(total, remaining)
    }

    /**
     * Get the shift type for a specific crew on a specific date.
     */
    fun getShift(crewId: String, date: LocalDate): ShiftType {
        val crew = CREWS.find { it.id == crewId } ?: return ShiftType.NGHI
        val targetJDN = toJulianDayNumber(date)
        val diff = targetJDN - ANCHOR_JDN
        val holidayAdj = countHolidaysBetween(ANCHOR_DATE, date)
        val idx = mod(crew.offset + diff - holidayAdj, CYCLE.size)
        return CYCLE[idx]
    }

    // Bảng ca trực lễ năm 2026 thực tế từ ảnh
    private val REAL_2026_HOLIDAYS = mapOf(
        Pair(1, 1) to mapOf("A" to ShiftType.DEM, "B" to ShiftType.NGHI, "C" to ShiftType.NGAY),
        Pair(4, 26) to mapOf("A" to ShiftType.NGAY, "B" to ShiftType.NGHI, "C" to ShiftType.DEM),
        Pair(4, 30) to mapOf("A" to ShiftType.DEM, "B" to ShiftType.NGAY, "C" to ShiftType.NGHI),
        Pair(5, 1) to mapOf("A" to ShiftType.NGHI, "B" to ShiftType.NGAY, "C" to ShiftType.DEM),
        Pair(9, 1) to mapOf("A" to ShiftType.NGAY, "B" to ShiftType.DEM, "C" to ShiftType.NGHI),
        Pair(9, 2) to mapOf("A" to ShiftType.NGAY, "B" to ShiftType.NGHI, "C" to ShiftType.DEM),
        Pair(11, 24) to mapOf("A" to ShiftType.NGHI, "B" to ShiftType.NGAY, "C" to ShiftType.DEM),
        Pair(12, 31) to mapOf("A" to ShiftType.NGAY, "B" to ShiftType.DEM, "C" to ShiftType.NGHI)
    )

    /**
     * Get the actual shift type for a specific crew on a specific date,
     * taking official holidays into account.
     */
    fun getActualShift(crewId: String, date: LocalDate): ShiftType {
        if (isLunarNewYear(date)) return ShiftType.NGHI

        if (isHoliday(date)) {
            val y = date.year
            val m = date.monthValue
            val d = date.dayOfMonth

            // 1. Nếu là năm 2026, sử dụng bảng ca trực lễ thực tế
            if (y == 2026) {
                val dayMonth = Pair(m, d)
                val holidayMap = REAL_2026_HOLIDAYS[dayMonth]
                if (holidayMap != null) {
                    return holidayMap[crewId] ?: ShiftType.NGHI
                }
            }

            // 2. Thuật toán tổng quát cho các năm khác
            val prevDay = date.minusDays(1)
            val nextDay = date.plusDays(1)

            val isPrevHol = isHoliday(prevDay) && !isLunarNewYear(prevDay)
            val isNextHol = isHoliday(nextDay) && !isLunarNewYear(nextDay)

            return when {
                isNextHol -> {
                    // Đây là ngày 1 của cụm lễ kép 2 ngày
                    val baseShifts = mapOf(
                        "A" to getShift("A", date),
                        "B" to getShift("B", date),
                        "C" to getShift("C", date)
                    )
                    val dayCrew = baseShifts.entries.firstOrNull { it.value == ShiftType.NGAY }?.key ?: "A"
                    val nightCrew = baseShifts.entries.firstOrNull { it.value == ShiftType.DEM }?.key ?: "B"
                    val offCrew = baseShifts.entries.firstOrNull { it.value == ShiftType.NGHI }?.key ?: "C"

                    when (crewId) {
                        offCrew -> ShiftType.NGAY
                        dayCrew -> ShiftType.DEM
                        nightCrew -> ShiftType.NGHI
                        else -> ShiftType.NGHI
                    }
                }
                isPrevHol -> {
                    // Đây là ngày 2 của cụm lễ kép 2 ngày
                    val baseShiftsPrev = mapOf(
                        "A" to getShift("A", prevDay),
                        "B" to getShift("B", prevDay),
                        "C" to getShift("C", prevDay)
                    )
                    val dayCrew = baseShiftsPrev.entries.firstOrNull { it.value == ShiftType.NGAY }?.key ?: "A"
                    val nightCrew = baseShiftsPrev.entries.firstOrNull { it.value == ShiftType.DEM }?.key ?: "B"
                    val offCrew = baseShiftsPrev.entries.firstOrNull { it.value == ShiftType.NGHI }?.key ?: "C"

                    when (crewId) {
                        offCrew -> ShiftType.NGAY
                        dayCrew -> ShiftType.NGHI
                        nightCrew -> ShiftType.DEM
                        else -> ShiftType.NGHI
                    }
                }
                else -> {
                    // Lễ đơn lẻ
                    val baseShifts = mapOf(
                        "A" to getShift("A", date),
                        "B" to getShift("B", date),
                        "C" to getShift("C", date)
                    )
                    val dayCrew = baseShifts.entries.firstOrNull { it.value == ShiftType.NGAY }?.key ?: "A"
                    val nightCrew = baseShifts.entries.firstOrNull { it.value == ShiftType.DEM }?.key ?: "B"
                    val offCrew = baseShifts.entries.firstOrNull { it.value == ShiftType.NGHI }?.key ?: "C"

                    when (crewId) {
                        dayCrew -> ShiftType.NGAY
                        nightCrew -> ShiftType.NGHI
                        offCrew -> ShiftType.DEM
                        else -> ShiftType.NGHI
                    }
                }
            }
        }
        return getShift(crewId, date)
    }

    /**
     * Get detailed shift info
     */
    fun getShiftInfo(crewId: String, date: LocalDate): ShiftInfo {
        val actualShift = getActualShift(crewId, date)
        val isOfficialHol = isHoliday(date)
        val holName = getHolidayName(date)

        val isSundayWork = (date.dayOfWeek.value == 7) && (actualShift != ShiftType.NGHI)
        val isSaturdayWork = isSaturdayHO(date) && (actualShift != ShiftType.NGHI)
        val isHOorHoliday = isOfficialHol || isSundayWork || isSaturdayWork

        val finalHolName = when {
            isOfficialHol -> holName
            isSundayWork -> "Chủ Nhật đi làm (HO)"
            isSaturdayWork -> "Thứ Bảy đi làm (HO)"
            else -> null
        }

        return ShiftInfo(actualShift, isHOorHoliday, finalHolName)
    }

    /**
     * Check if a date is a public holiday.
     */
    fun isHoliday(date: LocalDate): Boolean {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth

        if (m == 1 && d == 1) return true
        if (m == 4 && d == 30) return true
        if (m == 5 && d == 1) return true
        if (m == 9 && (d == 1 || d == 2)) return true
        if (m == 11 && d == 24) return true
        if (m == 12 && d == 31) return true

        return when (y) {
            2025 -> (m == 1 && d in 28..31) || (m == 2 && d == 1) || (m == 4 && d == 7)
            2026 -> (m == 2 && d in 16..20) || (m == 4 && d == 26)
            2027 -> (m == 2 && d in 5..9) || (m == 4 && d == 16)
            2028 -> (m == 1 && d in 25..29) || (m == 4 && d == 4)
            2029 -> (m == 2 && d in 12..16) || (m == 4 && d == 23)
            else -> false
        }
    }

    /**
     * Kiểm tra xem ngày có thuộc 5 ngày nghỉ Tết Âm lịch hàng năm hay không.
     */
    fun isLunarNewYear(date: LocalDate): Boolean {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth
        return when (y) {
            2025 -> (m == 1 && d in 28..31) || (m == 2 && d == 1)
            2026 -> m == 2 && d in 16..20
            2027 -> m == 2 && d in 5..9
            2028 -> m == 1 && d in 25..29
            2029 -> m == 2 && d in 12..16
            else -> false
        }
    }

    /**
     * Get holiday name if the date is a holiday.
     */
    fun getHolidayName(date: LocalDate): String? {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth

        return when {
            m == 1 && d == 1 -> "Tết Dương lịch"
            m == 4 && d == 30 -> "Giải phóng miền Nam"
            m == 5 && d == 1 -> "Quốc tế Lao động"
            m == 9 && (d == 1 || d == 2) -> "Quốc khánh"
            m == 11 && d == 24 -> "Kỷ niệm ngày thành lập"
            m == 12 && d == 31 -> "SDV Day"
            
            y == 2025 && ((m == 1 && d in 28..31) || (m == 2 && d == 1)) -> "Tết Nguyên Đán"
            y == 2025 && m == 4 && d == 7 -> "Giỗ Tổ Hùng Vương"
            
            y == 2026 && (m == 2 && d in 16..20) -> "Tết Nguyên Đán"
            y == 2026 && m == 4 && d == 26 -> "Giỗ Tổ Hùng Vương"
            
            y == 2027 && (m == 2 && d in 5..9) -> "Tết Nguyên Đán"
            y == 2027 && m == 4 && d == 16 -> "Giỗ Tổ Hùng Vương"
            
            y == 2028 && (m == 1 && d in 25..29) -> "Tết Nguyên Đán"
            y == 2028 && m == 4 && d == 4 -> "Giỗ Tổ Hùng Vương"
            
            y == 2029 && (m == 2 && d in 12..16) -> "Tết Nguyên Đán"
            y == 2029 && m == 4 && d == 23 -> "Giỗ Tổ Hùng Vương"
            
            isHoliday(date) -> "Ngày lễ"
            else -> null
        }
    }

    private fun countHolidaysBetween(anchorDate: LocalDate, targetDate: LocalDate): Int {
        var count = 0
        if (targetDate.isAfter(anchorDate)) {
            var curr = anchorDate.plusDays(1)
            while (curr.isBefore(targetDate)) {
                if (isHoliday(curr)) {
                    count++
                }
                curr = curr.plusDays(1)
            }
        } else if (targetDate.isBefore(anchorDate)) {
            var curr = targetDate.plusDays(1)
            while (curr.isBefore(anchorDate) || curr == anchorDate) {
                if (isHoliday(curr)) {
                    count--
                }
                curr = curr.plusDays(1)
            }
        }
        return count
    }

    private fun toJulianDayNumber(date: LocalDate): Int {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    private fun mod(a: Int, b: Int): Int {
        return ((a % b) + b) % b
    }

    fun findNextWorkDay(crewId: String, fromDate: LocalDate): Pair<LocalDate, ShiftType> {
        var date = fromDate
        for (i in 0..30) {
            val shift = getShift(crewId, date)
            if (shift != ShiftType.NGHI) {
                return Pair(date, shift)
            }
            date = date.plusDays(1)
        }
        return Pair(fromDate, ShiftType.NGHI)
    }
}
