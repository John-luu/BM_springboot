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
        List<Map<String, Object>> curReservation = userMapper.getCurReservation(map.get("uid"));
        if (curReservation.size() > 0) {
            return R.error("当前已有预约");
        }

        long startTime = (long) map.get("startTime");
        long endTime = (long) map.get("endTime");

        String finishTaskName = ReservationCode.FINISH + "-" + map.get("sid");
        String unSignedTaskName = ReservationCode.UNSIGNED + "-" + map.get("sid");

        // 插入预约记录
        userMapper.addReservation(map);
        userMapper.updateSeat(SeatCode.BE_RESERVATION, map.get("sid"));
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

        return R.ok();
    }

    @PostMapping("/toSigned")
    public R toSigned(@RequestBody Map<String, Object> map) {
        long number = Long.parseLong(map.get("number").toString());
        Map<String, Object> reservation = userMapper.getReservationByRid(map.get("rid"));
        long currentTimeMillis = System.currentTimeMillis();
        Object state = reservation.get("state");

        if (state.equals(ReservationCode.TIME_BEGAN)) {
            if (currentTimeMillis > ((long) reservation.get("startTime")) + 30 * 60 * 1000L
                    || currentTimeMillis < ((long) reservation.get("startTime")) - 30 * 60 * 1000L) {
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
            if (currentTimeMillis > ((long) reservation.get("leaveTime")) + 60 * 60 * 1000L) {
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

    @PostMapping("/getReservation")
    public R getReservationNeedSub(@RequestBody Map<String, Object> map) {
        return R.ok().put("rows", userMapper.getReservation(map));
    }

    @PostMapping("/getReservationByUid")
    public R getReservationByUid(@RequestBody Map<String, Object> map) {
        List<Map<String, Object>> list = userMapper.getReservationByUid(map.get("uid"));
        return R.ok().put("rows", list);
    }

    @PostMapping("/getScore")
    public R getScore(@RequestBody Map<String, Object> map) {
        return R.ok().put("score", userMapper.getScore(map.get("uid")));
    }
}
