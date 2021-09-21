package top.mowang.server;

import top.mowang.common.Message;
import top.mowang.common.MessageType;
import top.mowang.common.User;
import top.mowang.utils.Utility;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * MoChatClient
 * 服务端和客户端通信线程
 *
 * @author : Xuan Li
 * @date : 2021-09-20 20:46
 **/
@SuppressWarnings("all")
public class ServerConnectClientThread extends Thread {

    private Socket socket;

    private String userName;

    public ServerConnectClientThread(Socket socket, String userName) {
        this.socket = socket;
        this.userName = userName;
    }

    public Socket getSocket() {
        return socket;
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
        while (true) {
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                //如果客户端器没有发送信息过来，线程会阻塞在这里
                Message message = (Message) ois.readObject();
                //根据用户发送的行为消息，进行业务处理
                if (MessageType.MESSAGE_GET_ONLINE_LIST.equals(message.getMessageType())) {
                    //发送用户列表
                    ObjectOutputStream oos =
                            new ObjectOutputStream(socket.getOutputStream());
                    getOnlineUser(message, oos);
                } else if (MessageType.MESSAGE_COMMON_MES.equals(message.getMessageType())) {
                    privateMessage(message);
                } else if (MessageType.MESSAGE_COMMON_PUBLIC_MES.equals(message.getMessageType())) {
                    publicMessage(message);
                } else if (MessageType.MESSAGE_FILE_MES.equals(message.getMessageType())) {
                    //因为文件和信息的转发逻辑是一样的所在直接用信息转发的了
                    privateMessage(message);
                } else if (MessageType.MESSAGE_CLIENT_EXIT.equals(message.getMessageType())) {
                    System.out.println(userName + "和服务器断开了连接,离开了");
                    ManageServerThread.removeConnectClientThread(userName);
                    //关闭连接
                    socket.close();
                    break;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println(userName + "和服务器的连接中断,离开了");
                ManageServerThread.removeConnectClientThread(userName);
                try {
                    //关闭连接
                    socket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                break;
            }
        }
    }


    private void publicMessage(Message message) {
        //拿到所有在线用户的的连接
        try {
            for (String onlineUser : ManageServerThread.hashMap.keySet()) {
                //循环发送给除发送者外的每一个在线用户
                if (!onlineUser.equals(message.getSender())) {
                    Socket newSocket = ManageServerThread.getClientConnectServerThread(
                            onlineUser).getSocket();
                    ObjectOutputStream outputStream = new ObjectOutputStream(newSocket.getOutputStream());
                    outputStream.writeObject(message);
                    outputStream.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 用户私聊
     *
     * @param message
     */
    public void privateMessage(Message message) {
        //如果要发送的用户不在线或不存在
        if (ManageServerThread.hashMap.get(message.getReceiver()) == null) {
            message.setReceiver(message.getSender());
            message.setSender("服务器");
            message.setContent("发送失败，用户不在线或不存在");
            message.setMessageType(MessageType.MESSAGE_COMMON_SERVER_MES);
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(message);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        //拿到接收者的连接
        try {
            Socket newSocket = ManageServerThread.getClientConnectServerThread(
                    message.getReceiver()).getSocket();
            ObjectOutputStream outputStream = new ObjectOutputStream(newSocket.getOutputStream());
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过线程集合获取当前在线用户列表
     */
    public void getOnlineUser(Message oldMessage, ObjectOutputStream oos) throws IOException {
        Message message = new Message();
        message.setMessageType(MessageType.MESSAGE_RET_ONLINE_LIST);
        message.setReceiver(oldMessage.getSender());
        message.setSender(oldMessage.getReceiver());
        message.setSendTime(Utility.getTime());
        StringBuilder str = new StringBuilder();
        for (String value : ManageServerThread.hashMap.keySet()) {
            str.append(value).append(" ");
        }
        message.setContent(str.toString());
        oos.writeObject(message);
        oos.flush();
    }
}
