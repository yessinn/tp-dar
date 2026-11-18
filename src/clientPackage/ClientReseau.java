package clientPackage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Activité 1-3 (étape 1) : le client se connecte au serveur situé sur une AUTRE machine.
 * "localhost" est remplacé par l'adresse IP de la machine serveur.
 *
 * Utilisation : java -cp out clientPackage.ClientReseau [adresseIPServeur]
 * Sans argument, l'adresse IP est demandée au clavier.
 */
public class ClientReseau {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        try (Scanner clavier = new Scanner(System.in)) {
            String ipServeur;
            if (args.length > 0) {
                ipServeur = args[0];
            } else {
                System.out.print("Adresse IP du serveur : ");
                ipServeur = clavier.nextLine().trim();
            }

            InetSocketAddress adresseServeur = new InetSocketAddress(InetAddress.getByName(ipServeur), PORT);
            System.out.println("Je suis un client pas encore connecte...");

            Socket socket = new Socket();
            socket.connect(adresseServeur, 5000); // délai maximal de connexion : 5 secondes
            System.out.println("je suis un client connecte au serveur "
                    + adresseServeur.getAddress().getHostAddress() + ":" + PORT
                    + " depuis " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (true) {
                System.out.print("Donnez un entier x (0 pour quitter) : ");
                int x = clavier.nextInt();

                dos.writeInt(x);
                dos.flush();

                if (x == 0) {
                    System.out.println("Fin de la communication demandee.");
                    break;
                }

                int resultat = dis.readInt();
                System.out.println("Resultat recu du serveur : " + x + " * 5 = " + resultat);
            }

            socket.close();
            System.out.println("Socket du client fermee.");
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
