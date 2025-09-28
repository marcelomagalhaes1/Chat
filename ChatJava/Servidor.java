import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Servidor {

    private static final int PORT = 12345; // Porta em que o servidor escuta

    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private final Set<String> mutedUsers = Collections.synchronizedSet(new HashSet<>());

    private ClientHandler leader = null;

    public static void main(String[] args) {
        new Servidor().startServer();
    }

    public void startServer() {
        System.out.println("Servidor de Chat iniciado na porta " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    public synchronized void addClient(ClientHandler clientHandler) {
        clients.add(clientHandler);
        if (leader == null) {
            leader = clientHandler;
            System.out.println("Novo líder definido: " + leader.getUsername());
            broadcastMessage("[SISTEMA] " + leader.getUsername() + " é o novo líder.", null);
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        System.out.println("Cliente " + clientHandler.getUsername() + " desconectado.");
        broadcastMessage("[SISTEMA] " + clientHandler.getUsername() + " saiu do chat.", null);

        if (clientHandler == leader) {
            leader = null;
            System.out.println("O líder se desconectou.");
        }
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
    
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
