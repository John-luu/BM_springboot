package com.example.spring_seatreservation.controller;

import com.example.spring_seatreservation.common.R;
import com.example.spring_seatreservation.mapper.ForumMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/forum")
public class ForumController {

    @Resource
    private ForumMapper forumMapper;

    /* ===================== 用户端 ===================== */

    @GetMapping("/getArticle")
    public R getArticle() {
        return R.ok()
                .put("rows", forumMapper.getArticleForUser());
    }

    @PostMapping("/getComment")
    public R getComment(@RequestBody Map<String, Object> map) {
        return R.ok()
                .put("rows", forumMapper.getComment(map));
    }

    @PostMapping("/insertArticle")
    public R insertArticle(@RequestBody Map<String, Object> map) {
        map.put("datetime", System.currentTimeMillis());
        forumMapper.insertArticle(map);
        return R.ok();
    }

    @PostMapping("/insertComment")
    public R insertComment(@RequestBody Map<String, Object> map) {
        map.put("datetime", System.currentTimeMillis());
        forumMapper.insertComment(map);
        return R.ok();
    }

    /* ===================== 管理端 ===================== */

    @GetMapping("/admin/getArticle")
    public R getArticleAdmin() {
        return R.ok()
                .put("rows", forumMapper.getArticleForAdmin());
    }

    // 下线
    @PostMapping("/admin/offlineArticle")
    public R offlineArticle(@RequestBody Map<String, Object> map) {
        map.put("status", 0);
        forumMapper.updateArticleStatus(map);
        return R.ok();
    }

    // 上线
    @PostMapping("/admin/onlineArticle")
    public R onlineArticle(@RequestBody Map<String, Object> map) {
        map.put("status", 1);
        forumMapper.updateArticleStatus(map);
        return R.ok();
    }
}
