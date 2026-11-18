package serverPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Étape 3 : chat bidirectionnel avec diffusion à plusieurs clients.
 *
 * UDP n'ayant pas de connexion, le serveur « simule » la connexion en
 * mémorisant l'adresse (IP + port) de chaque client qui lui envoie un message.
 * Chaque message reçu est retransmis à tous les autres clients enregistrés,
 * sauf à l'expéditeur.
 *
 * Messages de contrôle envoyés par le client :
 *   /join Nom  -> arrivée d'un client
 *   /quit Nom  -> départ d'un client (son adresse est retirée de la liste)
 */
public class ServeurChatUDP {

    private static final int PORT = 1234;
    private static final int TAILLE_MAX = 1024;

    // Un seul thread utilise cette liste : aucune synchronisation n'est nécessaire
    private static final Set<SocketAddress> clients = new LinkedHashSet<>();

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(PORT))) {
            System.out.println("Serveur de chat UDP en ecoute sur le port " + PORT + "...");

            byte[] buffer = new byte[TAILLE_MAX];
            while (true) {
                DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquet);

                SocketAddress expediteur = paquet.getSocketAddress();
                String message = new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8);

                // Enregistrement de l'expéditeur s'il est nouveau
                if (clients.add(expediteur)) {
                    System.out.println("Nouveau client enregistre : " + expediteur
                            + " (" + clients.size() + " client(s))");
                }

                if (message.startsWith("/join ")) {
                    String nom = message.substring(6);
                    envoyer(socket, "*** Bienvenue " + nom + " ! " + clients.size()
                            + " participant(s) dans le chat ***", expediteur);
                    diffuser(socket, "*** " + nom + " a rejoint le chat ***", expediteur);
                } else if (message.startsWith("/quit ")) {
                    String nom = message.substring(6);
                    clients.remove(expediteur);
                    System.out.println("Client retire : " + expediteur + " (" + clients.size() + " client(s))");
                    diffuser(socket, "*** " + nom + " a quitte le chat ***", expediteur);
                } else {
                    System.out.println("Message de " + expediteur + " -> " + message);
                    diffuser(socket, message, expediteur);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }

    /** Envoie le message à tous les clients enregistrés, sauf l'expéditeur. */
    private static void diffuser(DatagramSocket socket, String message, SocketAddress expediteur)
            throws IOException {
        int nb = 0;
        for (SocketAddress client : clients) {
            if (!client.equals(expediteur)) {
                envoyer(socket, message, client);
                nb++;
            }
        }
        System.out.println("   diffuse a " + nb + " client(s)");
    }

    private static void envoyer(DatagramSocket socket, String message, SocketAddress destination)
            throws IOException {
        byte[] donnees = message.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(donnees, donnees.length, destination));
    }
}
