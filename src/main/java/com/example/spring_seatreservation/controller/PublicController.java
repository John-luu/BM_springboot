package com.example.spring_seatreservation.controller;

import com.example.spring_seatreservation.Other.DynamicTaskService;
import com.example.spring_seatreservation.Other.SignedNumber;
import com.example.spring_seatreservation.common.*;
import com.example.spring_seatreservation.config.ExcelUtil;
import com.example.spring_seatreservation.mapper.PublicMapper;
import com.example.spring_seatreservation.mapper.UserMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@RestController
@EnableAutoConfiguration
@RequestMapping("/public")
public class PublicController {

    private static final Logger log = LoggerFactory.getLogger(PublicController.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm")
                .optionalStart()
                .appendPattern(":ss")
                .optionalEnd()
            .optionalStart()
            .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter();

    @Resource
    private PublicMapper publicMapper;
    @Resource
    private UserMapper userMapper;

    private final DynamicTaskService dynamicTask;

    public PublicController(DynamicTaskService dynamicTask) {
        this.dynamicTask = dynamicTask;
    }

    /* ===================== 启动时恢复预约任务 ===================== */

    {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<Map<String, Object>> list = publicMapper.getNeedCheckReservation();

                for (Map<String, Object> map : list) {
                    try {
                        int state = (int) map.get("state");

                        // 兼容 startTime/endTime 存储为 datetime 或 long 的情况
                        long startTime = parseTimeToMillis(map.get("startTime"));
                        long endTime = parseTimeToMillis(map.get("endTime"));

                        if (startTime <= 0L || endTime <= 0L || endTime <= startTime) {
                            log.warn("Skip reservation task due to invalid time range: rid={}, startTime={}, endTime={}",
                                    map.get("rid"), map.get("startTime"), map.get("endTime"));
                            continue;
                        }

                        // 预约结束
                        dynamicTask.add(new MyTask(
                                ReservationCode.FINISH + "-" + map.get("sid"),
                                endTime,
                                () -> {
                                    userMapper.updateReservation(ReservationCode.FINISH, map.get("rid"));
                                    userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
                                }
                        ));

                        // 预约中，超时未签到
                        if (state == ReservationCode.TIME_BEGAN) {
                            long now = System.currentTimeMillis();
                            // 启动恢复时兜底：如果已经超过签到截止时间还未签到，直接判定为未签到
                            if (now > startTime + 30 * 60 * 1000L) {
                                dynamicTask.stop(ReservationCode.FINISH + "-" + map.get("sid"));
                                publicMapper.updateReservation(ReservationCode.UNSIGNED, map.get("rid"));
                                userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
                                continue;
                            }
                            dynamicTask.add(new MyTask(
                                    ReservationCode.UNSIGNED + "-" + map.get("sid"),
                                    startTime + 30 * 60 * 1000L,
                                    () -> {
                                        dynamicTask.stop(ReservationCode.FINISH + "-" + map.get("sid"));
                                        publicMapper.updateReservation(ReservationCode.UNSIGNED, map.get("rid"));
                                        userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
                                    }
                            ));
                        }

                        // 暂离
                        else if (state == ReservationCode.LEAVE) {
                            long leaveTime = parseTimeToMillis(map.get("leaveTime"));
                            if (leaveTime > 0L) {
                                dynamicTask.add(new MyTask(
                                        ReservationCode.LEAVE_UNSIGNED + "-" + map.get("sid"),
                                        leaveTime + 60 * 60 * 1000L,
                                        () -> {
                                            dynamicTask.stop(ReservationCode.FINISH + "-" + map.get("sid"));
                                            publicMapper.updateReservation(ReservationCode.LEAVE_UNSIGNED, map.get("rid"));
                                            userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
                                        }
                                ));
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Skip restoring reservation task due to invalid record: rid={}, data={}", map.get("rid"), map, e);
                    }
                }
            }
        }, 5000);
    }

