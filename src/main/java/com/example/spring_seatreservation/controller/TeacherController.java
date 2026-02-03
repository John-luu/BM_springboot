package com.example.spring_seatreservation.controller;

import com.example.spring_seatreservation.common.R;
import com.example.spring_seatreservation.mapper.TeacherMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Resource
    private TeacherMapper teacherMapper;

    // 获取所有预约（分页）
    @GetMapping("/getReservation")
    public R getReservation(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        int offset = (page - 1) * pageSize;

        List<Map<String, Object>> rows =
                teacherMapper.getReservationPage(offset, pageSize);

        Integer total = teacherMapper.getReservationTotal();

        return R.ok()
                .put("rows", rows)
                .put("total", total);
    }

    // 获取需要扣分的预约（分页）
    @GetMapping("/getReservationNeedSub")
    public R getReservationNeedSub(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> rows =
                teacherMapper.getReservationNeedSubPage(offset, pageSize);

        Integer total = teacherMapper.getReservationNeedSubTotal();

        return R.ok()
                .put("rows", rows)
                .put("total", total);
    }

    // 扣分
    @PostMapping("/subScore")
    public R subScore(@RequestBody Map<String, Object> map) {
        Integer uid = (Integer) map.get("uid");
        Integer rid = (Integer) map.get("rid");

        teacherMapper.subScore(uid);
        teacherMapper.subReservationScore(rid);

        return R.ok();
    }
}
