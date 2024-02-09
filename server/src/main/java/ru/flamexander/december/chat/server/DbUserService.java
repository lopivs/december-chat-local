package ru.flamexander.december.chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbUserService implements UserService, UserRole, AutoCloseable {
    class User {
        private String login;
        private String password;
        private String username;
        private boolean isBlocked;
        private int role_id;

        public User(String login, String password, String username, int role_id, boolean isBlocked) {
            this.login = login;
            this.password = password;
            this.username = username;
            this.isBlocked = isBlocked;
            this.role_id = role_id;
        }
    }

    private List<DbUserService.User> users;
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/december_chat";
    private static final String DB_LOGIN = "postgres";
    private static final String DB_PASSWORD = "postgres";
    private static Connection connect;
    private static Statement statement;
    private static PreparedStatement pStatement;

    private void connect() throws SQLException {
        connect = DriverManager.getConnection(DB_URL, DB_LOGIN, DB_PASSWORD);
        statement = connect.createStatement();
        System.out.println("Подключение к БД прошло успешно!");
    }

    public DbUserService() {

        try {
            connect();
            List<DbUserService.User> users = new ArrayList<>();
            ResultSet rs = statement.executeQuery("Select login, user_name, password, role_id, isblocked from users");
            while (rs.next()) {
                users.add(new User(rs.getString("login"),
                        rs.getString("user_name"),
                        rs.getString("password"),
                        rs.getInt("role_id"),
                        rs.getBoolean("isBlocked")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        List<DbUserService.User> users = new ArrayList<>();
        try {
            pStatement = connect.prepareStatement("Select user_name from users where login = ? and password = ?");
            pStatement.setString(1, login);
            pStatement.setString(2, password);
            System.out.println("Попытка авторизации " + login + " " + password);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    System.out.println("Авторизация, пользователь найден: " + rs.getString("user_name"));
                    return rs.getString("user_name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void createNewUser(String login, String password, String username) {
        try {
            pStatement = connect.prepareStatement(
                    "INSERT INTO users(login, user_name, password, role_id, isblocked) VALUES(?, ?, ?, ?, ?) ");
            connect.setAutoCommit(true);
            pStatement.setString(1, login);
            pStatement.setString(2, username);
            pStatement.setString(2, password);
            pStatement.setInt(2, 2);
            pStatement.setBoolean(2, false);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        try {
            pStatement = connect.prepareStatement("Select 1 from users where login = ?");
            pStatement.setString(1, login);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isUsernameAlreadyExist(String username) {
        try {
            pStatement = connect.prepareStatement("Select 1 from users where user_name = ?");
            pStatement.setString(1, username);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getRoleByUserName(String username) {// пришлось оставить строку т.к. до этого описал метод со строкой в интерфейсе и заюзал, лень переписывать
        try {
            pStatement = connect.prepareStatement("Select role_name from users u, acs_role r where user_name = ? and u.role_id = r.id");
            pStatement.setString(1, username);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setUserBlock(String username) {
        try {
            pStatement = connect.prepareStatement("update users set isBlocked = true where user_name = ?");
            connect.setAutoCommit(true);
            pStatement.setString(1, username);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setUserUnblock(String username) {
        try (PreparedStatement pStatement = connect.prepareStatement("update users set isBlocked = false where user_name = ?")) {
            connect.setAutoCommit(true);
            pStatement.setString(1, username);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean getAccessByRoleNameAndAccessName(String role, String access) {
        System.out.println("Запрос доступа " + access + " для роли " + role);
        try {
            pStatement = connect.prepareStatement("select true from acs_role r, link_role_acc_list l, acc_list a " +
                    "where r.role_name = ? and  r.id = l.role_id and l.access_id = a.id and a.access_name = ?");
            pStatement.setString(1, role);
            pStatement.setString(2, access);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void addRole(String role) {
        try {
            pStatement = connect.prepareStatement(
                    "INSERT INTO acs_role(role_name) VALUES(?) ");
            connect.setAutoCommit(true);
            pStatement.setString(1, role);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAccess(String role, String accessName) {
        //roles.put(role);
    }

    @Override
    public void close() {
        disconnect();
    }

    public void disconnect() {
        try {
            if (pStatement != null) {
                pStatement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connect != null) {
                connect.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

