package com.example.spring_seatreservation.controller;

import com.example.spring_seatreservation.common.R;
import com.example.spring_seatreservation.mapper.TeacherMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import javax.annotation.Resource;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Resource
    private TeacherMapper teacherMapper;

    @GetMapping("/getReservation")
    public R getReservation() {
        return R.ok()
                .put("rows", teacherMapper.getReservation());
    }

    @GetMapping("/getReservationNeedSub")
    public R getReservationNeedSub() {
        return R.ok()
                .put("rows", teacherMapper.getReservationNeedSub());
    }

    @PostMapping("/subScore")
    public R subScore(@RequestBody Map<String, Object> map) {
        teacherMapper.subScore(map);
        teacherMapper.subReservationScore(map);
        return R.ok();
    }
}
