package serverPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import commonPackage.Calculateur;
import commonPackage.Resultat;
import commonPackage.SyntaxeOperation;

/**
 * Activité 3-1 : thread dédié à un client.
 *
 * Il envoie au client son numéro d'ordre, puis traite ses opérations (une
 * opération complète par ligne, ex. "55 * 25") jusqu'à "fin" ou déconnexion.
 * Une ligne par opération rend le service utilisable aussi depuis telnet.
 */
public class ClientProcessCC extends Thread {

    private final Socket socket;
    private final int numero;

    public ClientProcessCC(Socket socket, int numero) {
        super("client-" + numero);
        this.socket = socket;
        this.numero = numero;
    }

    @Override
    public void run() {
        String prefixe = "[client " + numero + " " + socket.getRemoteSocketAddress() + "] ";
        try (Socket s = socket) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);

            // c) envoi au client de son numéro d'ordre
            out.println("Bienvenue ! Vous etes le client numero " + numero);
            out.println("Tapez une operation (ex. 55 * 25) ou 'fin' pour quitter.");

            String ligne;
            while ((ligne = in.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) {
                    continue;
                }
                if (ligne.equalsIgnoreCase("fin")) {
                    out.println("Au revoir client numero " + numero);
                    break;
                }

                // Le serveur valide lui-même la ligne : un client telnet n'a aucune validation
                String[] elements = SyntaxeOperation.analyser(ligne);
                String reponse;
                if (elements == null) {
                    reponse = "Erreur : syntaxe invalide \"" + ligne + "\"";
                } else {
                    double x = Double.parseDouble(elements[0]);
                    char op = elements[1].charAt(0);
                    double y = Double.parseDouble(elements[2]);
                    Resultat r = Calculateur.calculer(x, op, y);
                    reponse = r.isSucces()
                            ? SyntaxeOperation.formater(x) + " " + op + " " + SyntaxeOperation.formater(y)
                                    + " = " + SyntaxeOperation.formater(r.getValeur())
                            : "Erreur : " + r.getMessage();
                }
                out.println(reponse);
                System.out.println(prefixe + ligne + "  ->  " + reponse);
            }
        } catch (IOException e) {
            System.err.println(prefixe + "erreur : " + e.getMessage());
        }
        System.out.println(prefixe + "deconnecte.");
    }
}
