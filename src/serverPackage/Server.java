package serverPackage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Activité 1-1 : Première connexion Client/Serveur (sockets TCP).
 * Le serveur attend la connexion d'un seul client, l'accepte puis ferme les sockets.
 */
public class Server {

    public static void main(String[] args) {
        try {
            // La première étape : créer la socket d'écoute sur le port 1234
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Je suis un serveur en attente de la connexion d'un client ");

            // accept() est bloquante : le serveur reste ici tant qu'aucun client ne se connecte
            Socket socket = serverSocket.accept();

            // La deuxième étape :
            System.out.println("un client est connecte");

            // La dernière étape : Fermer socket
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
