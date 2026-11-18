package service;

import java.util.Map;
import java.util.TreeMap;

import corbaBanque.Compte;
import corbaBanque.IBanqueRemotePOA;
import corbaBanque.OperationRefusee;

/**
 * Servant : implémentation concrète des méthodes de IBanqueRemoteOperations.
 *
 * Il hérite du squelette IBanqueRemotePOA généré par idlj : le squelette
 * reçoit les requêtes de l'ORB, décode les paramètres et appelle ces méthodes.
 *
 * L'ORB peut traiter plusieurs requêtes en parallèle : les méthodes qui
 * lisent ou modifient la table des comptes sont synchronisées.
 */
public class BanqueImpl extends IBanqueRemotePOA {

    /** Taux fixe utilisé pour l'exercice : 1 EUR = 3.40 DT. */
    public static final double TAUX_EUR_DT = 3.40;

    // TreeMap : les comptes sont listés par code croissant
    private final Map<Integer, Compte> comptes = new TreeMap<>();

    @Override
    public synchronized void creerCompte(Compte cpte) throws OperationRefusee {
        if (comptes.containsKey(cpte.code)) {
            refuser("creerCompte", "un compte avec le code " + cpte.code + " existe deja");
        }
        if (cpte.solde < 0) {
            refuser("creerCompte", "le solde initial ne peut pas etre negatif");
        }
        comptes.put(cpte.code, new Compte(cpte.code, cpte.solde));
        log("creerCompte(code=" + cpte.code + ", solde=" + cpte.solde + ") -> OK");
    }

    @Override
    public synchronized void verser(float mt, int code) throws OperationRefusee {
        Compte c = trouver("verser", code);
        if (mt <= 0) {
            refuser("verser", "le montant doit etre positif");
        }
        c.solde += mt;
        log("verser(" + mt + ", code=" + code + ") -> nouveau solde " + c.solde);
    }

    @Override
    public synchronized void retirer(float mt, int code) throws OperationRefusee {
        Compte c = trouver("retirer", code);
        if (mt <= 0) {
            refuser("retirer", "le montant doit etre positif");
        }
        if (mt > c.solde) {
            refuser("retirer", "solde insuffisant sur le compte " + code + " (solde " + c.solde
                    + ", retrait demande " + mt + ")");
        }
        c.solde -= mt;
        log("retirer(" + mt + ", code=" + code + ") -> nouveau solde " + c.solde);
    }

    @Override
    public synchronized Compte getCompte(int code) throws OperationRefusee {
        Compte c = trouver("getCompte", code);
        log("getCompte(" + code + ") -> solde " + c.solde);
        return new Compte(c.code, c.solde); // copie : la struct est envoyée par valeur
    }

    @Override
    public synchronized Compte[] getComptes() {
        Compte[] tab = new Compte[comptes.size()];
        int i = 0;
        for (Compte c : comptes.values()) {
            tab[i++] = new Compte(c.code, c.solde);
        }
        log("getComptes() -> " + tab.length + " compte(s)");
        return tab;
    }

    @Override
    public double conversion(float mt) {
        double resultat = mt * TAUX_EUR_DT;
        log("conversion(" + mt + " EUR) -> " + resultat + " DT");
        return resultat;
    }

    private Compte trouver(String operation, int code) throws OperationRefusee {
        Compte c = comptes.get(code);
        if (c == null) {
            refuser(operation, "aucun compte avec le code " + code);
        }
        return c;
    }

    private void refuser(String operation, String message) throws OperationRefusee {
        log(operation + " -> REFUSE : " + message);
        throw new OperationRefusee(message);
    }

    private static void log(String message) {
        System.out.println("[serveur] " + message);
    }
}
