package com.example.spring_seatreservation.mapper;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.spring_seatreservation.common.MyUser;

import java.util.List;
import java.util.Map;

public interface PublicMapper {


    @Insert("insert into user(number,username,password,type) " +
            "values(${number},#{username},#{password},${type})")
    void insertUser(MyUser user);

    @Select("select * from user where number=${number}")
    MyUser getUserByNumber(long number);

    @Update("update user set password=#{password} where number=#{number}")
    void updatePwd(String password, long number);

    @Select("SELECT " +
        "aid, " +
        "areaName, " +
        "subName, " +
        "row_count as `rows`, " +      // 别名：数据库字段 → 前端字段
        "col_count as `columns` " +    // 别名：数据库字段 → 前端字段
        "FROM area")
List<Map<String, Object>> getArea();

    @Select("select * from seat where area=${area}")
    List<Map<String, Object>> getAreaSeats(Map<String, Object> map);

    /**
     * 查询在指定区域、指定时间段内已被预约（有重叠时间段）的座位 sid 列表
     *
     * 时间段重叠条件：r.startTime < endTime AND r.endTime > startTime
     * 预约状态：排除已完成/已失效的状态，参考 getNeedCheckReservation
     */
    @Select("SELECT s.sid " +
            "FROM seat s " +
            "JOIN reservation r ON s.sid = r.sid " +
            "WHERE s.area = #{area} " +
            "AND r.state != -1 AND r.state != 2 AND r.state != 4 AND r.state != 5 " +
            "AND r.startTime < #{endTime} " +
            "AND r.endTime > #{startTime}")
    List<Integer> getReservedSeatIdsInArea(@Param("area") int area,
                                           @Param("startTime") long startTime,
                                           @Param("endTime") long endTime);
// 删除指定区域的所有座位
@Delete("DELETE FROM seat WHERE area = #{areaId}")
void deleteSeatsByArea(@Param("areaId") Integer areaId);

// 删除区域
@Delete("DELETE FROM area WHERE aid = #{areaId}")
void deleteArea(@Param("areaId") Integer areaId);

    @Select("SELECT * FROM reservation WHERE state!=-1 and state !=2 and state != 4 and state != 5")
    List<Map<String, Object>> getNeedCheckReservation();

    @Update("update reservation set state=${state} where rid=${rid}")
    void updateReservation(Object state, Object rid);

    @Select("SELECT * FROM reservation WHERE sid=#{sid} AND (state=0 OR state=3)")
    Map<String, Object> getReservationBySid(Object rid);
}
