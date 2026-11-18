package serverPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import commonPackage.SyntaxeOperation;

/**
 * Activité 2-1 : échange de chaînes de caractères (CC).
 *
 * Le serveur reçoit, pour chaque opération, trois lignes de texte :
 * le premier opérande, l'opérateur et le deuxième opérande.
 * Il calcule le résultat puis renvoie une ligne de texte au client.
 * La communication s'arrête quand le client ferme la connexion.
 */
public class ServerOperationCC {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Je suis un serveur en attente de la connexion d'un client ");

            Socket socket = serverSocket.accept();
            System.out.println("un client est connecte");

            // Flux de traitement de caractères construits sur les flux d'octets de la socket
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            while (true) {
                // a) réception des trois éléments de l'opération (une ligne chacun)
                String operande1 = in.readLine();
                if (operande1 == null) { // readLine() renvoie null quand le client a fermé la connexion
                    System.out.println("Le client s'est deconnecte : fin de la communication.");
                    break;
                }
                String operateur = in.readLine();
                String operande2 = in.readLine();
                if (operateur == null || operande2 == null) {
                    System.out.println("Operation incomplete recue : fin de la communication.");
                    break;
                }
                System.out.println("Recu : operande1 = \"" + operande1 + "\", operateur = \"" + operateur
                        + "\", operande2 = \"" + operande2 + "\"");

                // b) calcul  -  c) envoi du résultat (une ligne de texte)
                String reponse = calculer(operande1, operateur, operande2);
                out.println(reponse);
                System.out.println("Resultat envoye : " + reponse);
            }

            socket.close();
            System.out.println("Sockets du serveur fermees.");
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }

    /**
     * Calcule l'opération à partir des chaînes reçues. Le serveur revérifie les
     * données : il ne fait pas confiance aveuglément au client.
     */
    static String calculer(String texte1, String operateur, String texte2) {
        double x;
        double y;
        try {
            x = Double.parseDouble(texte1);
            y = Double.parseDouble(texte2);
        } catch (NumberFormatException e) {
            return "Erreur : operande invalide";
        }

        double resultat;
        switch (operateur) {
            case "+": resultat = x + y; break;
            case "-": resultat = x - y; break;
            case "*": resultat = x * y; break;
            case "/":
                if (y == 0) {
                    return "Erreur : division par zero impossible";
                }
                resultat = x / y;
                break;
            default:
                return "Erreur : operateur inconnu \"" + operateur + "\"";
        }
        return SyntaxeOperation.formater(x) + " " + operateur + " " + SyntaxeOperation.formater(y)
                + " = " + SyntaxeOperation.formater(resultat);
    }
}
