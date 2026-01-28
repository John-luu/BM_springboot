package com.example.spring_seatreservation.controller;

import com.example.spring_seatreservation.common.R;
import com.example.spring_seatreservation.mapper.AdminMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminMapper adminMapper;

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
    // 但还需要确保有rows和columns字段（前端传的就是这些字段名）
    return R.ok().data(map);
}

    @PostMapping("/addSeat")
    public R addSeat(@RequestBody Map<String, Object> map) {
        adminMapper.addSeat(map);
        return R.ok();
    }

    @PostMapping("/deleteSeat")
    public R deleteSeat(@RequestBody Map<String, Object> map) {
        adminMapper.deleteSeat(map);
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

    /* ===================== 用户搜索（分页） ===================== */

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
        // 或：.total(total)
    }

    /* ===================== 教师 ===================== */

    @GetMapping("/getTeacher")
    public R getTeacher() {
        return R.ok()
                .put("rows", adminMapper.getTeacher());
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
}
