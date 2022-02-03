package cn.liuwei;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * 聊天室客户端
 *
 * @author 6v
 */
public class ChatClient {

    public void startClient(String clientName) throws IOException {
        //创建SocketChannel连接服务器
        SocketChannel socketChannel =
                SocketChannel.open(new InetSocketAddress("127.0.0.1", 8000));

        //用非阻塞nio方式接收消息
        Selector selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        new Thread(new ReadThread(selector)).start();

        //键盘输入发送消息
        Scanner scanner = new Scanner(System.in);
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        while (scanner.hasNext()) {
            byteBuffer.clear();
            String next = scanner.next();
            byteBuffer.put((clientName + ": " + next).getBytes());
            byteBuffer.flip();
            socketChannel.write(byteBuffer);
        }
    }

    private class ReadThread extends Thread {

        private Selector selector;

        public ReadThread(Selector selector) {
            this.selector = selector;
        }

        @Override
        public void run() {
            while (true) {
                int count = 0;
                try {
                    count = selector.select();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (count == 0) {
                    continue;
                }
                //遍历key 处理read读状态
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    //删除该事件
                    iterator.remove();
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
         * @param selector
         * @param selectionKey
         */
        private void readOperator(SelectionKey selectionKey, Selector selector) {
            //读消息
            //从SelectionKey中获取SelectableChannel
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            //从channel中读取消息
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            String msg = "";
            try {
                if (socketChannel.read(byteBuffer) > 0) {
                    byteBuffer.flip();
                    msg += Charset.forName("UTF-8").decode(byteBuffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(msg);
            //重新将channel读事件注册到selector
            try {
                socketChannel.register(selector, SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        }
    }
}
