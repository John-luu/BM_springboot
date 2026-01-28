package com.example.spring_seatreservation.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface ForumMapper {

    /* ================= 帖子 ================= */

    // ✅ 普通用户：只能看到正常帖子
    @Select("SELECT * FROM article WHERE status = 1 ORDER BY id DESC")
    List<Map<String, Object>> getArticleForUser();

    // ✅ 管理员：看到所有帖子（含下架）
    @Select("SELECT * FROM article ORDER BY id DESC")
    List<Map<String, Object>> getArticleForAdmin();

    // ✅ 发帖（默认 status = 1）
    @Insert("INSERT INTO article(title, content, datetime, uid, status) " +
            "VALUES(#{title}, #{content}, #{datetime}, #{uid}, 1)")
    void insertArticle(Map<String, Object> map);

    // ✅ 下架 +恢复
    @Update("UPDATE article SET status = #{status} WHERE id = #{id}")
    void updateArticleStatus(Map<String, Object> map);
    /* ================= 评论 ================= */

    // ✅ 插入评论
    @Insert("INSERT INTO comments(content, uid, datetime, aid) " +
            "VALUES(#{content}, #{uid}, #{datetime}, #{aid})")
    void insertComment(Map<String, Object> map);

    // ✅ 查询评论（只显示未下架帖子的评论）
    @Select("SELECT a.*, b.username FROM comments a " +
            "LEFT JOIN user b ON b.uid = a.uid " +
            "WHERE aid = #{aid} ORDER BY cid DESC")
    List<Map<String, Object>> getComment(Map<String, Object> map);
}
