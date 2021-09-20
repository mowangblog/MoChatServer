package top.mowang.server;

import top.mowang.common.Message;
import top.mowang.common.MessageType;
import top.mowang.common.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MoChatServer
 * 服务器
 *
 * @author : Xuan Li
 * @date : 2021-09-20 21:15
 **/
@SuppressWarnings("all")
public class MoChatServer {
    private ServerSocket serverSocket = null;
    //ConcurrentHashMap线程安全
    private static ConcurrentHashMap<String, User> userData = new ConcurrentHashMap<>();
    private static Properties properties = new Properties();
    static {
        //静态代码启动服务的时候从dat文件里面初始化用户信息
        try {
            properties.load(new FileReader("src/set.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String adress = (String) properties.get("dataAddress");
        File file = new File(adress);
        if(file.exists()){
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
                int num = inputStream.readInt();
                for (int i = 0; i < num; i++) {
                    User user = (User) inputStream.readObject();
                    userData.put(user.getUserName(),user);
                }
                System.out.println("已从dat文件中恢复"+num+"个用户");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new MoChatServer();
    }

    /**
     * 判断用户是否存在
     * @param user
     * @return
     */
    public boolean checkUser(User user) {
        User check = userData.get(user.getUserName());
        if (check == null) {
            return false;
        }
        if(!check.getPassWord().equals(user.getPassWord())){
            return false;
        }
        return true;
    }

    /**
     * 把map里面的数据持久化到dat文件
     */
    public void persistence(){
        String adress = (String) properties.get("dataAddress");
        File file = new File(adress);
        ObjectOutputStream outputStream = null;
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeInt(userData.size());
            for (User value : userData.values()) {
                outputStream.writeObject(value);
            }
            System.out.println("用户数据已保存");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public MoChatServer() {
        System.out.println("服务端在9999端口监听");
        try {
            //从Properties配置文件里读取端口号
            String port = (String) properties.get("port");

            serverSocket = new ServerSocket(Integer.parseInt(port));
            while (true) {
                Socket socket = serverSocket.accept();
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                Message actionMessage = (Message) inputStream.readObject();
                User user = (User) inputStream.readObject();
                System.out.println(user.getUserName() + "和服务端连接成功");
                Message message = new Message();
                if (actionMessage.getMessageType().equals(MessageType.MESSAGE_LOGIN)) {
                    //如果客户端需要登录
                    if (checkUser(user)) {
                        //登录成功
                        System.out.println(user.getUserName() + "登录成功");
                        message.setMessageType(MessageType.MESSAGE_LOGIN_SUCCEED);
                        objectOutputStream.writeObject(message);
                        objectOutputStream.flush();
                        //启动和该用户的通信线程，保持连接
                        ServerConnectClientThread serverConnectClientThread =
                                new ServerConnectClientThread(socket, user.getUserName());
                        serverConnectClientThread.start();
                        //把线程放入集合中进行管理
                        ManageServerThread.addClientConnectServerThread(user.getUserName(), serverConnectClientThread);
                    } else {
                        //登录失败
                        message.setMessageType(MessageType.MESSAGE_LOGIN_FAIL);
                        objectOutputStream.writeObject(message);
                        objectOutputStream.flush();
                        //登录失败需要关闭socket
                        if (socket != null) {
                            socket.close();
                        }
                        System.out.println(user.getUserName() + "登录失败");
                    }
                } else if(actionMessage.getMessageType().equals(MessageType.MESSAGE_REGISTER)){
                    boolean isOk = false;
                    //如果客户端需要注册
                    if(userData.get(user.getUserName()) != null){
                        //用户存在注册失败
                        message.setMessageType(MessageType.MESSAGE_REGISTER_FAIL);
                    }else {
                        userData.put(user.getUserName(), user);
                        message.setMessageType(MessageType.MESSAGE_REGISTER_SUCCEED);
                        isOk = true;
                    }
                    objectOutputStream.writeObject(message);
                    objectOutputStream.flush();
                    if (socket != null) {
                        socket.close();
                    }
                    //把注册的用户写入到文件里进行存储
                    if (isOk) {
                        persistence();
                        System.out.println(user.getUserName()+"注册成功");
                    }else {
                        System.out.println(user.getUserName()+"注册失败，用户已存在");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
