import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class ClienteGUI extends JFrame {

    // Componentes da Interface
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel typingStatusLabel; // NOVO: Label para mostrar quem está digitando

    // Componentes de Rede
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Informações do Usuário
    private String username;
    private String communicationMode;

    // NOVO: Componentes para a funcionalidade "digitando"
    private Timer typingTimer;
    private boolean isTyping = false;
    private final Set<String> usersTyping = new HashSet<>();

    public ClienteGUI() {
        if (!collectUserInfo()) {
            System.exit(0);
        }
        if (!connectToServer()) {
            System.exit(0);
        }
        initUI();
        initTypingComponents(); // NOVO: Inicia os componentes de "digitando"
        startMessageListener();
    }

    private boolean collectUserInfo() {
        // ... (este método continua exatamente o mesmo)
        String serverAddress = JOptionPane.showInputDialog(this, "Digite o endereço IP do servidor:", "Conexão",
                JOptionPane.PLAIN_MESSAGE);
        if (serverAddress == null || serverAddress.trim().isEmpty())
            return false;
        String serverPortStr = JOptionPane.showInputDialog(this, "Digite a porta do servidor:", "Conexão",
                JOptionPane.PLAIN_MESSAGE);
        if (serverPortStr == null || serverPortStr.trim().isEmpty())
            return false;
        int serverPort = Integer.parseInt(serverPortStr);
        this.username = JOptionPane.showInputDialog(this, "Digite seu nome de usuário:", "Conexão",
                JOptionPane.PLAIN_MESSAGE);
        if (this.username == null || this.username.trim().isEmpty())
            return false;
        Object[] options = { "Assíncrono", "Síncrono" };
        int choice = JOptionPane.showOptionDialog(this, "Escolha o modo de comunicação:", "Modo de Comunicação",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 0)
            this.communicationMode = "ASYNC";
        else if (choice == 1)
            this.communicationMode = "SYNC";
        else
            return false;
        System.setProperty("server.address", serverAddress);
        System.setProperty("server.port", String.valueOf(serverPort));
        return true;
    }

    private boolean connectToServer() {
        try {
            String address = System.getProperty("server.address");
            int port = Integer.parseInt(System.getProperty("server.port"));
            socket = new Socket(address, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println(username);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao servidor: " + e.getMessage(), "Erro de Conexão",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void initUI() {
        setTitle("Chat - " + username + " (" + communicationMode + ")");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // Painel central para o chat e o status de digitação
        JPanel centerPanel = new JPanel(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // NOVO: Adiciona a label de status abaixo da área de chat
        typingStatusLabel = new JLabel(" ");
        typingStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        typingStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        centerPanel.add(typingStatusLabel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Painel inferior
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        sendButton = new JButton("Enviar");

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        ActionListener sendAction = _ -> sendMessage();
        sendButton.addActionListener(sendAction);
        messageField.addActionListener(sendAction);

        setVisible(true);
    }

    // NOVO: Método para configurar o listener e o timer de "digitando"
    private void initTypingComponents() {
        // Timer que espera 1.5 segundos de inatividade para enviar TYPING_STOP
        typingTimer = new Timer(1500, _ -> {
            if (isTyping) {
                out.println("TYPING_STOP");
                isTyping = false;
            }
        });
        typingTimer.setRepeats(false); // O timer só dispara uma vez

        // Listener que observa qualquer mudança no campo de texto
        messageField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleTyping();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleTyping();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleTyping();
            }
        });
    }

    // NOVO: Lógica que é chamada toda vez que o usuário digita algo
    private void handleTyping() {
        if (!isTyping) {
            out.println("TYPING_START");
            isTyping = true;
        }
        typingTimer.restart(); // Reinicia o timer a cada tecla pressionada
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // NOVO: Para o timer e envia a notificação de parada
            if (isTyping) {
                typingTimer.stop();
                out.println("TYPING_STOP");
                isTyping = false;
            }

            if (message.startsWith("/")) {
                out.println(message);
            } else {
                out.println(communicationMode + ":" + message);
            }

            chatArea.append("Você: " + message + "\n");
            messageField.setText("");

            if ("SYNC".equals(communicationMode)) {
                messageField.setEnabled(false);
                sendButton.setEnabled(false);
                chatArea.append("[SISTEMA] Aguardando confirmação (ACK)...\n");
            }
        }
    }

    private void startMessageListener() {
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    publish(serverMessage);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    // NOVO: Lógica para processar as notificações de "digitando"
                    if (message.startsWith("TYPING_INFO:")) {
                        processTypingInfo(message);
                        continue;
                    }

                    if ("ACK".equals(message) && "SYNC".equals(communicationMode)) {
                        chatArea.append("[SISTEMA] Confirmação recebida. Pode enviar a próxima mensagem.\n");
                        messageField.setEnabled(true);
                        sendButton.setEnabled(true);
                        messageField.requestFocus();
                    } else {
                        chatArea.append(message + "\n");
                    }
                }
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(ClienteGUI.this, "Conexão com o servidor perdida.", "Desconectado",
                        JOptionPane.WARNING_MESSAGE);
                messageField.setEnabled(false);
                sendButton.setEnabled(false);
            }
        };
        worker.execute();
    }

    // NOVO: Processa a mensagem TYPING_INFO
    private void processTypingInfo(String message) {
        String[] parts = message.split(":");
        String user = parts[1];
        String status = parts[2];

        if ("START".equals(status)) {
            usersTyping.add(user);
        } else if ("STOP".equals(status)) {
            usersTyping.remove(user);
        }
        updateTypingStatusLabel();
    }

    // NOVO: Atualiza a label com base na lista de usuários que estão digitando
    private void updateTypingStatusLabel() {
        if (usersTyping.isEmpty()) {
            typingStatusLabel.setText(" ");
        } else {
            String users = String.join(", ", usersTyping);
            String verb = usersTyping.size() > 1 ? "estão" : "está";
            typingStatusLabel.setText(users + " " + verb + " digitando...");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClienteGUI::new);
    }
}