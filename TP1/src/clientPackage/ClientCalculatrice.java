package clientPackage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Activité 1-3 (étape 2) : client de la calculatrice distante.
 * Le client affiche un menu, envoie le choix et les opérandes au serveur,
 * puis affiche le résultat calculé par le serveur.
 *
 * Utilisation : java -cp out clientPackage.ClientCalculatrice [adresseIPServeur]
 */
public class ClientCalculatrice {

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

            System.out.println("Je suis un client pas encore connecte...");
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(InetAddress.getByName(ipServeur), PORT), 5000);
            System.out.println("je suis un client connecte au serveur " + ipServeur + ":" + PORT);

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (true) {
                afficherMenu();
                int choix = lireEntier(clavier, "Votre choix : ");

                if (choix == 0) {
                    dos.writeInt(0);
                    dos.flush();
                    System.out.println("Fin de la communication demandee.");
                    break;
                }
                if (choix < 1 || choix > 4) {
                    System.out.println("Choix invalide, veuillez reessayer.");
                    continue;
                }

                double x = lireReel(clavier, "Donnez x : ");
                double y = lireReel(clavier, "Donnez y : ");

                // Transmission du choix et des opérandes au serveur
                dos.writeInt(choix);
                dos.writeDouble(x);
                dos.writeDouble(y);
                dos.flush();

                // Réception de la réponse du serveur
                boolean succes = dis.readBoolean();
                String symbole = new String[] {"", "+", "-", "*", "/"}[choix];
                if (succes) {
                    double resultat = dis.readDouble();
                    System.out.println(">>> Resultat recu du serveur : " + formater(x) + " " + symbole + " "
                            + formater(y) + " = " + formater(resultat));
                } else {
                    System.out.println(">>> " + dis.readUTF());
                }
            }

            socket.close();
            System.out.println("Socket du client fermee.");
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }

    private static void afficherMenu() {
        System.out.println();
        System.out.println("===== Calculatrice distante =====");
        System.out.println("1. Addition");
        System.out.println("2. Soustraction");
        System.out.println("3. Multiplication");
        System.out.println("4. Division");
        System.out.println("0. Quitter");
    }

    private static int lireEntier(Scanner clavier, String message) {
        while (true) {
            System.out.print(message);
            String ligne = clavier.nextLine().trim();
            try {
                return Integer.parseInt(ligne);
            } catch (NumberFormatException e) {
                System.out.println("Veuillez saisir un entier.");
            }
        }
    }

    /** Accepte la virgule ou le point comme séparateur décimal (ex. 2,5 ou 2.5). */
    private static double lireReel(Scanner clavier, String message) {
        while (true) {
            System.out.print(message);
            String ligne = clavier.nextLine().trim().replace(',', '.');
            try {
                return Double.parseDouble(ligne);
            } catch (NumberFormatException e) {
                System.out.println("Veuillez saisir un nombre.");
            }
        }
    }

    private static String formater(double v) {
        return (v == Math.rint(v) && !Double.isInfinite(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
