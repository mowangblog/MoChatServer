package top.mowang.server;

import top.mowang.common.Message;
import top.mowang.common.MessageType;
import top.mowang.utils.Utility;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * MoChatServer
 * 系统发送通知线程
 *
 * @author : Xuan Li
 * @date : 2021-09-21 13:59
 **/
public class ServerNotifyAllThread extends Thread{

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true){
            System.out.println("请输入你要发送的系统通知:");
            String content = Utility.readString(100);
            Message message = new Message();
            message.setMessageType(MessageType.MESSAGE_COMMON_SERVER_MES);
            message.setContent(content);
            message.setSender("服务器");
            message.setReceiver("所有人");
            message.setSendTime(Utility.getTime());
            try {
                for (String onlineUser : ManageServerThread.hashMap.keySet()) {
                    //循环给每一个在线用户发送通知
                    Socket newSocket = ManageServerThread.getClientConnectServerThread(
                            onlineUser).getSocket();
                    ObjectOutputStream outputStream = new ObjectOutputStream(newSocket.getOutputStream());
                    outputStream.writeObject(message);
                    outputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("通知以发送给当前在线的"+ManageServerThread.hashMap.size()+"个用户");
        }
    }
}
