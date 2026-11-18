package serverPackage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Activité 3-2 : service de calculatrice multi-thread, communication par objets.
 *
 * Chaque client est servi par un thread ClientProcess. Tous les threads
 * partagent le même CompteurOperations (accès synchronisé).
 */
public class ServeurCalculatrice {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        CompteurOperations compteur = new CompteurOperations(); // ressource partagée
        int numeroClient = 0;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur calculatrice multi-thread demarre sur le port " + PORT
                    + ", en attente de clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                numeroClient++;
                System.out.println("Connexion du client numero " + numeroClient
                        + " depuis " + socket.getRemoteSocketAddress());

                new ClientProcess(socket, numeroClient, compteur).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
