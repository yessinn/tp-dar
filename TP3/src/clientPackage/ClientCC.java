package clientPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import commonPackage.SyntaxeOperation;

/**
 * Activité 3-1 : client du serveur multi-thread (chaînes de caractères).
 *
 * L'adresse IP du serveur est donnée en argument (localhost par défaut) et
 * résolue avec InetAddress / InetSocketAddress, ce qui permet les tests en
 * réseau local.
 *
 * Utilisation : java -cp out clientPackage.ClientCC [adresseIPServeur]
 */
public class ClientCC {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        String ip = (args.length > 0) ? args[0] : "localhost";

        try (Scanner clavier = new Scanner(System.in)) {
            InetSocketAddress adresseServeur = new InetSocketAddress(InetAddress.getByName(ip), PORT);
            Socket socket = new Socket();
            socket.connect(adresseServeur, 5000);
            System.out.println("Connecte au serveur " + adresseServeur.getAddress().getHostAddress() + ":" + PORT);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Message de bienvenue : numéro d'ordre attribué par le serveur
            System.out.println("<<< " + in.readLine());
            System.out.println("<<< " + in.readLine());

            while (true) {
                System.out.print("Operation : ");
                if (!clavier.hasNextLine()) {
                    break;
                }
                String saisie = clavier.nextLine().trim();
                if (saisie.equalsIgnoreCase("fin")) {
                    out.println("fin");
                    System.out.println("<<< " + in.readLine());
                    break;
                }
                if (SyntaxeOperation.analyser(saisie) == null) {
                    System.out.println("Syntaxe invalide, format attendu : nombre operateur nombre");
                    continue;
                }
                out.println(saisie);
                String reponse = in.readLine();
                if (reponse == null) {
                    System.out.println("Le serveur a ferme la connexion.");
                    break;
                }
                System.out.println("<<< " + reponse);
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
