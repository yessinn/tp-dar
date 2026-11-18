package serverPackage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;

/**
 * Activité 1-3 (étape 1) : connexion Client/Serveur sur deux machines distinctes.
 * Le ServerSocket est lié (bind) à l'adresse IP réelle de la machine (et non plus à localhost)
 * grâce à InetAddress et InetSocketAddress.
 *
 * Utilisation : java -cp out serverPackage.ServerReseau [adresseIP]
 * Sans argument, la première adresse IPv4 du réseau local détectée est utilisée.
 */
public class ServerReseau {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        try {
            InetAddress ip = (args.length > 0) ? InetAddress.getByName(args[0]) : trouverAdresseLocale();
            InetSocketAddress adresseEcoute = new InetSocketAddress(ip, PORT);

            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(adresseEcoute);
            System.out.println("Nom de la machine serveur : " + InetAddress.getLocalHost().getHostName());
            System.out.println("Serveur a l'ecoute sur " + ip.getHostAddress() + ":" + PORT);
            System.out.println("Je suis un serveur en attente de la connexion d'un client ");

            Socket socket = serverSocket.accept();
            InetSocketAddress adresseClient = (InetSocketAddress) socket.getRemoteSocketAddress();
            System.out.println("un client est connecte depuis "
                    + adresseClient.getAddress().getHostAddress() + ":" + adresseClient.getPort());

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (true) {
                int x = dis.readInt();
                if (x == 0) {
                    System.out.println("Le client a envoye 0 : fin de la communication.");
                    break;
                }
                int resultat = x * 5;
                dos.writeInt(resultat);
                dos.flush();
                System.out.println("Recu x = " + x + " -> resultat envoye : " + resultat);
            }

            socket.close();
            serverSocket.close();
            System.out.println("Sockets du serveur fermees.");
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }

    /**
     * Parcourt les interfaces réseau actives et affiche les adresses IPv4 disponibles.
     * Retourne la première adresse privée d'une carte physique (Wi-Fi / Ethernet),
     * en ignorant les cartes virtuelles (VirtualBox, VMware, Hyper-V, VPN...).
     * Si la détection ne convient pas, passer l'adresse IP voulue en argument.
     */
    static InetAddress trouverAdresseLocale() throws SocketException, UnknownHostException {
        InetAddress choisie = null;
        System.out.println("Adresses IPv4 disponibles sur cette machine :");
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                continue;
            }
            String nom = ni.getDisplayName().toLowerCase();
            boolean carteVirtuelle = nom.matches(".*(virtual|vmware|vbox|hyper-v|zerotier|tailscale|vpn|tap|tun).*");
            for (InetAddress adr : Collections.list(ni.getInetAddresses())) {
                if (adr instanceof Inet4Address && adr.isSiteLocalAddress()) {
                    System.out.println("  - " + adr.getHostAddress() + " (" + ni.getDisplayName() + ")"
                            + (carteVirtuelle ? " [virtuelle, ignoree]" : ""));
                    if (choisie == null && !carteVirtuelle) {
                        choisie = adr;
                    }
                }
            }
        }
        return (choisie != null) ? choisie : InetAddress.getLocalHost();
    }
}
