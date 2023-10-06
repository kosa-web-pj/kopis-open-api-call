package com.kosa.showfan.show.dao;

import com.kosa.showfan.show.dto.ShowDTO;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

public class ShowDAO {
    private SqlSessionFactory sqlSessionFactory = null;

    public ShowDAO(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public int insert(ShowDTO showDTO) {
        int id = -1;
        SqlSession session = sqlSessionFactory.openSession();

        try {
            id = session.insert("Show.insertShow", showDTO);
        } finally {
            session.commit();
            session.close();
        }

        return id;
    }

    public ShowDTO selectById(String id) {
        ShowDTO showDTO = null;
        SqlSession session = sqlSessionFactory.openSession();
        try {
            showDTO = session.selectOne("Show.selectShowById", id);
        } finally {
            session.close();
        }

        return showDTO;
    }
}
