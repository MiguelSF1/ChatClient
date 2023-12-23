import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatClient {
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    private final String server;
    private final int port;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(16384);
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(16384);
    private SocketChannel socketChannel;
    private final Charset charset = StandardCharsets.UTF_8;
    private final CharsetDecoder charsetDecoder = charset.newDecoder();

    public void printMessage(final String message) {
        chatArea.append(message);
    }

    public ChatClient(String server, int port) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException e) {
                } finally {
                     chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });

        this.server = server;
        this.port = port;
    }

    public void newMessage(String message) throws IOException {
        if (!message.isEmpty()) {
            String[] args = message.split(" ");
            if (message.charAt(0) == '/') {
                if (!args[0].equals("/nick") && !args[0].equals("/join") && !args[0].equals("/leave") && !args[0].equals("/bye") && !args[0].equals("/priv")) {
                    message = "/" + message;
                }
            }
        }
        sendBuffer.clear();
        sendBuffer.put(charset.encode(message + "\n"));
        sendBuffer.flip();
        while (sendBuffer.hasRemaining()) {
            socketChannel.write(sendBuffer);
        }
    }

    public void run() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(server, port));
        Receiver receiver = new Receiver();
        receiver.run();
    }

    class Receiver implements Runnable {
        Receiver() {
        }

        @Override
        public void run() {
            while (true) {
                receiveBuffer.clear();
                try {
                    socketChannel.read(receiveBuffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                receiveBuffer.flip();
                if (receiveBuffer.limit() != 0) {
                    String message = null;
                    try {
                        message = charsetDecoder.decode(receiveBuffer).toString();
                    } catch (CharacterCodingException e) {
                        throw new RuntimeException(e);
                    }
                    String[] args = message.split(" ");
                    String messageType = args[0];
                    String friendlyMessage;
                    if (messageType.equals("OK\n")) {
                        friendlyMessage = "Sucesso!\n";
                    } else if (messageType.equals("ERROR\n")) {
                        friendlyMessage = "Erro!\n";
                    } else if (messageType.equals("MESSAGE")) {
                        int messageStartIndex = messageType.length() + args[1].length() + 2;
                        friendlyMessage = args[1] + ": " + message.substring(messageStartIndex);
                    } else if (messageType.equals("NEWNICK")) {
                        friendlyMessage = args[1] + " mudou de nome para " + args[2];
                    } else if (messageType.equals("JOINED")) {
                        friendlyMessage = args[1].replace("\n", "") + " juntou se a sala\n";
                    } else if (messageType.equals("LEFT")) {
                        friendlyMessage = args[1].replace("\n", "") + " deixou a sala\n";
                    } else if (messageType.equals("PRIVATE")) {
                        int messageStartIndex = messageType.length() + args[1].length() + 2;
                        friendlyMessage = "Mensagem privada de " + args[1] + ": " + message.substring(messageStartIndex);
                    } else {
                        friendlyMessage = "Xau!\n";
                    }

                    printMessage(friendlyMessage);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
