package top.mowang.server;

import java.util.HashMap;

/**
 * MoChatClient
 * 管理服务端和客户端连接线程
 *
 * @author : Xuan Li
 * @date : 2021-09-20 20:55
 **/
public class ManageServerThread {
    /**
     * 把线程放入hashmap里面存储,key是用户名，value是通信的线程
     */
    private static HashMap<String, ServerConnectClientThread> hashMap = new HashMap<>();

    /**
     * 把线程放入hashmap集合中
     * @param key
     * @param clientConnectServerThread
     */
    public static void addClientConnectServerThread(String key, ServerConnectClientThread clientConnectServerThread){
        hashMap.put(key,clientConnectServerThread);
    }

    /**
     * 根据用户名获取该用户的通信线程
     * @param key
     * @return
     */
    public static ServerConnectClientThread getClientConnectServerThread(String key){
        return hashMap.get(key);
    }

}
