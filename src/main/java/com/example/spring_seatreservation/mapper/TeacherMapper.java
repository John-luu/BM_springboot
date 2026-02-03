package com.example.spring_seatreservation.mapper;


import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.spring_seatreservation.common.R;

import java.util.List;
import java.util.Map;
public interface TeacherMapper {

    // 分页查询所有预约
    @Select(
        "SELECT a.*, b.*, c.*, d.*, d.score AS uScore " +
        "FROM reservation AS a " +
        "LEFT JOIN seat AS b ON b.sid = a.sid " +
        "LEFT JOIN area AS c ON c.aid = b.area " +
        "LEFT JOIN USER AS d ON d.uid = a.uid " +
        "ORDER BY a.rid DESC " +
        "LIMIT #{offset}, #{pageSize}"
    )
    List<Map<String, Object>> getReservationPage(
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    @Select("SELECT COUNT(*) FROM reservation")
    Integer getReservationTotal();

    // 分页查询需要扣分的预约
    @Select(
        "SELECT a.*, b.* " +
        "FROM reservation AS a " +
        "LEFT JOIN USER AS b ON b.uid = a.uid " +
        "WHERE a.state = 2 OR a.state = 4 " +
        "ORDER BY a.rid DESC " +
        "LIMIT #{offset}, #{pageSize}"
    )
    List<Map<String, Object>> getReservationNeedSubPage(
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    @Select("SELECT COUNT(*) FROM reservation WHERE state = 2 OR state = 4")
    Integer getReservationNeedSubTotal();

    // 扣用户分
    @Update("UPDATE user SET score = score - 10 WHERE uid = #{uid}")
    void subScore(@Param("uid") Integer uid);

    // 更新预约扣分状态
    @Update("UPDATE reservation SET score = 10 WHERE rid = #{rid}")
    void subReservationScore(@Param("rid") Integer rid);
}
