import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    private final String server;
    private final int port;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(16384);
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(16384);
    private SocketChannel socketChannel;
    private final Charset charset = StandardCharsets.UTF_8;
    private final CharsetDecoder charsetDecoder = charset.newDecoder();


    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
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
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
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
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

        this.server = server;
        this.port = port;

    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
        sendBuffer.clear();
        sendBuffer.put(charset.encode(message));
        sendBuffer.flip();
        while (sendBuffer.hasRemaining()) {
            socketChannel.write(sendBuffer);
        }



    }

    
    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
       socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
       socketChannel.connect(new InetSocketAddress(server, port));
        while(!socketChannel.finishConnect()){
        }

       while (true) {
           receiveBuffer.clear();
           socketChannel.read(receiveBuffer);
           receiveBuffer.flip();
           if (receiveBuffer.limit() != 0) {
               String message = charsetDecoder.decode(receiveBuffer).toString();
               String messageType = message.split(" ")[0];
               String friendlyMessage = " ";
               if (messageType.equals("OK\n")) {
                    friendlyMessage = "Sucesso!\n";
               } else if (messageType.equals("ERROR\n")) {
                    friendlyMessage = "Erro!\n";
               } else if (messageType.equals("MESSAGE")) {
                   int messageStartIndex = messageType.length() + 2 + message.split(" ")[1].length();
                    String newMessage = message.substring(messageStartIndex);
                    friendlyMessage = message.split(" ")[1] + ": " + newMessage ;
               } else if (messageType.equals("NEWNICK")) {
                    friendlyMessage = message.split(" ")[1] + " mudou de nome para " + message.split(" ")[2];
               } else if (messageType.equals("JOINED")) {
                    friendlyMessage = message.split(" ")[1].replace("\n", "") + " juntou se a sala\n";
               } else if (messageType.equals("LEFT")) {
                    friendlyMessage = message.split(" ")[1].replace("\n", "") + " deixou a sala\n";
               } else if (messageType.equals("PRIVATE")) {
                   int messageStartIndex = messageType.length() + 2 + message.split(" ")[1].length();
                   friendlyMessage = "Mensagem privada de " + message.split(" ")[1] + ": " + message.substring(messageStartIndex);
               } else {
                    friendlyMessage = "Xau!\n";
               }

               printMessage(friendlyMessage);
           }
       }


    }
    

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}
