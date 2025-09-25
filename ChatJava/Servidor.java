import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Classe principal do Servidor de Chat.
 * Gerencia a conexão de múltiplos clientes, a distribuição de mensagens
 * e o mecanismo de coordenação (líder e mute).
 */
public class Servidor {

    private static final int PORT = 12345; // Porta em que o servidor escuta
    
    // Usamos CopyOnWriteArrayList para segurança em ambientes concorrentes.
    // É eficiente para cenários onde leituras (broadcasts) são mais frequentes que escritas (conexões/desconexões).
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    
    // Conjunto sincronizado para armazenar os nomes dos usuários mutados.
    private final Set<String> mutedUsers = Collections.synchronizedSet(new HashSet<>());
    
    // Referência ao ClientHandler que é o líder.
    private ClientHandler leader = null;

    public static void main(String[] args) {
        new Servidor().startServer();
    }

    public void startServer() {
        System.out.println("Servidor de Chat iniciado na porta " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Aguarda uma nova conexão de cliente
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

                // Cria um handler para o novo cliente em uma nova thread
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    /**
     * Adiciona um cliente à lista de clientes conectados.
     * Este método é sincronizado para evitar condições de corrida ao adicionar clientes
     * e ao definir o líder.
     * @param clientHandler O handler do cliente a ser adicionado.
     */
    public synchronized void addClient(ClientHandler clientHandler) {
        clients.add(clientHandler);
        // O primeiro cliente a se conectar se torna o líder
        if (leader == null) {
            leader = clientHandler;
            System.out.println("Novo líder definido: " + leader.getUsername());
            broadcastMessage("[SISTEMA] " + leader.getUsername() + " é o novo líder.", null);
        }
    }

    /**
     * Remove um cliente da lista quando ele se desconecta.
     * @param clientHandler O handler do cliente a ser removido.
     */
    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        System.out.println("Cliente " + clientHandler.getUsername() + " desconectado.");
        broadcastMessage("[SISTEMA] " + clientHandler.getUsername() + " saiu do chat.", null);

        // Lógica para reeleição de líder poderia ser adicionada aqui, se necessário.
        if (clientHandler == leader) {
            leader = null;
            System.out.println("O líder se desconectou.");
            // Opcional: eleger um novo líder a partir dos clientes restantes.
            // if (!clients.isEmpty()) {
            //     leader = clients.get(0);
            //     System.out.println("Novo líder definido: " + leader.getUsername());
            //     broadcastMessage("[SISTEMA] " + leader.getUsername() + " é o novo líder.", null);
            // }
        }
    }

    /**
     * Retransmite uma mensagem para todos os clientes, exceto o remetente.
     * @param message A mensagem a ser transmitida.
     * @param sender O cliente que enviou a mensagem original.
     */
    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
    
    // Métodos de Coordenação (Líder)
    
    public ClientHandler getLeader() {
        return leader;
    }
    
    public synchronized void muteUser(String username) {
        mutedUsers.add(username);
        System.out.println("Usuário " + username + " foi mutado pelo líder.");
        broadcastMessage("[SISTEMA] Usuário " + username + " foi mutado pelo líder.", null);
    }
    
    public synchronized void unmuteUser(String username) {
        if (mutedUsers.remove(username)) {
            System.out.println("Usuário " + username + " foi desmutado pelo líder.");
            broadcastMessage("[SISTEMA] Usuário " + username + " foi desmutado pelo líder.", null);
        }
    }
    
    public boolean isMuted(String username) {
        return mutedUsers.contains(username);
    }
}