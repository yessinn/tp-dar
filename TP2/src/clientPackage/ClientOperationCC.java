package clientPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import commonPackage.SyntaxeOperation;

/**
 * Activité 2-1 : échange de chaînes de caractères (CC).
 *
 * Le client lit une opération complète au clavier (ex. "55 * 25"), valide sa
 * syntaxe, envoie ses trois éléments au serveur (une ligne chacun) puis affiche
 * le résultat reçu. Taper "fin" pour quitter.
 *
 * Utilisation : java -cp out clientPackage.ClientOperationCC [adresseServeur]
 */
public class ClientOperationCC {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";

        try (Scanner clavier = new Scanner(System.in)) {
            System.out.println("Je suis un client pas encore connecte...");
            Socket socket = new Socket(host, PORT);
            System.out.println("je suis un client connecte au serveur " + host + ":" + PORT);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            // autoFlush = true : chaque println() est envoyé immédiatement
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            while (true) {
                System.out.print("Operation (ex. 55 * 25, 'fin' pour quitter) : ");
                if (!clavier.hasNextLine()) {
                    break;
                }
                String saisie = clavier.nextLine().trim();
                if (saisie.equalsIgnoreCase("fin")) {
                    break;
                }

                // Validation de la syntaxe AVANT l'envoi : rien n'est envoyé si elle est incorrecte
                String[] elements = SyntaxeOperation.analyser(saisie);
                if (elements == null) {
                    System.out.println("Syntaxe invalide : \"" + saisie
                            + "\" (format attendu : nombre operateur nombre, operateurs + - * /)");
                    continue;
                }

                // a) envoi des trois éléments de l'opération
                out.println(elements[0]); // premier opérande
                out.println(elements[1]); // opérateur
                out.println(elements[2]); // deuxième opérande

                // d) lecture et affichage du résultat
                String reponse = in.readLine();
                if (reponse == null) {
                    System.out.println("Le serveur a ferme la connexion.");
                    break;
                }
                if (reponse.startsWith("Erreur")) {
                    System.out.println(">>> " + reponse.replaceFirst("Erreur", "Erreur recue du serveur"));
                } else {
                    System.out.println(">>> Resultat recu du serveur : " + reponse);
                }
            }

            socket.close();
            System.out.println("Fin de la communication. Socket du client fermee.");
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
