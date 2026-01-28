package com.example.spring_seatreservation.controller;

import com.example.spring_seatreservation.Other.DynamicTaskService;
import com.example.spring_seatreservation.Other.SignedNumber;
import com.example.spring_seatreservation.common.*;
import com.example.spring_seatreservation.mapper.PublicMapper;
import com.example.spring_seatreservation.mapper.UserMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@EnableAutoConfiguration
@RequestMapping("/public")
public class PublicController {

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
                    int state = (int) map.get("state");
                    long startTime = Long.parseLong(map.get("startTime").toString());

                    // 预约结束
                    dynamicTask.add(new MyTask(
                            ReservationCode.FINISH + "-" + map.get("sid"),
                            (long) map.get("endTime"),
                            () -> {
                                userMapper.updateReservation(ReservationCode.FINISH, map.get("rid"));
                                userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
                            }
                    ));

                    // 预约中，超时未签到
                    if (state == ReservationCode.TIME_BEGAN) {
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
                        dynamicTask.add(new MyTask(
                                ReservationCode.LEAVE_UNSIGNED + "-" + map.get("sid"),
                                ((long) map.get("leaveTime")) + 60 * 60 * 1000L,
                                () -> {
                                    dynamicTask.stop(ReservationCode.FINISH + "-" + map.get("sid"));
                                    publicMapper.updateReservation(ReservationCode.LEAVE_UNSIGNED, map.get("rid"));
                                    userMapper.updateSeat(SeatCode.CAN_USE, map.get("sid"));
                                }
                        ));
                    }
                }
            }
        }, 5000);
    }

    /* ===================== 签到码 ===================== */

    @PostMapping("/getSignedNumber")
    public R getSignedNumber(@RequestBody Map<String, Object> map) {
        Map<String, Object> reservation =
                publicMapper.getReservationBySid(map.get("sid"));

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
        areaSeats.forEach(seat -> seat.put("show", false));
        return R.ok()
                .put("rows", areaSeats);
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

    @PostMapping("/login")
    public R login(@RequestBody MyUser user) {
        try {
            MyUser dbUser = publicMapper.getUserByNumber(user.getNumber());
            if (dbUser.getPassword().equals(user.getPassword())) {
                return R.ok().put("user", dbUser);
            }
        } catch (Exception ignored) {}
        return R.error("账号或密码错误");
    }
}
