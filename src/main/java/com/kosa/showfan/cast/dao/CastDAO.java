package com.kosa.showfan.cast.dao;

import com.kosa.showfan.cast.dto.CastDTO;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.List;

public class CastDAO {
    private SqlSessionFactory sqlSessionFactory = null;

    public CastDAO(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public int insert(CastDTO castDTO) {
        int id = -1;
        SqlSession session = sqlSessionFactory.openSession();

        try {
            id = session.insert("Cast.insertCast", castDTO);
        } finally {
            session.commit();
            session.close();
        }

        return id;
    }

    public List<CastDTO> selectByShowId(String showId) {
        List<CastDTO> castDTOList = null;
        SqlSession session = sqlSessionFactory.openSession();
        try {
            castDTOList = session.selectList("Cast.selectCastByShowId", showId);
        } finally {
            session.close();
        }

        return castDTOList;
    }
}
