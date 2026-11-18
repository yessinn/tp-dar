package serverPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Étape 2 : chat unidirectionnel.
 *
 * Le serveur est lié au port 1234 (InetSocketAddress) et affiche chaque
 * datagramme reçu avec l'adresse IP et le port de l'expéditeur.
 * Il ne répond jamais : la communication va du client vers le serveur uniquement.
 */
public class ServeurUDP {

    private static final int PORT = 1234;
    private static final int TAILLE_MAX = 1024;

    public static void main(String[] args) {
        // Pas d'accept() ni de connexion en UDP : on lie simplement la socket au port
        try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(PORT))) {
            System.out.println("Serveur UDP en ecoute sur le port " + PORT + "...");

            byte[] buffer = new byte[TAILLE_MAX];
            while (true) {
                DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquet); // bloquant jusqu'à l'arrivée d'un datagramme

                // Seuls les getLength() premiers octets du tampon contiennent le message
                String message = new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8);
                System.out.println("Message de " + paquet.getAddress().getHostAddress() + ":" + paquet.getPort()
                        + " -> " + message);
            }
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
