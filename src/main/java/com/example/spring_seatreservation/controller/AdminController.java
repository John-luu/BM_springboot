package com.example.spring_seatreservation.controller;

import com.example.spring_seatreservation.common.R;
import com.example.spring_seatreservation.mapper.AdminMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminMapper adminMapper;

    @PostConstruct
    public void initScoreLogTable() {
        adminMapper.ensureScoreLogTable();
    }

    /* ===================== 运动状态 ===================== */

    @PostMapping("/updateSport")
    public R updateSport(@RequestBody Map<String, Object> map) {
        adminMapper.updateSport(map);
        return R.ok();
    }

    /* ===================== 公告 ===================== */

    @GetMapping("/getAnnounce")
    public R getAnnounce() {
        return R.ok()
                .put("rows", adminMapper.getAnnounce());
    }

    @PostMapping("/addAnnounce")
    public R addAnnounce(@RequestBody Map<String, Object> map) {
        map.put("datetime", System.currentTimeMillis());
        adminMapper.addAnnounce(map);
        return R.ok();
    }

    @PostMapping("/deleteAnnounce")
    public R deleteAnnounce(@RequestBody Map<String, Object> map) {
        adminMapper.deleteAnnounce(map);
        return R.ok();
    }

    /* ===================== 区域 / 座位 ===================== */

    @PostMapping("/addArea")
    public R addArea(@RequestBody Map<String, Object> map) {
        // 插入数据，aid会自动设置到map中
        adminMapper.addArea(map);
        
        // 直接返回map，它现在包含了aid
        return R.ok().data(map);
    }

    @PostMapping("/addSeat")
    public R addSeat(@RequestBody Map<String, Object> map) {
        adminMapper.addSeat(map);
        return R.ok();
    }

    @PostMapping("/addSeatsBatch")
    public R addSeatsBatch(@RequestBody Map<String, Object> map) {
        Integer area = (Integer) map.get("area");
        Integer type = (Integer) map.get("type");
        List<Map<String, Integer>> rows =
                (List<Map<String, Integer>>) map.get("rows");

        if (area == null || type == null || rows == null || rows.isEmpty()) {
            return R.error("参数不完整");
        }

        adminMapper.addSeatsBatch(area, type, rows);
        return R.ok("批量添加成功");
    }

    @PostMapping("/deleteSeat")
    public R deleteSeat(@RequestBody Map<String, Object> map) {
        Integer sid = parseInt(map.get("sid"));
        if (sid == null || sid <= 0) {
            return R.error("缺少有效的座位ID");
        }

        adminMapper.deleteReservationBySid(sid);
        adminMapper.deleteSeatBySid(sid);
        return R.ok();
    }

    /* ===================== 文章 ===================== */

    @PostMapping("/deleteArticle")
    public R deleteArticle(@RequestBody Map<String, Object> map) {
        adminMapper.deleteArticle(map);
        return R.ok();
    }

    /* ===================== 管理员 ===================== */

    @PostMapping("/updatePwd")
    public R updatePwd(@RequestBody Map<String, Object> map) {
        adminMapper.updatePassword(map);
        return R.ok();
    }

    /* ===================== 学生搜索（分页） ===================== */

    @GetMapping("/searchUser")
    public R searchUser(
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        int offset = (page - 1) * pageSize;

        List<Map<String, Object>> rows =
                adminMapper.searchUser(number, username, offset, pageSize);
        int total = adminMapper.countUser(number, username);

        return R.ok()
                .put("rows", rows)
                .put("total", total);
    }

    /* ===================== 教师搜索（分页） ===================== */

    @GetMapping("/searchTeacher")
    public R searchTeacher(
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        int offset = (page - 1) * pageSize;

        List<Map<String, Object>> rows =
                adminMapper.searchTeacher(number, username, offset, pageSize);
        int total = adminMapper.countTeacher(number, username);

        return R.ok()
                .put("rows", rows)
                .put("total", total);
    }

    /* ===================== 教师（旧接口，兼容） ===================== */

    @GetMapping("/getTeacher")
    public R getTeacher() {
        return R.ok()
                .put("rows", adminMapper.getTeacher());
    }

    @PostMapping("/getReservation")
    public R getReservation(@RequestBody Map<String, Object> map) {
        Integer page = Integer.parseInt(String.valueOf(map.getOrDefault("page", 1)));
        Integer pageSize = Integer.parseInt(String.valueOf(map.getOrDefault("pageSize", 10)));
        String date = String.valueOf(map.getOrDefault("date", "")).trim();
        if ("null".equalsIgnoreCase(date)) {
            date = "";
        }
        int offset = (page - 1) * pageSize;

        List<Map<String, Object>> rows = adminMapper.getReservationPage(offset, pageSize, date);
        int total = adminMapper.countReservation(date);
        Map<String, Object> summary = adminMapper.getReservationSummary(date);
        if (summary == null) {
            summary = new HashMap<>();
        }

        summary.put("reserveTotal", parseInt(summary.get("reserveTotal")) == null ? 0 : parseInt(summary.get("reserveTotal")));
        summary.put("signTotal", parseInt(summary.get("signTotal")) == null ? 0 : parseInt(summary.get("signTotal")));
        summary.put("finishTotal", parseInt(summary.get("finishTotal")) == null ? 0 : parseInt(summary.get("finishTotal")));
        summary.put("leaveTotal", parseInt(summary.get("leaveTotal")) == null ? 0 : parseInt(summary.get("leaveTotal")));
        summary.put("violateTotal", parseInt(summary.get("violateTotal")) == null ? 0 : parseInt(summary.get("violateTotal")));

        return R.ok()
                .put("rows", rows)
                .put("total", total)
                .put("summary", summary)
                .put("date", date);
    }

    @PostMapping("/getReservationNeedSub")
    public R getReservationNeedSub(@RequestBody Map<String, Object> map) {
        Integer page = Integer.parseInt(String.valueOf(map.getOrDefault("page", 1)));
        Integer pageSize = Integer.parseInt(String.valueOf(map.getOrDefault("pageSize", 10)));
        int offset = (page - 1) * pageSize;

        Map<String, Object> params = new HashMap<>(map);
        params.put("offset", offset);
        params.put("pageSize", pageSize);

        List<Map<String, Object>> rows = adminMapper.getScoreReservationPage(params);
        int total = adminMapper.countScoreReservation(params);

        return R.ok()
                .put("rows", rows)
                .put("total", total);
    }

    @PostMapping("/subScore")
    @Transactional(rollbackFor = Exception.class)
    public R subScore(@RequestBody Map<String, Object> map) {
        Integer uid;
        Integer rid;
        Integer score;
        try {
            uid = Integer.parseInt(String.valueOf(map.getOrDefault("uid", 0)));
            rid = Integer.parseInt(String.valueOf(map.getOrDefault("rid", 0)));
            score = Integer.parseInt(String.valueOf(map.getOrDefault("score", 5)));
        } catch (Exception e) {
            return R.error("参数格式错误");
        }
        Long operatorUid = null;
        try {
            Object operatorUidObj = map.get("operatorUid");
            if (operatorUidObj != null && !String.valueOf(operatorUidObj).trim().isEmpty()) {
                operatorUid = Long.parseLong(String.valueOf(operatorUidObj));
            }
        } catch (Exception ignored) {
        }
        String operatorName = String.valueOf(map.getOrDefault("operatorName", "管理员")).trim();
        if (operatorName.isEmpty()) {
            operatorName = "管理员";
        }

        if (uid <= 0 || rid <= 0) {
            return R.error("参数错误");
        }

        if (score <= 0) {
            score = 5;
        }

        int updated = adminMapper.subReservationScore(rid, score);
        if (updated == 0) {
            return R.error("该记录已扣分，无需重复操作");
        }

        adminMapper.subUserScore(uid, score);
        adminMapper.ensureScoreLogTable();
        adminMapper.addScoreLog(uid, rid, score, operatorUid, operatorName);
        return R.ok("扣分成功")
                .put("uid", uid)
                .put("rid", rid)
                .put("score", score);
    }

    /* ===================== 统计 ===================== */

    @GetMapping("/getStatistics")
    public R getStatistics() {
        List<Map<String, Object>> list = adminMapper.getStatistics();
        List<Map<String, Object>> timeList = new ArrayList<>();

        for (int i = 16; i < 45; i++) {
            String time = (i / 2) + (i % 2 == 0 ? ":00" : ":30");
            String endTime = ((i + 1) / 2) + ((i + 1) % 2 == 0 ? ":00" : ":30");
            int sum = 0;

            for (Map<String, Object> map : list) {
                Date startDate = new Date((long) map.get("startTime"));
                Calendar c1 = Calendar.getInstance();
                c1.setTime(startDate);
                int startHalfHour =
                        c1.get(Calendar.HOUR_OF_DAY) * 2 +
                        (c1.get(Calendar.MINUTE) > 0 ? 1 : 0);

                Date endDate = new Date((long) map.get("endTime"));
                Calendar c2 = Calendar.getInstance();
                c2.setTime(endDate);
                int endHalfHour =
                        c2.get(Calendar.HOUR_OF_DAY) * 2 +
                        (c2.get(Calendar.MINUTE) > 0 ? 1 : 0);

                if (startHalfHour <= i && endHalfHour > i) {
                    sum++;
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("time", time + "-" + endTime);
            item.put("sum", sum);
            timeList.add(item);
        }

        return R.ok()
                .put("userCounter", adminMapper.getUserCounter())
                .put("timeList", timeList);
    }

    @PostMapping("/getStatisticsRanking")
    public R getStatisticsRanking(@RequestBody(required = false) Map<String, Object> map) {
        Long startTime = parseLong(map == null ? null : map.get("startTime"));
        Long endTime = parseLong(map == null ? null : map.get("endTime"));

        List<Map<String, Object>> signRank = adminMapper.getSignLeaderboard(startTime, endTime);
        List<Map<String, Object>> monthRank = adminMapper.getMonthReservationRanking(startTime, endTime);
        List<Map<String, Object>> dayRank = adminMapper.getDayReservationRanking(startTime, endTime);

        List<Map<String, Object>> timeSlotRows = adminMapper.getTimeSlotReservationRanking(startTime, endTime);
        List<Map<String, Object>> timeSlotRank = new ArrayList<>();
        for (Map<String, Object> row : timeSlotRows) {
            Integer slotIndex = parseInt(row.get("slotIndex"));
            if (slotIndex == null) {
                continue;
            }
            int startHour = slotIndex / 2;
            int startMinute = slotIndex % 2 == 0 ? 0 : 30;
            int next = slotIndex + 1;
            int endHour = next / 2;
            int endMinute = next % 2 == 0 ? 0 : 30;

            String label = String.format(
                    "%02d:%02d-%02d:%02d",
                    startHour,
                    startMinute,
                    endHour,
                    endMinute
            );

            Map<String, Object> item = new HashMap<>();
            item.put("period", label);
            item.put("slotIndex", slotIndex);
            item.put("total", row.get("total"));
            timeSlotRank.add(item);
        }

        return R.ok()
                .put("signRank", signRank)
                .put("monthRank", monthRank)
                .put("dayRank", dayRank)
                .put("timeSlotRank", timeSlotRank);
    }

    @PostMapping("/getStatisticsDayDetail")
    public R getStatisticsDayDetail(@RequestBody(required = false) Map<String, Object> map) {
        String date = map == null ? null : String.valueOf(map.getOrDefault("date", "")).trim();
        if (date.isEmpty() || "null".equalsIgnoreCase(date)) {
            date = LocalDate.now().toString();
        }

        Map<String, Object> summary = adminMapper.getDayStatisticsSummary(date);
        if (summary == null) {
            summary = new HashMap<>();
        }

        summary.put("reserveTotal", parseInt(summary.get("reserveTotal")) == null ? 0 : parseInt(summary.get("reserveTotal")));
        summary.put("signTotal", parseInt(summary.get("signTotal")) == null ? 0 : parseInt(summary.get("signTotal")));
        summary.put("finishTotal", parseInt(summary.get("finishTotal")) == null ? 0 : parseInt(summary.get("finishTotal")));
        summary.put("leaveTotal", parseInt(summary.get("leaveTotal")) == null ? 0 : parseInt(summary.get("leaveTotal")));
        summary.put("violateTotal", parseInt(summary.get("violateTotal")) == null ? 0 : parseInt(summary.get("violateTotal")));

        List<Map<String, Object>> rows = adminMapper.getDayStatisticsUsers(date);
        for (Map<String, Object> row : rows) {
            Integer state = parseInt(row.get("state"));
            row.put("state", state == null ? 0 : state);
            row.put("stateText", getStateText(state));
            row.put("signed", state != null && (state == 1 || state == 3 || state == -1));
        }

        return R.ok()
                .put("date", date)
                .put("summary", summary)
                .put("rows", rows);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String getStateText(Integer state) {
        if (state == null) {
            return "待签到";
        }
        if (state == -1) {
            return "正常结束";
        }
        if (state == 1) {
            return "已签到";
        }
        if (state == 3) {
            return "临时离开";
        }
        if (state == 2) {
            return "未签到";
        }
        if (state == 4) {
            return "违规离座";
        }
        return "待签到";
    }
}