package com.example.spring_seatreservation.controller;

import com.example.spring_seatreservation.Other.DynamicTaskService;
import com.example.spring_seatreservation.Other.SignedNumber;
import com.example.spring_seatreservation.common.MyTask;
import com.example.spring_seatreservation.common.R;
import com.example.spring_seatreservation.common.ReservationCode;
import com.example.spring_seatreservation.common.SeatCode;
import com.example.spring_seatreservation.mapper.PublicMapper;
import com.example.spring_seatreservation.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/user")
public class UserController {

    private final DynamicTaskService dynamicTask;

    public UserController(DynamicTaskService dynamicTask) {
        this.dynamicTask = dynamicTask;
    }

    @Resource
    private UserMapper userMapper;
    @Resource
    private PublicMapper publicMapper;

    @PostMapping("/addReservation")
    public R addReservation(@RequestBody Map<String, Object> map) {
        Object startObj = map.get("startTime");
        Object endObj = map.get("endTime");

        long startTime = 0L;
        long endTime = 0L;
        try {
            if (startObj instanceof Number) {
                startTime = ((Number) startObj).longValue();
            } else {
                startTime = Long.parseLong(startObj.toString());
            }
            if (endObj instanceof Number) {
                endTime = ((Number) endObj).longValue();
            } else {
                endTime = Long.parseLong(endObj.toString());
            }
        } catch (Exception e) {
            return R.error("时间格式错误");
        }

        if (endTime <= startTime) {
            return R.error("预约时间段无效");
        }

        long now = System.currentTimeMillis();
        if (endTime <= now) {
            return R.error("预约结束时间必须晚于当前时间");
        }

        int futureCount = userMapper.countFutureReservations(
                map.get("uid"),
                new java.sql.Timestamp(now)
        );
        if (futureCount >= 3) {
            return R.error("未来预约已达上限（最多3条），请等待已有预约结束后再预约");
        }

        int overlap = userMapper.countSeatTimeOverlap(map.get("sid"), startTime, endTime);
        if (overlap > 0) {
            return R.error("该时间段座位已被预约");
        }

        String finishTaskName = ReservationCode.FINISH + "-" + map.get("sid");
        String unSignedTaskName = ReservationCode.UNSIGNED + "-" + map.get("sid");

        // 将毫秒时间转换为数据库可接受的 Timestamp 后插入预约记录
        map.put("startTime", new java.sql.Timestamp(startTime));
        map.put("endTime", new java.sql.Timestamp(endTime));
        userMapper.addReservation(map);
        int rid = Integer.parseInt(map.get("rid").toString());

        // 设置超时未签到和预约结束自动恢复座位状态的任务
        dynamicTask.add(new MyTask(unSignedTaskName, startTime + 30 * 60 * 1000L, () -> {
            dynamicTask.stop(finishTaskName);
            userMapper.updateReservation(ReservationCode.UNSIGNED, rid);
            userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
        }));

        dynamicTask.add(new MyTask(finishTaskName, endTime, () -> {
            userMapper.updateReservation(ReservationCode.FINISH, rid);
            userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
        }));

        int remaining = Math.max(0, 3 - (futureCount + 1));
        return R.ok().put("remainingFutureQuota", remaining);
    }

    @PostMapping("/toSigned")
    public R toSigned(@RequestBody Map<String, Object> map) {
        long number = Long.parseLong(map.get("number").toString());
        Map<String, Object> reservation = userMapper.getReservationByRid(map.get("rid"));
        long currentTimeMillis = System.currentTimeMillis();
        Object state = reservation.get("state");

        if (state.equals(ReservationCode.TIME_BEGAN)) {
            Object startObjRes = reservation.get("startTime");
            long startMillis;
            if (startObjRes instanceof Number) {
                startMillis = ((Number) startObjRes).longValue();
            } else if (startObjRes instanceof java.util.Date) {
                startMillis = ((java.util.Date) startObjRes).getTime();
            } else {
                startMillis = Long.parseLong(startObjRes.toString());
            }
            if (currentTimeMillis > startMillis + 30 * 60 * 1000L
                    || currentTimeMillis < startMillis - 30 * 60 * 1000L) {
                return R.error("不在签到时间范围内");
            }
            boolean flag = number == SignedNumber.getSignedNumber(reservation);
            if (flag) {
                userMapper.updateReservation(ReservationCode.SIGNED_BE_USE, reservation.get("rid"));
                userMapper.updateSeat(SeatCode.BE_USE, reservation.get("sid"));
                dynamicTask.stop(ReservationCode.UNSIGNED + "-" + reservation.get("sid").toString());
            }
            return R.ok().put("verify", flag);
        }

        if (state.equals(ReservationCode.LEAVE)) {
            Object leaveObjRes = reservation.get("leaveTime");
            long leaveMillis;
            if (leaveObjRes instanceof Number) {
                leaveMillis = ((Number) leaveObjRes).longValue();
            } else if (leaveObjRes instanceof java.util.Date) {
                leaveMillis = ((java.util.Date) leaveObjRes).getTime();
            } else {
                leaveMillis = Long.parseLong(leaveObjRes.toString());
            }
            if (currentTimeMillis > leaveMillis + 60 * 60 * 1000L) {
                return R.error("暂离超时");
            }
            boolean flag = number == SignedNumber.getLeaveSignedNumber(reservation);
            if (flag) {
                userMapper.updateReservation(ReservationCode.SIGNED_BE_USE, reservation.get("rid"));
                userMapper.updateSeat(SeatCode.BE_USE, reservation.get("sid"));
                dynamicTask.stop(ReservationCode.LEAVE_UNSIGNED + "-" + reservation.get("sid").toString());
            }
            return R.ok().put("verify", flag);
        }

        return R.error("无效的状态");
    }

