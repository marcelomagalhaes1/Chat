import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Cliente de Chat com Interface Gráfica (GUI) usando Swing.
 * Substitui a versão de console (Cliente.java).
 */
public class ClienteGUI extends JFrame {

    // Componentes da Interface
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;

    // Componentes de Rede
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Informações do Usuário
    private String username;
    private String communicationMode;

    public ClienteGUI() {
        // 1. Coletar informações do usuário antes de iniciar a interface
        if (!collectUserInfo()) {
            System.exit(0); // Fecha se o usuário cancelar
        }

        // 2. Conectar ao servidor
        if (!connectToServer()) {
            System.exit(0); // Fecha se a conexão falhar
        }

        // 3. Inicializar e construir a interface gráfica
        initUI();

        // 4. Iniciar um "worker" em background para escutar mensagens do servidor
        startMessageListener();
    }

    /**
     * Usa caixas de diálogo para obter as informações necessárias do usuário.
     * @return false se o usuário cancelar qualquer etapa, true caso contrário.
     */
    private boolean collectUserInfo() {
        String serverAddress = JOptionPane.showInputDialog(
                this,
                "Digite o endereço IP do servidor:",
                "Conexão",
                JOptionPane.PLAIN_MESSAGE
        );
        if (serverAddress == null || serverAddress.trim().isEmpty()) return false;

        String serverPortStr = JOptionPane.showInputDialog(
                this,
                "Digite a porta do servidor:",
                "Conexão",
                JOptionPane.PLAIN_MESSAGE
        );
        if (serverPortStr == null || serverPortStr.trim().isEmpty()) return false;
        int serverPort = Integer.parseInt(serverPortStr);
        
        this.username = JOptionPane.showInputDialog(
                this,
                "Digite seu nome de usuário:",
                "Conexão",
                JOptionPane.PLAIN_MESSAGE
        );
        if (this.username == null || this.username.trim().isEmpty()) return false;

        // Menu para escolher o modo
        Object[] options = {"Assíncrono", "Síncrono"};
        int choice = JOptionPane.showOptionDialog(this,
                "Escolha o modo de comunicação:",
                "Modo de Comunicação",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            this.communicationMode = "ASYNC";
        } else if (choice == 1) {
            this.communicationMode = "SYNC";
        } else {
            return false; // Usuário fechou a janela de diálogo
        }
        
        // Define as informações de conexão para serem usadas depois
        // (Isso é um truque para passar os dados para o método de conexão)
        System.setProperty("server.address", serverAddress);
        System.setProperty("server.port", String.valueOf(serverPort));
        
        return true;
    }

    /**
     * Conecta ao servidor usando as informações coletadas.
     * @return true se a conexão for bem-sucedida, false caso contrário.
     */
    private boolean connectToServer() {
        try {
            String address = System.getProperty("server.address");
            int port = Integer.parseInt(System.getProperty("server.port"));
            
            socket = new Socket(address, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Envia o nome de usuário ao servidor
            out.println(username);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao conectar ao servidor: " + e.getMessage(),
                    "Erro de Conexão",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    /**
     * Constrói a interface gráfica do chat.
     */
    private void initUI() {
        setTitle("Chat - " + username + " (" + communicationMode + ")");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza na tela
        setLayout(new BorderLayout(5, 5)); // Layout principal com espaçamento

        // Área de chat (onde as mensagens aparecem)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Painel inferior para digitação e envio
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        sendButton = new JButton("Enviar");
        
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Adiciona o comportamento ao botão e ao campo de texto (pressionar Enter)
        ActionListener sendAction = e -> sendMessage();
        sendButton.addActionListener(sendAction);
        messageField.addActionListener(sendAction);

        setVisible(true); // Torna a janela visível
    }
    
    /**
     * Envia a mensagem do campo de texto para o servidor.
     */
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            if (message.startsWith("/")) {
                out.println(message); // Comandos são enviados diretamente
            } else {
                out.println(communicationMode + ":" + message);
            }
            
            // Adiciona a própria mensagem na área de chat
            chatArea.append("Você: " + message + "\n");
            messageField.setText("");

            if ("SYNC".equals(communicationMode)) {
                // No modo síncrono, desabilita a entrada até receber o ACK
                messageField.setEnabled(false);
                sendButton.setEnabled(false);
                chatArea.append("[SISTEMA] Aguardando confirmação (ACK)...\n");
            }
        }
    }

    /**
     * Inicia uma thread em background para ouvir continuamente as mensagens do servidor.
     * Usa SwingWorker para interagir de forma segura com a interface gráfica.
     */
    private void startMessageListener() {
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    publish(serverMessage); // Envia a mensagem para ser processada na UI thread
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                // Este método roda na thread da interface (EDT)
                for (String message : chunks) {
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
                // Executado quando o loop doInBackground termina (ex: servidor caiu)
                JOptionPane.showMessageDialog(ClienteGUI.this,
                        "Conexão com o servidor perdida.",
                        "Desconectado",
                        JOptionPane.WARNING_MESSAGE);
                messageField.setEnabled(false);
                sendButton.setEnabled(false);
            }
        };
        worker.execute(); // Inicia o worker
    }

    public static void main(String[] args) {
        // Garante que a interface gráfica seja criada na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(ClienteGUI::new);
    }
}