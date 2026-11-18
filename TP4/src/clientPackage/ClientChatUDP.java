package clientPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Étape 3 : client de chat bidirectionnel.
 *
 * Deux threads utilisent la même DatagramSocket :
 *   - le thread principal lit le clavier et envoie les messages ;
 *   - un thread de réception attend les datagrammes du serveur et les affiche
 *     dès leur arrivée (temps réel), sans attendre que l'utilisateur tape.
 *
 * Utilisation : java -cp out clientPackage.ClientChatUDP [adresseIPServeur]
 */
public class ClientChatUDP {

    private static final int PORT = 1234;
    private static final int TAILLE_MAX = 1024;

    public static void main(String[] args) {
        String ip = (args.length > 0) ? args[0] : "localhost";

        try (Scanner clavier = new Scanner(System.in);
             DatagramSocket socket = new DatagramSocket()) {
            InetSocketAddress serveur = new InetSocketAddress(InetAddress.getByName(ip), PORT);

            System.out.print("Nom d'utilisateur : ");
            String nom = clavier.nextLine().trim();

            // Thread de réception
            Thread reception = new Thread(() -> recevoir(socket), "reception");
            reception.setDaemon(true); // ne retient pas la JVM à la fermeture
            reception.start();

            // Annonce au serveur : il enregistre notre adresse
            envoyer(socket, "/join " + nom, serveur);
            System.out.println("Tapez vos messages ('quit' pour quitter).");

            // Thread principal : envoi
            while (clavier.hasNextLine()) {
                String texte = clavier.nextLine();
                if (texte.equalsIgnoreCase("quit")) {
                    break;
                }
                if (!texte.isBlank()) {
                    envoyer(socket, "[" + nom + "] : " + texte, serveur);
                }
            }
            envoyer(socket, "/quit " + nom, serveur);
            System.out.println("Vous avez quitte le chat.");
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }

    private static void recevoir(DatagramSocket socket) {
        byte[] buffer = new byte[TAILLE_MAX];
        try {
            while (true) {
                DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquet);
                System.out.println(">> " + new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8));
            }
        } catch (SocketException e) {
            // socket fermée par le thread principal : fin normale de la réception
        } catch (IOException e) {
            System.err.println("Erreur de reception : " + e.getMessage());
        }
    }

    private static void envoyer(DatagramSocket socket, String message, InetSocketAddress serveur)
            throws IOException {
        byte[] donnees = message.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(donnees, donnees.length, serveur));
    }
}