    @PostMapping("/toLeave")
    public R toLeave(@RequestBody Map<String, Object> map) {
        long leaveTime = System.currentTimeMillis();
        userMapper.leaveReservation(leaveTime, map.get("rid"));
        userMapper.leaveSeat(map.get("sid"));

        dynamicTask.add(new MyTask(ReservationCode.LEAVE_UNSIGNED + "-" + map.get("sid"),
                leaveTime + 60 * 60 * 1000L, () -> {
            dynamicTask.stop(ReservationCode.FINISH + "-" + map.get("sid").toString());
            userMapper.updateReservation(ReservationCode.LEAVE_UNSIGNED, map.get("rid"));
            userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
        }));

        return R.ok();
    }

    @PostMapping("/scanSignIn")
    public R scanSignIn(@RequestBody Map<String, Object> map) {
        Object uidObj = map.get("uid");
        String payload = String.valueOf(map.getOrDefault("qrPayload", "")).trim();

        if (uidObj == null) {
            return R.error("缺少用户信息");
        }
        if (payload.isEmpty()) {
            return R.error("二维码内容为空");
        }

        Long uid;
        try {
            uid = Long.parseLong(String.valueOf(uidObj));
        } catch (Exception e) {
            return R.error("用户信息格式错误");
        }

        Map<String, String> qr = parseSeatQrPayload(payload);
        if (qr == null) {
            return R.error("二维码格式无效");
        }

        List<Map<String, Object>> candidates = userMapper.getPendingSignReservationsByUid(uid);
        if (candidates == null || candidates.isEmpty()) {
            return R.error("当前没有待签到预约");
        }

        long now = System.currentTimeMillis();
        List<Map<String, Object>> inWindow = new ArrayList<>();

        for (Map<String, Object> reservation : candidates) {
            long startMillis = parseObjectTimeToMillis(reservation.get("startTime"));
            if (now >= startMillis - 30 * 60 * 1000L && now <= startMillis + 30 * 60 * 1000L) {
                inWindow.add(reservation);
            }
        }

        if (inWindow.isEmpty()) {
            return R.error("不在签到时间范围内");
        }

        Map<String, Object> matched = null;
        for (Map<String, Object> reservation : inWindow) {
            if (isQrSeatMatch(qr, reservation)) {
                matched = reservation;
                break;
            }
        }

        if (matched == null) {
            return R.error("扫码座位与当前预约不匹配");
        }

        Object rid = matched.get("rid");
        Object sid = matched.get("sid");
        userMapper.updateReservation(ReservationCode.SIGNED_BE_USE, rid);
        userMapper.updateSeat(SeatCode.BE_USE, sid);
        dynamicTask.stop(ReservationCode.UNSIGNED + "-" + String.valueOf(sid));

        String areaName = String.valueOf(matched.getOrDefault("areaName", ""));
        String subName = String.valueOf(matched.getOrDefault("subName", ""));
        String seatLabel = areaName + (subName.isEmpty() ? "" : (" " + subName)) + " " +
                String.valueOf(matched.getOrDefault("row", "")) + "排" +
                String.valueOf(matched.getOrDefault("column", "")) + "列";

        return R.ok("签到成功")
                .put("rid", rid)
                .put("sid", sid)
                .put("seatLabel", seatLabel.trim());
    }

    @PostMapping("/getReservation")
    public R getReservationNeedSub(@RequestBody Map<String, Object> map) {
        return R.ok().put("rows", userMapper.getReservation(map));
    }

    @PostMapping("/getReservationByUid")
    public R getReservationByUid(@RequestBody Map<String, Object> map) {
        List<Map<String, Object>> list = userMapper.getReservationByUid(map.get("uid"));
        return R.ok().put("rows", list);
    }

