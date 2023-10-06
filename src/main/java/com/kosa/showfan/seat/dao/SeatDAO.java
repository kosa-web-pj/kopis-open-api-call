package com.kosa.showfan.seat.dao;

import com.kosa.showfan.seat.dto.SeatDTO;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.List;

public class SeatDAO {
    private SqlSessionFactory sqlSessionFactory = null;

    public SeatDAO(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public int insert(SeatDTO seatDTO) {
        int id = -1;
        SqlSession session = sqlSessionFactory.openSession();

        try {
            id = session.insert("Seat.insertSeat", seatDTO);
        } finally {
            session.commit();
            session.close();
        }

        return id;
    }

    public List<SeatDTO> selectByShowId(String showId) {
        List<SeatDTO> seatDTOList = null;
        SqlSession session = sqlSessionFactory.openSession();
        try {
            seatDTOList = session.selectList("Cast.selectSeatByShowId", showId);
        } finally {
            session.close();
        }

        return seatDTOList;
    }
}
