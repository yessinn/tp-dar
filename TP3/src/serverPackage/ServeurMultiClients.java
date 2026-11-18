package serverPackage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Activité 3-1 : serveur multi-thread (échange de chaînes de caractères).
 *
 * Le thread principal ne fait qu'accepter les connexions, en boucle.
 * Chaque client accepté est confié à un thread dédié (ClientProcessCC), ce qui
 * permet au serveur de retourner immédiatement à accept() et de servir
 * plusieurs clients simultanément.
 *
 * Compatible avec le client Java ClientCC et avec telnet : telnet localhost 1234
 */
public class ServeurMultiClients {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        int numeroClient = 0; // modifié uniquement par le thread principal

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur multi-thread demarre sur le port " + PORT
                    + ", en attente de clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                numeroClient++;
                System.out.println("Connexion du client numero " + numeroClient
                        + " depuis " + socket.getRemoteSocketAddress());

                new ClientProcessCC(socket, numeroClient).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
