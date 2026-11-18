package serverPackage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Activité 1-3 (étape 2) : service de calculatrice amélioré (machines distinctes).
 *
 * Protocole (DataInputStream / DataOutputStream) :
 *   Client -> Serveur : int choix (1=+, 2=-, 3=*, 4=/, 0=quitter), puis double x, double y
 *   Serveur -> Client : boolean succes, puis double resultat (si succes) ou String message d'erreur
 *
 * Utilisation : java -cp out serverPackage.ServerCalculatrice [adresseIP]
 */
public class ServerCalculatrice {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        try {
            InetAddress ip = (args.length > 0)
                    ? InetAddress.getByName(args[0])
                    : ServerReseau.trouverAdresseLocale();

            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ip, PORT));
            System.out.println("Serveur calculatrice a l'ecoute sur " + ip.getHostAddress() + ":" + PORT);
            System.out.println("Je suis un serveur en attente de la connexion d'un client ");

            Socket socket = serverSocket.accept();
            System.out.println("un client est connecte depuis " + socket.getInetAddress().getHostAddress());

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (true) {
                int choix = dis.readInt();
                if (choix == 0) {
                    System.out.println("Le client a choisi de quitter : fin de la communication.");
                    break;
                }
                double x = dis.readDouble();
                double y = dis.readDouble();

                double resultat;
                String symbole;
                String erreur = null;
                switch (choix) {
                    case 1: symbole = "+"; resultat = x + y; break;
                    case 2: symbole = "-"; resultat = x - y; break;
                    case 3: symbole = "*"; resultat = x * y; break;
                    case 4:
                        symbole = "/";
                        if (y == 0) {
                            resultat = 0;
                            erreur = "Erreur : division par zero impossible";
                        } else {
                            resultat = x / y;
                        }
                        break;
                    default:
                        symbole = "?";
                        resultat = 0;
                        erreur = "Erreur : operation inconnue (" + choix + ")";
                }

                if (erreur == null) {
                    dos.writeBoolean(true);
                    dos.writeDouble(resultat);
                    System.out.println("Demande : " + formater(x) + " " + symbole + " " + formater(y)
                            + " -> resultat envoye : " + formater(resultat));
                } else {
                    dos.writeBoolean(false);
                    dos.writeUTF(erreur);
                    System.out.println("Demande : " + formater(x) + " " + symbole + " " + formater(y)
                            + " -> " + erreur);
                }
                dos.flush();
            }

            socket.close();
            serverSocket.close();
            System.out.println("Sockets du serveur fermees.");
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }

    /** Affiche 12.0 sous la forme 12, et garde les décimales sinon. */
    static String formater(double v) {
        return (v == Math.rint(v) && !Double.isInfinite(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
