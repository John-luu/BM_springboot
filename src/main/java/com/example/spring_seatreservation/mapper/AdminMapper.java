package com.example.spring_seatreservation.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface AdminMapper {
    @Update("update sport set full=${full},excellence=${excellence},good=${good},pass=${pass},fail=${fail}" +
            " where sid=${sid}")
    void updateSport(Map<String, Object> map);

    /* ================= 公告 ================= */

    @Select("SELECT * FROM announce ORDER BY id DESC")
    List<Map<String, Object>> getAnnounce();

    @Insert("insert announce(content,title,datetime)" +
            " values(#{content},#{title},#{datetime})")
    void addAnnounce(Map<String, Object> map);

    @Insert(
        "INSERT INTO area (areaName, subName, row_count, col_count) " +
        "VALUES (#{areaName}, #{subName}, #{rows}, #{columns})"
    )
    @Options(useGeneratedKeys = true, keyProperty = "aid")
    void addArea(Map<String, Object> map);

    /* ================= 学生搜索分页 ================= */
    @Select({
      "<script>",
      "SELECT * FROM user",
      "WHERE type = 0",
      "<if test='number != null and number != \"\"'>",
      "AND number LIKE CONCAT('%', #{number}, '%')",
      "</if>",
      "<if test='username != null and username != \"\"'>",
      "AND username LIKE CONCAT('%', #{username}, '%')",
      "</if>",
      "ORDER BY uid DESC",
      "LIMIT #{offset}, #{pageSize}",
      "</script>"
    })
    List<Map<String, Object>> searchUser(
        @Param("number") String number,
        @Param("username") String username,
        @Param("offset") Integer offset,
        @Param("pageSize") Integer pageSize
    );

    @Select({
      "<script>",
      "SELECT COUNT(*) FROM user",
      "WHERE type = 0",
      "<if test='number != null and number != \"\"'>",
      "AND number LIKE CONCAT('%', #{number}, '%')",
      "</if>",
      "<if test='username != null and username != \"\"'>",
      "AND username LIKE CONCAT('%', #{username}, '%')",
      "</if>",
      "</script>"
    })
    int countUser(
        @Param("number") String number,
        @Param("username") String username
    );

    /* ================= 教师搜索分页 ================= */
    @Select({
      "<script>",
      "SELECT * FROM user",
      "WHERE type = 1",
      "<if test='number != null and number != \"\"'>",
      "AND number LIKE CONCAT('%', #{number}, '%')",
      "</if>",
      "<if test='username != null and username != \"\"'>",
      "AND username LIKE CONCAT('%', #{username}, '%')",
      "</if>",
      "ORDER BY uid DESC",
      "LIMIT #{offset}, #{pageSize}",
      "</script>"
    })
    List<Map<String, Object>> searchTeacher(
        @Param("number") String number,
        @Param("username") String username,
        @Param("offset") Integer offset,
        @Param("pageSize") Integer pageSize
    );

    @Select({
      "<script>",
      "SELECT COUNT(*) FROM user",
      "WHERE type = 1",
      "<if test='number != null and number != \"\"'>",
      "AND number LIKE CONCAT('%', #{number}, '%')",
      "</if>",
      "<if test='username != null and username != \"\"'>",
      "AND username LIKE CONCAT('%', #{username}, '%')",
      "</if>",
      "</script>"
    })
    int countTeacher(
        @Param("number") String number,
        @Param("username") String username
    );

        @Insert("insert seat(`area`,`type`,`row`,`column`,`state`)" +
          " values(${area},${type},${row},${column},0)")
    void addSeat(Map<String, Object> map);

    @Insert({
      "<script>",
      "INSERT INTO seat (`area`,`type`,`row`,`column`,`state`) VALUES",
      "<foreach collection='rows' item='item' separator=','>",
        "(#{area}, #{type}, #{item.row}, #{item.column}, 0)",
      "</foreach>",
      "</script>"
    })
    void addSeatsBatch(
        @Param("area") Integer area,
        @Param("type") Integer type,
        @Param("rows") List<Map<String, Integer>> rows
    );

    @Delete("delete from reservation where sid=#{sid}")
    void deleteReservationBySid(@Param("sid") Integer sid);

    @Delete("delete from seat where sid=#{sid}")
    void deleteSeatBySid(@Param("sid") Integer sid);

    default void deleteSeat(Map<String, Object> map) {
      if (map == null || map.get("sid") == null) {
        return;
      }
      Integer sid = Integer.parseInt(String.valueOf(map.get("sid")));
      deleteReservationBySid(sid);
      deleteSeatBySid(sid);
    }

    @Delete("delete from article where id=${id}")
    void deleteArticle(Map<String, Object> map);

    @Delete("delete from announce where id=${id}")
    void deleteAnnounce(Map<String, Object> map);

    @Select("select * from user where type=1")
    List<Map<String, Object>> getTeacher();

    @Update("update user set password =#{password} where uid=${uid}")
    void updatePassword(Map<String, Object> map);

    @Select({
      "<script>",
      "SELECT r.rid, r.uid, r.sid, r.startTime, r.endTime, r.leaveTime, r.state, r.score, r.scoreStatus,",
      "u.username, s.row, s.column, a.subName",
      "FROM reservation r",
      "LEFT JOIN user u ON u.uid = r.uid",
      "LEFT JOIN seat s ON s.sid = r.sid",
      "LEFT JOIN area a ON a.aid = s.area",
      "WHERE r.state != 5",
      "<if test='date != null and date != \"\"'>",
      "AND DATE(r.startTime) = #{date}",
      "</if>",
      "ORDER BY r.rid DESC",
      "LIMIT #{offset}, #{pageSize}",
      "</script>"
    })
    List<Map<String, Object>> getReservationPage(
        @Param("offset") Integer offset,
        @Param("pageSize") Integer pageSize,
        @Param("date") String date
    );

    @Select({
      "<script>",
      "SELECT COUNT(*) FROM reservation r",
      "WHERE r.state != 5",
      "<if test='date != null and date != \"\"'>",
      "AND DATE(r.startTime) = #{date}",
      "</if>",
      "</script>"
    })
    int countReservation(@Param("date") String date);

    @Select({
      "<script>",
      "SELECT",
      "COUNT(*) AS reserveTotal,",
      "SUM(CASE WHEN r.state IN (1, 3, -1) THEN 1 ELSE 0 END) AS signTotal,",
      "SUM(CASE WHEN r.state = -1 THEN 1 ELSE 0 END) AS finishTotal,",
      "SUM(CASE WHEN r.state = 3 THEN 1 ELSE 0 END) AS leaveTotal,",
      "SUM(CASE WHEN r.state IN (2, 4) THEN 1 ELSE 0 END) AS violateTotal",
      "FROM reservation r",
      "WHERE r.state != 5",
      "<if test='date != null and date != \"\"'>",
      "AND DATE(r.startTime) = #{date}",
      "</if>",
      "</script>"
    })
    Map<String, Object> getReservationSummary(@Param("date") String date);

    @Select({
      "<script>",
      "SELECT r.rid, r.uid, r.startTime, r.endTime, r.state, r.score, r.scoreStatus, u.username",
      "FROM reservation r",
      "LEFT JOIN user u ON u.uid = r.uid",
      "WHERE r.state IN (2,4)",
      "<if test='username != null and username != \"\"'>",
      "AND u.username LIKE CONCAT('%', #{username}, '%')",
      "</if>",
      "<if test='state != null'>",
      "AND r.state = #{state}",
      "</if>",
      "<if test='date != null and date != \"\"'>",
      "AND DATE(r.startTime) = #{date}",
      "</if>",
      "<if test='scoreStatus != null'>",
      "<choose>",
      "<when test='scoreStatus == 0'>",
      "AND (r.score IS NULL OR r.score = 0)",
      "</when>",
      "<otherwise>",
      "AND r.score IS NOT NULL AND r.score &gt; 0",
      "</otherwise>",
      "</choose>",
      "</if>",
      "ORDER BY r.rid DESC",
      "LIMIT #{offset}, #{pageSize}",
      "</script>"
    })
    List<Map<String, Object>> getScoreReservationPage(Map<String, Object> params);

    @Select({
      "<script>",
      "SELECT COUNT(*)",
      "FROM reservation r",
      "LEFT JOIN user u ON u.uid = r.uid",
      "WHERE r.state IN (2,4)",
      "<if test='username != null and username != \"\"'>",
      "AND u.username LIKE CONCAT('%', #{username}, '%')",
      "</if>",
      "<if test='state != null'>",
      "AND r.state = #{state}",
      "</if>",
      "<if test='date != null and date != \"\"'>",
      "AND DATE(r.startTime) = #{date}",
      "</if>",
      "<if test='scoreStatus != null'>",
      "<choose>",
      "<when test='scoreStatus == 0'>",
      "AND (r.score IS NULL OR r.score = 0)",
      "</when>",
      "<otherwise>",
      "AND r.score IS NOT NULL AND r.score &gt; 0",
      "</otherwise>",
      "</choose>",
      "</if>",
      "</script>"
    })
    int countScoreReservation(Map<String, Object> params);

        @Update("UPDATE reservation SET score=#{score}, scoreStatus=1 " +
          "WHERE rid=#{rid} AND (score IS NULL OR score=0)")
        int subReservationScore(@Param("rid") Integer rid,
              @Param("score") Integer score);

        @Update("UPDATE user SET score = IF(score >= #{score}, score - #{score}, 0) WHERE uid=#{uid}")
        int subUserScore(@Param("uid") Integer uid,
             @Param("score") Integer score);

            @Update("CREATE TABLE IF NOT EXISTS score_log (" +
              "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
              "uid BIGINT NOT NULL," +
              "rid BIGINT NULL," +
              "score INT NOT NULL," +
              "operator_uid BIGINT NULL," +
              "operator_name VARCHAR(64) NOT NULL DEFAULT '管理员'," +
              "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
              "INDEX idx_uid_created (uid, created_at)," +
              "INDEX idx_rid (rid)" +
              ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
            void ensureScoreLogTable();

            @Insert("INSERT INTO score_log(uid, rid, score, operator_uid, operator_name, created_at) " +
              "VALUES(#{uid}, #{rid}, #{score}, #{operatorUid}, #{operatorName}, NOW())")
            int addScoreLog(@Param("uid") Integer uid,
                @Param("rid") Integer rid,
                @Param("score") Integer score,
                @Param("operatorUid") Long operatorUid,
                @Param("operatorName") String operatorName);

    @Select("SELECT `startTime`,`endTime` FROM reservation  WHERE state=-1 AND state!=5 ORDER BY startTime")
    List<Map<String, Object>> getStatistics();

    @Select("SELECT COUNT(a.`uid`) as counter,b.`number`,b.`username` FROM reservation AS a \n" +
            "LEFT JOIN USER AS b ON b.`uid`=a.`uid`\n" +
          "WHERE a.`state`=-1 AND a.`state`!=5 GROUP BY a.`uid` ORDER BY COUNT(a.`uid`)")
    List<Map<String, Object>> getUserCounter();

        @Select({
          "<script>",
          "SELECT u.uid, u.number, u.username, COUNT(*) AS signCount",
          "FROM reservation r",
          "LEFT JOIN user u ON u.uid = r.uid",
          "WHERE r.state IN (1, 3, -1)",
          "AND r.state != 5",
          "AND u.type = 0",
          "<if test='startTime != null'>",
          "AND r.startTime <![CDATA[>=]]> FROM_UNIXTIME(#{startTime} / 1000)",
          "</if>",
          "<if test='endTime != null'>",
          "AND r.startTime <![CDATA[<=]]> FROM_UNIXTIME(#{endTime} / 1000)",
          "</if>",
          "GROUP BY u.uid, u.number, u.username",
          "ORDER BY signCount DESC, u.uid ASC",
          "LIMIT 20",
          "</script>"
        })
        List<Map<String, Object>> getSignLeaderboard(
          @Param("startTime") Long startTime,
          @Param("endTime") Long endTime
        );

        @Select({
          "<script>",
          "SELECT DATE_FORMAT(r.startTime, '%Y-%m') AS period, COUNT(*) AS total",
          "FROM reservation r",
          "WHERE r.state != 5",
          "<if test='startTime != null'>",
          "AND r.startTime <![CDATA[>=]]> FROM_UNIXTIME(#{startTime} / 1000)",
          "</if>",
          "<if test='endTime != null'>",
          "AND r.startTime <![CDATA[<=]]> FROM_UNIXTIME(#{endTime} / 1000)",
          "</if>",
          "GROUP BY DATE_FORMAT(r.startTime, '%Y-%m')",
          "ORDER BY total DESC, period DESC",
          "LIMIT 12",
          "</script>"
        })
        List<Map<String, Object>> getMonthReservationRanking(
          @Param("startTime") Long startTime,
          @Param("endTime") Long endTime
        );

        @Select({
          "<script>",
          "SELECT DATE_FORMAT(r.startTime, '%Y-%m-%d') AS period, COUNT(*) AS total",
          "FROM reservation r",
          "WHERE r.state != 5",
          "<if test='startTime != null'>",
          "AND r.startTime <![CDATA[>=]]> FROM_UNIXTIME(#{startTime} / 1000)",
          "</if>",
          "<if test='endTime != null'>",
          "AND r.startTime <![CDATA[<=]]> FROM_UNIXTIME(#{endTime} / 1000)",
          "</if>",
          "GROUP BY DATE_FORMAT(r.startTime, '%Y-%m-%d')",
          "ORDER BY total DESC, period DESC",
          "LIMIT 31",
          "</script>"
        })
        List<Map<String, Object>> getDayReservationRanking(
          @Param("startTime") Long startTime,
          @Param("endTime") Long endTime
        );

        @Select({
          "<script>",
          "SELECT (HOUR(r.startTime) * 2 + IF(MINUTE(r.startTime) &gt;= 30, 1, 0)) AS slotIndex,",
          "COUNT(*) AS total",
          "FROM reservation r",
          "WHERE r.state != 5",
          "<if test='startTime != null'>",
          "AND r.startTime <![CDATA[>=]]> FROM_UNIXTIME(#{startTime} / 1000)",
          "</if>",
          "<if test='endTime != null'>",
          "AND r.startTime <![CDATA[<=]]> FROM_UNIXTIME(#{endTime} / 1000)",
          "</if>",
          "GROUP BY (HOUR(r.startTime) * 2 + IF(MINUTE(r.startTime) &gt;= 30, 1, 0))",
          "ORDER BY total DESC, slotIndex ASC",
          "LIMIT 12",
          "</script>"
        })
        List<Map<String, Object>> getTimeSlotReservationRanking(
          @Param("startTime") Long startTime,
          @Param("endTime") Long endTime
        );

        @Select({
          "<script>",
          "SELECT",
          "COUNT(*) AS reserveTotal,",
          "SUM(CASE WHEN r.state IN (1, 3, -1) THEN 1 ELSE 0 END) AS signTotal,",
          "SUM(CASE WHEN r.state = -1 THEN 1 ELSE 0 END) AS finishTotal,",
          "SUM(CASE WHEN r.state = 3 THEN 1 ELSE 0 END) AS leaveTotal,",
          "SUM(CASE WHEN r.state IN (2, 4) THEN 1 ELSE 0 END) AS violateTotal",
          "FROM reservation r",
          "WHERE DATE(r.startTime) = #{date}",
          "AND r.state != 5",
          "</script>"
        })
        Map<String, Object> getDayStatisticsSummary(@Param("date") String date);

        @Select({
          "<script>",
          "SELECT",
          "r.rid, r.uid, u.number, u.username, r.sid,",
          "s.row, s.column, a.subName,",
          "DATE_FORMAT(r.startTime, '%H:%i') AS startAt,",
          "DATE_FORMAT(r.endTime, '%H:%i') AS endAt,",
          "r.state",
          "FROM reservation r",
          "LEFT JOIN user u ON u.uid = r.uid",
          "LEFT JOIN seat s ON s.sid = r.sid",
          "LEFT JOIN area a ON a.aid = s.area",
          "WHERE DATE(r.startTime) = #{date}",
          "AND r.state != 5",
          "AND u.type = 0",
          "ORDER BY r.startTime ASC, r.rid ASC",
          "LIMIT 500",
          "</script>"
        })
        List<Map<String, Object>> getDayStatisticsUsers(@Param("date") String date);
}