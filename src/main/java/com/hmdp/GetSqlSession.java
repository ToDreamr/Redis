package com.hmdp;

import org.apache.ibatis.session.SqlSession;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * <p>
 * GetSqlSession
 * </p>
 *
 * @author 春江花朝秋月夜
 * @since 2023/8/9 1:32
 */
public class GetSqlSession {
    public static Connection getConnection(String url,String username,String password){
        Connection connection;
        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }
}
