package rmiService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import metier.Compte;

/**
 * Implémentation du service de banque. Tous les comptes sont conservés en
 * mémoire sur le serveur central.
 *
 * RMI peut exécuter plusieurs appels en parallèle (un thread par appel
 * entrant) : la table des comptes est donc une ConcurrentHashMap, et la
 * création utilise putIfAbsent(), qui vérifie et insère de façon atomique.
 */
public class BanqueImpl extends UnicastRemoteObject implements IBanque {

    private static final long serialVersionUID = 1L;

    private final Map<Integer, Compte> comptes = new ConcurrentHashMap<>();

    public BanqueImpl() throws RemoteException {
        super();
    }

    @Override
    public String creerCompte(Compte c) throws RemoteException {
        Compte existant = comptes.putIfAbsent(c.getCode(), c);
        String reponse = (existant == null)
                ? "Compte " + c.getCode() + " cree avec un solde initial de " + c.getSolde()
                : "Erreur : un compte avec le code " + c.getCode() + " existe deja";
        System.out.println("creerCompte(" + c + ") -> " + reponse);
        return reponse;
    }

    @Override
    public String getInfoCompte(int code) throws RemoteException {
        Compte c = comptes.get(code);
        String reponse = (c != null) ? c.toString() : "Erreur : aucun compte avec le code " + code;
        System.out.println("getInfoCompte(" + code + ") -> " + reponse);
        return reponse;
    }
}
