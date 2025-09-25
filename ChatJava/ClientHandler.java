import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Gerencia a comunicação com um único cliente no lado do servidor.
 * Implementa Runnable para ser executada em uma thread separada.
 */
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
            // Inicializa os fluxos de entrada e saída
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // A primeira mensagem do cliente é sempre o seu nome de usuário
            this.username = in.readLine();
            System.out.println("Usuário " + username + " associado ao cliente.");
            
            // Adiciona este cliente à lista do servidor
            server.addClient(this);
            server.broadcastMessage("[SISTEMA] " + username + " entrou no chat.", this);

            String clientMessage;
            // Loop principal para ler mensagens do cliente
            while ((clientMessage = in.readLine()) != null) {
                // Verifica se a mensagem é um comando de moderação
                if (clientMessage.startsWith("/")) {
                    handleCommand(clientMessage);
                } else {
                    // Se o usuário não estiver mutado, processa a mensagem
                    if (!server.isMuted(this.username)) {
                        processMessage(clientMessage);
                    } else {
                        // Informa ao usuário mutado que sua mensagem foi bloqueada
                        sendMessage("[SISTEMA] Você está mutado e não pode enviar mensagens.");
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Conexão com " + (username != null ? username : "cliente") + " foi perdida.");
        } catch (IOException e) {
            System.err.println("Erro no handler do cliente: " + e.getMessage());
        } finally {
            // Bloco de limpeza: remove o cliente e fecha os recursos
            server.removeClient(this);
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignorar
            }
        }
    }

    /**
     * Processa uma mensagem padrão do cliente, diferenciando entre síncrona e assíncrona.
     * @param message Mensagem recebida.
     */
    private void processMessage(String message) {
        if (message.startsWith("SYNC:")) {
            String syncMessage = message.substring(5);
            String formattedMessage = "[SYNC] " + username + ": " + syncMessage;
            System.out.println("Mensagem síncrona recebida de " + username);
            server.broadcastMessage(formattedMessage, this);
            // Envia a confirmação "ACK" de volta apenas para o remetente
            sendMessage("ACK");
        } else if (message.startsWith("ASYNC:")) {
            String asyncMessage = message.substring(6);
            String formattedMessage = username + ": " + asyncMessage;
            System.out.println("Mensagem assíncrona recebida de " + username);
            server.broadcastMessage(formattedMessage, this);
        }
    }

    /**
     * Processa comandos de moderação enviados pelo líder.
     * @param command Comando recebido.
     */
    private void handleCommand(String command) {
        // Verifica se quem enviou o comando é o líder
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

    /**
     * Envia uma mensagem para o cliente associado a este handler.
     * @param message A mensagem a ser enviada.
     */
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public String getUsername() {
        return username;
    }
}