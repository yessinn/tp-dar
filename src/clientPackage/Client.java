package clientPackage;

import java.io.IOException;
import java.net.Socket;

/**
 * Activité 1-1 : Première connexion Client/Serveur (sockets TCP).
 * Le client se connecte au serveur (port 1234) puis ferme sa socket.
 *
 * Étape 4.f : l'hôte peut être passé en argument pour remplacer "localhost"
 * par une adresse IP, par exemple : java -cp out clientPackage.Client 127.0.0.1
 */
public class Client {

    public static void main(String[] args) {
        // La première étape : définir l'adresse du serveur et le port
        String host = (args.length > 0) ? args[0] : "localhost";
        int port = 1234;
        System.out.println("Je suis un client pas encore connecte...");

        try {
            // Création de la socket = demande de connexion au serveur (host:port)
            Socket socket = new Socket(host, port);
            System.out.println("je suis un client connecte");

            // La dernière étape : Fermer socket
            socket.close();
        } catch (IOException e) {
            System.err.println("Connexion impossible a " + host + ":" + port + " -> " + e.getMessage());
        }
    }
}
