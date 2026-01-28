package com.example.spring_seatreservation.mapper;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
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
        "row_count as rows, " +      // 别名：数据库字段 → 前端字段
        "col_count as columns " +    // 别名：数据库字段 → 前端字段
        "FROM area")
List<Map<String, Object>> getArea();

    @Select("select * from seat where area=${area}")
    List<Map<String, Object>> getAreaSeats(Map<String, Object> map);

    @Select("SELECT * FROM reservation WHERE state!=-1 and state !=2 and state != 4")
    List<Map<String, Object>> getNeedCheckReservation();

    @Update("update reservation set state=${state} where rid=${rid}")
    void updateReservation(Object state, Object rid);

    @Select("SELECT * FROM reservation WHERE sid=${sid} AND state=0 OR state=3")
    Map<String, Object> getReservationBySid(Object rid);
}
