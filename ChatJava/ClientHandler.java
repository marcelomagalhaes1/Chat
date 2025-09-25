import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Servidor server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket, Servidor server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            this.username = in.readLine();
            System.out.println("Usuário " + username + " associado ao cliente.");

            server.addClient(this);
            server.broadcastMessage("[SISTEMA] " + username + " entrou no chat.", this);

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                // DEBUG: Mostra exatamente o que o servidor recebeu
                System.out.println("[DEBUG SERVER] Recebido de " + username + ": '" + clientMessage + "'");

                // Lógica para o status "digitando"
                if (clientMessage.equals("TYPING_START")) {
                    String broadcastMsg = "TYPING_INFO:" + username + ":START";
                    System.out.println("[DEBUG SERVER] Retransmitindo: '" + broadcastMsg + "'");
                    server.broadcastMessage(broadcastMsg, this);
                    continue;
                } else if (clientMessage.equals("TYPING_STOP")) {
                    String broadcastMsg = "TYPING_INFO:" + username + ":STOP";
                    System.out.println("[DEBUG SERVER] Retransmitindo: '" + broadcastMsg + "'");
                    server.broadcastMessage(broadcastMsg, this);
                    continue;
                }

                // Lógica de comandos e mensagens normais
                if (clientMessage.startsWith("/")) {
                    handleCommand(clientMessage);
                } else {
                    if (!server.isMuted(this.username)) {
                        processMessage(clientMessage);
                    } else {
                        sendMessage("[SISTEMA] Você está mutado e não pode enviar mensagens.");
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Conexão com " + (username != null ? username : "cliente") + " foi perdida.");
        } catch (IOException e) {
            System.err.println("Erro no handler do cliente: " + e.getMessage());
        } finally {
            server.removeClient(this);
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignorar
            }
        }
    }

    private void processMessage(String message) {
        if (message.startsWith("SYNC:")) {
            String syncMessage = message.substring(5);
            String formattedMessage = "[SYNC] " + username + ": " + syncMessage;
            System.out.println("Mensagem síncrona recebida de " + username);
            server.broadcastMessage(formattedMessage, this);
            sendMessage("ACK");
        } else if (message.startsWith("ASYNC:")) {
            String asyncMessage = message.substring(6);
            String formattedMessage = username + ": " + asyncMessage;
            System.out.println("Mensagem assíncrona recebida de " + username);
            server.broadcastMessage(formattedMessage, this);
        }
    }

    private void handleCommand(String command) {
        if (this != server.getLeader()) {
            sendMessage("[SISTEMA] Apenas o líder pode executar comandos.");
            return;
        }
        String[] parts = command.split(" ", 2);
        String cmd = parts[0];
        if (parts.length < 2) {
            sendMessage("[SISTEMA] Comando inválido. Use /mute <username> ou /unmute <username>.");
            return;
        }
        String targetUser = parts[1];
        switch (cmd) {
            case "/mute":
                server.muteUser(targetUser);
                break;
            case "/unmute":
                server.unmuteUser(targetUser);
                break;
            default:
                sendMessage("[SISTEMA] Comando desconhecido.");
                break;
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }
}