package com.example.spring_seatreservation.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface UserMapper {

    //插入后，得到刚插入的数据id(在承载实体里面)
    @Insert("insert reservation (uid,startTime,endTime,sid)" +
            " values(#{uid},#{startTime},#{endTime},#{sid})")
    @Options(useGeneratedKeys = true, keyProperty = "rid")
    void addReservation(Map<String, Object> map);

    @Select("SELECT a.*,b.*,c.* FROM reservation AS a \n" +
            "LEFT JOIN  seat AS b ON b.`sid`=a.`sid` \n" +
            "LEFT JOIN `area` AS c ON c.`aid`=b.`area` \n" +
            "WHERE a.`uid`=#{uid} ORDER BY a.`startTime` DESC, a.`rid` DESC")
    List<Map<String, Object>>  getReservationByUid(@Param("uid") Object uid);

    @Select("SELECT r.*, s.`row`, s.`column`, s.`area` AS aid, a.`areaName`, a.`subName` " +
            "FROM reservation r " +
            "LEFT JOIN seat s ON s.sid = r.sid " +
            "LEFT JOIN area a ON a.aid = s.area " +
            "WHERE r.uid = #{uid} AND r.state = 0 " +
            "ORDER BY r.startTime ASC")
    List<Map<String, Object>> getPendingSignReservationsByUid(@Param("uid") Object uid);


        @Select("SELECT * FROM reservation WHERE uid=#{uid} AND (state=0 OR state=1 OR state=3)")
    List<Map<String, Object>> getCurReservation(@Param("uid") Object uid);

            @Select("SELECT COUNT(*) FROM reservation " +
                    "WHERE uid=#{uid} AND state IN (0,1,3) AND endTime > #{nowTime}")
            int countFutureReservations(@Param("uid") Object uid,
                                        @Param("nowTime") Object nowTime);


        @Select("SELECT a.*,b.* FROM reservation AS a LEFT JOIN USER AS b ON b.`uid`=a.`uid` WHERE (state=2 OR state=4 ) AND a.`score` IS NOT NULL AND a.`uid`=#{uid}")
        List<Map<String, Object>> getReservation(Map<String, Object> map);


        @Update("update reservation set state=#{state} where rid=#{rid}")
        void updateReservation(@Param("state") Object state, @Param("rid") Object rid);

        @Select("select * from reservation where rid=#{rid}")
        Map<String, Object> getReservationByRid(@Param("rid") Object rid);

        @Select("select * from reservation where rid=#{rid} and uid=#{uid}")
        Map<String, Object> getReservationByRidAndUid(@Param("rid") Object rid,
                                                      @Param("uid") Object uid);

        @Update("update seat set state=#{state} where sid=#{sid}")
        void updateSeat(@Param("state") int state, @Param("sid") Object sid);

        @Update("update reservation set leaveTime=#{leaveTime}, state=3 where rid=#{rid}")
        void leaveReservation(@Param("leaveTime") Object leaveTime, @Param("rid") Object rid);

        @Update("update seat set state=1 where sid=#{sid}")
        void leaveSeat(@Param("sid") Object sid);

        @Select("select score from user where uid=#{uid}")
        int getScore(@Param("uid") Object uid);

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

        @Select("SELECT id, uid, rid, score, operator_uid AS operatorUid, operator_name AS operatorName, created_at AS createdAt " +
                "FROM score_log WHERE uid=#{uid} ORDER BY created_at DESC")
        List<Map<String, Object>> getScoreLogs(@Param("uid") Object uid);

        @Select("SELECT COUNT(*) FROM reservation " +
                "WHERE sid=#{sid} " +
                "AND state != -1 AND state != 2 AND state != 4 AND state != 5 " +
                "AND startTime < #{endTime} AND endTime > #{startTime}")
        int countSeatTimeOverlap(@Param("sid") Object sid,
                                 @Param("startTime") long startTime,
                                 @Param("endTime") long endTime);
}
