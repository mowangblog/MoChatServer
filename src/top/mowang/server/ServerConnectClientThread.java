package top.mowang.server;

import top.mowang.common.Message;

import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * MoChatClient
 * 服务端和客户端通信线程
 *
 * @author : Xuan Li
 * @date : 2021-09-20 20:46
 **/
public class ServerConnectClientThread extends Thread{

    private Socket socket;

    private String userName;

    public ServerConnectClientThread(Socket socket, String userName) {
        this.socket = socket;
        this.userName = userName;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ServerConnectClientThread(Socket socket) {
        this.socket = socket;
    }

    /**
     * 建立线程和服务端保持通信
     */
    @Override
    public void run() {
        //无限循环保持和某个客户端通信
        while (true){
            System.out.println("服务端和客户端"+userName+"保持连接中");
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(socket.getInputStream());
                //如果客户端器没有发送信息过来，线程会阻塞在这里
                Message message = (Message) ois.readObject();

            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println(userName+"和服务器的连接中断");
                break;
            }
        }
    }
}
