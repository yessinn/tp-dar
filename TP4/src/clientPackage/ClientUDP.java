package clientPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Étape 2 : chat unidirectionnel.
 *
 * Le client demande un nom d'utilisateur, puis envoie chaque ligne saisie au
 * serveur dans un datagramme UDP, préfixée par le nom : "[Nom] : message".
 * Taper "quit" pour quitter.
 *
 * Utilisation : java -cp out clientPackage.ClientUDP [adresseIPServeur]
 */
public class ClientUDP {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        String ip = (args.length > 0) ? args[0] : "localhost";

        try (Scanner clavier = new Scanner(System.in);
             DatagramSocket socket = new DatagramSocket()) { // port local choisi par le système
            InetSocketAddress serveur = new InetSocketAddress(InetAddress.getByName(ip), PORT);

            System.out.print("Nom d'utilisateur : ");
            String nom = clavier.nextLine().trim();
            System.out.println("Bonjour " + nom + ", tapez vos messages ('quit' pour quitter).");

            while (clavier.hasNextLine()) {
                String texte = clavier.nextLine();
                if (texte.equalsIgnoreCase("quit")) {
                    break;
                }
                byte[] donnees = ("[" + nom + "] : " + texte).getBytes(StandardCharsets.UTF_8);
                // Chaque datagramme porte lui-même l'adresse de destination
                socket.send(new DatagramPacket(donnees, donnees.length, serveur));
                System.out.println("(envoye) " + texte);
            }
            System.out.println("Fin du client.");
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