    @PostMapping("/cancelReservation")
    public R cancelReservation(@RequestBody Map<String, Object> map) {
        Object uidObj = map.get("uid");
        Object ridObj = map.get("rid");
        if (uidObj == null || ridObj == null) {
            return R.error("缺少预约信息");
        }

        Long uid;
        Long rid;
        try {
            uid = Long.parseLong(String.valueOf(uidObj));
            rid = Long.parseLong(String.valueOf(ridObj));
        } catch (Exception e) {
            return R.error("预约参数格式错误");
        }

        Map<String, Object> reservation = userMapper.getReservationByRidAndUid(rid, uid);
        if (reservation == null || reservation.isEmpty()) {
            return R.error("预约记录不存在");
        }

        Object stateObj = reservation.get("state");
        int state;
        try {
            state = Integer.parseInt(String.valueOf(stateObj));
        } catch (Exception e) {
            return R.error("预约状态异常");
        }

        if (state == ReservationCode.CANCELED) {
            return R.error("该预约已取消");
        }
        if (state != ReservationCode.TIME_BEGAN) {
            return R.error("当前预约状态不可取消");
        }

        long startTime = parseObjectTimeToMillis(reservation.get("startTime"));
        long now = System.currentTimeMillis();
        if (startTime <= now) {
            return R.error("预约已开始，无法取消");
        }

        long twoHoursMillis = 2 * 60 * 60 * 1000L;
        long diff = startTime - now;
        if (diff < twoHoursMillis) {
            return R.error("距离预约开始不足2小时，无法取消");
        }

        userMapper.updateReservation(ReservationCode.CANCELED, rid);
        dynamicTask.stop(ReservationCode.UNSIGNED + "-" + String.valueOf(reservation.get("sid")));
        dynamicTask.stop(ReservationCode.FINISH + "-" + String.valueOf(reservation.get("sid")));
        return R.ok("取消预约成功");
    }

    @PostMapping("/getScore")
    public R getScore(@RequestBody Map<String, Object> map) {
        Object uid = map.get("uid");
        if (uid == null) {
            return R.error("缺少用户ID");
        }
        int score = userMapper.getScore(uid);
        Map<String, Object> payload = new HashMap<>();
        payload.put("score", score);
        return R.ok()
                .put("score", score)
                .data(payload);
    }

    @PostMapping("/getScoreLogs")
    public R getScoreLogs(@RequestBody Map<String, Object> map) {
        Object uid = map.get("uid");
        if (uid == null) {
            return R.error("缺少用户ID");
        }
        userMapper.ensureScoreLogTable();
        List<Map<String, Object>> rows = userMapper.getScoreLogs(uid);
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", rows);
        return R.ok()
                .put("rows", rows)
                .data(payload);
    }

    @PostMapping("/getFutureReservationQuota")
    public R getFutureReservationQuota(@RequestBody Map<String, Object> map) {
        long now = System.currentTimeMillis();
        int futureCount = userMapper.countFutureReservations(
                map.get("uid"),
                new java.sql.Timestamp(now)
        );
        int remaining = Math.max(0, 3 - futureCount);
        return R.ok()
                .put("futureCount", futureCount)
                .put("remainingFutureQuota", remaining);
    }

    private Map<String, String> parseSeatQrPayload(String payload) {
        if (!payload.startsWith("SEAT_QR|")) {
            return null;
        }
        String[] parts = payload.split("\\|");
        Map<String, String> kv = new HashMap<>();
        for (String part : parts) {
            if (!part.contains("=")) continue;
            String[] item = part.split("=", 2);
            if (item.length == 2) {
                kv.put(item[0].trim(), item[1].trim());
            }
        }
        if (!kv.containsKey("row") || !kv.containsKey("column") || !kv.containsKey("sid")) {
            return null;
        }
        return kv;
    }

    private boolean isQrSeatMatch(Map<String, String> qr, Map<String, Object> reservation) {
        String sid = String.valueOf(reservation.getOrDefault("sid", ""));
        String row = String.valueOf(reservation.getOrDefault("row", ""));
        String column = String.valueOf(reservation.getOrDefault("column", ""));
        String aid = String.valueOf(reservation.getOrDefault("aid", ""));

        if (!sid.equals(qr.get("sid"))) {
            return false;
        }
        if (!row.equals(qr.get("row")) || !column.equals(qr.get("column"))) {
            return false;
        }
        String qrAid = qr.get("aid");
        return qrAid == null || qrAid.isEmpty() || aid.equals(qrAid);
    }

    private long parseObjectTimeToMillis(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).getTime();
        }
        String s = String.valueOf(value);
        if (s.matches("\\d+")) {
            return Long.parseLong(s);
        }
        s = s.replace('T', ' ');
        return java.sql.Timestamp.valueOf(s).getTime();
    }
}
