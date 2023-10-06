package com.kosa.showfan.artist.dao;

import com.kosa.showfan.artist.dto.ArtistDTO;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

public class ArtistDAO {
    private SqlSessionFactory sqlSessionFactory = null;

    public ArtistDAO(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public int insert(ArtistDTO artistDTO) {
        int id = -1;
        SqlSession session = sqlSessionFactory.openSession();

        try {
            id = session.insert("Artist.insertArtist", artistDTO);
        } finally {
            session.commit();
            session.close();
        }

        return id;
    }

    public ArtistDTO selectByName(String name) {
        ArtistDTO artistDTO = null;
        SqlSession session = sqlSessionFactory.openSession();
        try {
            artistDTO = session.selectOne("Artist.selectArtistByName", name);
        } finally {
            session.close();
        }

        return artistDTO;
    }
}
