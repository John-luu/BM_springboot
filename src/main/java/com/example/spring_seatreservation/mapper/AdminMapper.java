package com.example.spring_seatreservation.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
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
    "INSERT INTO area (areaName,subName, row_count, col_count) " +
    "VALUES (#{areaName}, #{subName},#{rows}, #{columns})"
)
void addArea(Map<String, Object> map);
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


    @Insert("insert seat(`area`,`type`,`row`,`column`)" +
            " values(${area},${type},${row},${column})")
    void addSeat(Map<String, Object> map);


    @Delete("delete from seat where sid=${sid}")
    void deleteSeat(Map<String, Object> map);

    @Delete("delete from article where id=${id}")
    void deleteArticle(Map<String, Object> map);

    @Delete("delete from announce where id=${id}")
    void deleteAnnounce(Map<String, Object> map);


    @Select("select * from user where type=1")
    List<Map<String, Object>> getTeacher();

    @Update("update user set password =#{password} where uid=${uid}")
    void updatePassword(Map<String, Object> map);

    @Select("SELECT `startTime`,`endTime` FROM reservation  WHERE state=-1 ORDER BY startTime")
    List<Map<String, Object>> getStatistics();

    @Select("SELECT COUNT(a.`uid`) as counter,b.`number`,b.`username` FROM reservation AS a \n" +
            "LEFT JOIN USER AS b ON b.`uid`=a.`uid`\n" +
            "WHERE a.`state`=-1 GROUP BY a.`uid` ORDER BY COUNT(a.`uid`)")
    List<Map<String, Object>> getUserCounter();
}