    /**
     * 工具方法：把可能为 long / Date / 字符串(datetime) 的时间统一转成毫秒
     */
    private long parseTimeToMillis(Object value) {
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
        s = s.trim();
        if (s.isEmpty()) {
            return 0L;
        }

        try {
            return Instant.parse(s).toEpochMilli();
        } catch (Exception ignored) {
        }

        try {
            return OffsetDateTime.parse(s).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }

        String normalized = s.replace('T', ' ');
        try {
            LocalDateTime dateTime = LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }

        try {
            LocalDate date = LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("Unsupported timestamp value: " + value);
    }

    /* ===================== 签到码 ===================== */

    @PostMapping("/getSignedNumber")
    public R getSignedNumber(@RequestBody Map<String, Object> map) {
        Map<String, Object> reservation =
                publicMapper.getReservationBySid(map.get("sid"));
if (reservation == null || reservation.isEmpty()) {
    return R.error("未找到对应的预约记录");
}
        R result = R.ok();
        Object state = reservation.get("state");

        if (state.equals(ReservationCode.TIME_BEGAN)) {
            result.put("number", SignedNumber.getSignedNumber(reservation));
        } else if (state.equals(ReservationCode.LEAVE)) {
            result.put("number", SignedNumber.getLeaveSignedNumber(reservation));
        }

        return result;
    }

    /* ===================== 动态任务 ===================== */

    @GetMapping
    public List<String> getStartingDynamicTask() {
        return dynamicTask.getTaskList();
    }

    @DeleteMapping("/{name}")
    public String stopDynamicTask(@PathVariable String name) {
        return dynamicTask.stop(name) ? "任务已停止" : "停止失败,任务已在进行中.";
    }

    /* ===================== 区域 / 座位 ===================== */

    @GetMapping("/getArea")
    public R getArea() {
        return R.ok()
                .put("rows", publicMapper.getArea());
    }

    @PostMapping("/getAreaSeats")
    public R getAreaSeats(@RequestBody Map<String, Object> map) {
        List<Map<String, Object>> areaSeats = publicMapper.getAreaSeats(map);

        // 默认 show=false（前端弹窗控制）
        areaSeats.forEach(seat -> seat.put("show", false));

        // 如果前端传了时间段，就按时间段动态计算哪些座位“有预约”，覆盖 seat.state
        Object areaObj = map.get("area");
        Object startObj = map.get("startTime");
        Object endObj = map.get("endTime");
        if (areaObj != null && startObj != null && endObj != null) {
            try {
                int area = Integer.parseInt(areaObj.toString());
                long startTime = Long.parseLong(startObj.toString());
                long endTime = Long.parseLong(endObj.toString());
                if (endTime > startTime) {
                    List<Integer> reservedIds =
                            publicMapper.getReservedSeatIdsInArea(area, startTime, endTime);
                    Set<Integer> reservedSet = new HashSet<>();
                    if (reservedIds != null) {
                        reservedSet.addAll(reservedIds);
                    }
                    for (Map<String, Object> seat : areaSeats) {
                        Object sidObj = seat.get("sid");
                        if (sidObj == null) continue;
                        int sid = Integer.parseInt(sidObj.toString());
                        // 按时间段动态覆盖 state：只反映该时间段是否被占用
                        if (reservedSet.contains(sid)) {
                            seat.put("state", SeatCode.BE_RESERVATION);
                        } else if ((int) seat.getOrDefault("type", 0) == 0) {
                            seat.put("state", SeatCode.CAN_USE);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return R.ok().put("rows", areaSeats);
    }
    @PostMapping("/deleteAreaWithSeats")
public R deleteAreaWithSeats(@RequestBody Map<String, Object> map) {
    try {
        // 获取区域ID
        Integer areaId = (Integer) map.get("aid");
        
        if (areaId == null) {
            return R.error("区域ID不能为空");
        }
        
        // 1. 先删除该区域的所有座位
        publicMapper.deleteSeatsByArea(areaId);
        
        // 2. 删除区域本身
        publicMapper.deleteArea(areaId);
        
        return R.ok("区域删除成功");
    } catch (Exception e) {
        e.printStackTrace();
        return R.error("删除区域失败: " + e.getMessage());
    }
}

    /* ===================== 用户 ===================== */

    @PostMapping("/upPwd")
    public R updatePwd(@RequestBody Map<String, Object> map) {
        Long number = Long.parseLong(map.get("number").toString());
        if (publicMapper.getUserByNumber(number).getPassword()
                .equals(map.get("opassword"))) {
            publicMapper.updatePwd(map.get("npassword").toString(), number);
            return R.ok();
        }
        return R.error("原密码错误");
    }

    @PostMapping("/register")
    public R register(@RequestBody MyUser user) {
        try {
            publicMapper.insertUser(user);
            return R.ok();
        } catch (Exception e) {
            return R.error("注册失败");
        }
    }

@PostMapping("/importStudents")
public R importStudents(
        @RequestParam("file") MultipartFile file,
        @RequestParam("password") String password
) {
    try {
        // 1. 解析 Excel
        List<MyUser> users = ExcelUtil.parseStudentExcel(file, password);

        // 2. 批量插入
        for (MyUser user : users) {
            publicMapper.insertUser(user);
        }

        return R.ok();
    } catch (Exception e) {
        e.printStackTrace();
        return R.error("批量导入失败");
    }
}


    @PostMapping("/login")
    public R login(@RequestBody MyUser user) {
        try {
            MyUser dbUser = publicMapper.getUserByNumber(user.getNumber());
            if (dbUser == null) {
                return R.error("账号或密码错误");
            }
            if (!String.valueOf(dbUser.getPassword()).equals(String.valueOf(user.getPassword()))) {
                return R.error("账号或密码错误");
            }
            return R.ok().put("user", dbUser);
        } catch (Exception ignored) {}
        return R.error("账号或密码错误");
    }
    
    @PostMapping("/changePassword")
    public R changePassword(@RequestBody Map<String, Object> map) {
        Object numberObj = map.get("number");
        if (numberObj == null) {
            return R.error("缺少学号");
        }
        Long number;
        try {
            number = Long.parseLong(numberObj.toString());
        } catch (Exception e) {
            return R.error("学号格式错误");
        }
        String oldPwd = map.containsKey("oldPassword")
                ? String.valueOf(map.get("oldPassword"))
                : String.valueOf(map.getOrDefault("opassword", ""));
        String newPwd = map.containsKey("newPassword")
                ? String.valueOf(map.get("newPassword"))
                : String.valueOf(map.getOrDefault("npassword", ""));
        if (newPwd == null || newPwd.trim().isEmpty()) {
            return R.error("新密码不能为空");
        }
        MyUser dbUser = publicMapper.getUserByNumber(number);
        if (dbUser == null) {
            return R.error("用户不存在");
        }
        if (dbUser.getType() != 0) {
            return R.error("仅学生可修改该密码");
        }
        if (!String.valueOf(dbUser.getPassword()).equals(oldPwd)) {
            return R.error("原密码错误");
        }
        publicMapper.updatePwd(newPwd, number);
        return R.ok();
    }
}
