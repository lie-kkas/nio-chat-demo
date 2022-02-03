package cn.liuwei;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * 聊天室服务器
 *
 * @author 6v
 */
public class ChatServer {

    public static void main(String[] args) throws IOException {
        new ChatServer().startServer();
    }

    /**
     * 启动服务端
     */
    public void startServer() throws IOException {
        //创建selector
        Selector selector = Selector.open();
        //创建ServerSocketChannel并绑定端口
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 8000));
        //设置channel非阻塞
        serverSocketChannel.configureBlocking(false);
        //将channel接收事件注册到selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务端启动成功");
        //循环判断selector是否有就绪状态的key
        while (true) {
            int count = selector.select();
            if (count == 0) {
                continue;
            }
            //遍历key 分别处理accept接受状态和read读状态
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                //移除该事件
                iterator.remove();
                //接受
                if (selectionKey.isAcceptable()) {
                    acceptOperator(serverSocketChannel, selector);
                }
                //读
                if (selectionKey.isReadable()) {
                    readOperator(selectionKey, selector);
                }
            }
        }
    }

    /**
     * 读事件处理
     *
     * @param selectionKey
     * @param selector
     */
    private void readOperator(SelectionKey selectionKey, Selector selector) throws IOException {
        //读消息
        //从SelectionKey中获取SelectableChannel
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        //从channel中读取消息
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        String msg = "";
        if (socketChannel.read(byteBuffer) > 0) {
            byteBuffer.flip();
            msg += Charset.forName("UTF-8").decode(byteBuffer);
        }
        System.out.println(msg);
        //重新将channel读事件注册到selector
        socketChannel.register(selector, SelectionKey.OP_READ);
        //将读到的消息广播到其它客户端
        if (msg.length() > 0) {
            broadcast(selector, socketChannel, msg);
        }
    }

    /**
     * 广播消息
     *
     * @param selector
     * @param socketChannel
     * @param msg
     * @throws IOException
     */
    private void broadcast(Selector selector, SocketChannel socketChannel, String msg) throws IOException {
        Set<SelectionKey> selectionKeys = selector.keys();
        for (SelectionKey targetKey : selectionKeys) {
            Channel targetChannel = targetKey.channel();
            if (targetChannel instanceof SocketChannel && targetChannel != socketChannel) {
                ((SocketChannel) targetChannel).write(Charset.forName("UTF-8").encode(msg));
            }
        }
    }

    /**
     * 接受事件处理
     *
     * @param serverSocketChannel
     * @param selector
     */
    private void acceptOperator(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        //获取客户端SocketChannel
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        //将客户端channel读事件注册到selector
        socketChannel.register(selector, SelectionKey.OP_READ);
        //向客户端发送消息
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("欢迎加入聊天室".getBytes());
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
    }
}
