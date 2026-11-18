# TP5 — Invocation de méthodes sur des objets distants avec Java RMI

| | |
|---|---|
| **Matière** | Développement d'applications réparties |
| **Auditoire** | LSI 3 — Faculté des Sciences de Sfax |
| **Année universitaire** | 2026 – 2027 |
| **Étudiant(e)** | _Nom Prénom à compléter_ |

Ce dossier contient les deux activités du TP 5 :

- **Activité 5-1** : service de **conversion de devises** (euro → dinar) avec RMI ;
- **Activité 5-2** : service de **banque** avec RMI, puis avec **JNDI**.

Comme demandé, chaque application est découpée en **deux projets indépendants**, un serveur et un client. Le client ne connaît du serveur que l'**interface distante**, qu'il reçoit sous forme d'un fichier `.jar`.

---

## Structure

```
TP5/
├── README.md
├── RMIConversion_Server/                 # Activite 5-1 - serveur
│   └── src/
│       ├── rmiService/
│       │   ├── IConversion.java          # interface distante
│       │   └── ConversionImpl.java       # implementation (reste sur le serveur)
│       └── rmiServer/
│           └── ConversionServer.java     # registre + publication
├── RMIConversion_Client/                 # Activite 5-1 - client
│   ├── lib/conversionClient.jar          # contient UNIQUEMENT rmiService/IConversion.class
│   └── src/rmiClient/ConversionClient.java
├── RMIBanque_Server/                     # Activite 5-2 - serveur
│   └── src/
│       ├── jndi.properties               # configuration JNDI (etape 5)
│       ├── metier/Compte.java            # classe Serializable
│       ├── rmiService/
│       │   ├── IBanque.java              # interface distante
│       │   └── BanqueImpl.java           # implementation
│       └── rmiServer/
│           ├── BanqueServer.java         # etapes 2-4 : LocateRegistry + Naming
│           └── BanqueServerJNDI.java     # etape 5 : InitialContext
└── RMIBanque_Client/                     # Activite 5-2 - client
    ├── lib/banqueClient.jar              # contient UNIQUEMENT IBanque.class et Compte.class
    └── src/
        ├── jndi.properties
        └── rmiClient/
            ├── BanqueClient.java         # etapes 3-4 : Naming.lookup
            └── BanqueClientJNDI.java     # etape 5 : InitialContext.lookup
```

## Compilation, génération des `.jar` et exécution

Depuis le dossier `TP5/` (PowerShell). Le séparateur de classpath sous Windows est `;`.

```powershell
# ---------- Activité 5-1 ----------
# Serveur : compilation, puis export de l'interface seule dans conversionClient.jar
cd RMIConversion_Server
javac -encoding UTF-8 -d out src/rmiService/*.java src/rmiServer/*.java
jar cf ../RMIConversion_Client/lib/conversionClient.jar -C out rmiService/IConversion.class
cd ../RMIConversion_Client
javac -encoding UTF-8 -cp lib/conversionClient.jar -d out src/rmiClient/*.java

# Exécution (deux terminaux)
cd RMIConversion_Server;  java -cp out rmiServer.ConversionServer
cd RMIConversion_Client;  java -cp "out;lib/conversionClient.jar" rmiClient.ConversionClient

# ---------- Activité 5-2 ----------
cd RMIBanque_Server
javac -encoding UTF-8 -d out src/metier/*.java src/rmiService/*.java src/rmiServer/*.java
copy src\jndi.properties out\
jar cf ../RMIBanque_Client/lib/banqueClient.jar -C out rmiService/IBanque.class -C out metier/Compte.class
cd ../RMIBanque_Client
javac -encoding UTF-8 -cp lib/banqueClient.jar -d out src/rmiClient/*.java
copy src\jndi.properties out\

# Étapes 2-4 (RMI direct)
cd RMIBanque_Server;  java -cp out rmiServer.BanqueServer
cd RMIBanque_Client;  java -cp "out;lib/banqueClient.jar" rmiClient.BanqueClient
# Étape 5 (JNDI)
cd RMIBanque_Server;  java -cp out rmiServer.BanqueServerJNDI
cd RMIBanque_Client;  java -cp "out;lib/banqueClient.jar" rmiClient.BanqueClientJNDI
```

> **Équivalent Eclipse :** la commande `jar cf ...` correspond à *Export → Java → JAR file* en ne cochant que `IConversion` (ou `IBanque` et `Compte`). L'option `-cp lib/...jar` correspond à *Properties → Java Build Path → Libraries → Add External JARs*.

> `javac` ne copie pas les fichiers non Java. Il faut donc copier `jndi.properties` dans `out/` pour qu'il soit dans le classpath à l'exécution.

---

## Principe de RMI

```
       JVM CLIENT                                         JVM SERVEUR
 ┌──────────────────────┐                       ┌────────────────────────────────┐
 │ ConversionClient     │   1. lookup("Conv..") │  RMI Registry (port 1099)      │
 │                      │ ────────────────────► │  "ConversionService" → stub    │
 │                      │ ◄──────────────────── │                                │
 │  IConversion stub    │   2. renvoie le stub  │                                │
 │  (proxy)             │                       │                                │
 │   convertirMontant() │   3. appel (TCP)      │  ConversionImpl                │
 │   ───────────────────┼─────────────────────► │  .convertirMontant(500)        │
 │   ◄──────────────────┼────────────────────── │   = 500 * 3.40                 │
 └──────────────────────┘   4. résultat 1700.0  └────────────────────────────────┘
```

Le cycle de vie d'un service RMI comporte quatre étapes :

1. **Définition de l'interface** : elle étend `java.rmi.Remote`, et chaque méthode déclare `RemoteException`.
2. **Implémentation** : la classe hérite de `UnicastRemoteObject`, ce qui l'**exporte**. Elle écoute alors sur un port TCP et peut recevoir des appels.
3. **Enregistrement** : la référence de l'objet est publiée sous un nom dans l'annuaire (**RMI registry**).
4. **Invocation** : le client récupère dans l'annuaire un **stub** (proxy) qui implémente la même interface. Chaque appel sur ce stub est transmis sur le réseau et exécuté **sur le serveur**.

Par rapport aux sockets (TP1 à TP4), on n'écrit plus de protocole : ni flux, ni format de message, ni boucle de lecture. On appelle une méthode comme si l'objet était local. RMI se charge de la connexion TCP et de la sérialisation des paramètres et du résultat.

---

# Activité 5-1 : Service de conversion de devise

## Interface distante — `rmiService.IConversion`

```java
package rmiService;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IConversion extends Remote {
    double convertirMontant(double mt) throws RemoteException;
}
```

## Implémentation — `rmiService.ConversionImpl`

```java
public class ConversionImpl extends UnicastRemoteObject implements IConversion {

    public static final double TAUX_EUR_TND = 3.40;   // taux fixe pour l'exercice

    public ConversionImpl() throws RemoteException {
        super();                                       // exportation de l'objet
    }

    @Override
    public double convertirMontant(double mt) throws RemoteException {
        double resultat = mt * TAUX_EUR_TND;
        System.out.println("Appel distant convertirMontant(" + mt + ") -> " + resultat
                + " TND   [thread " + Thread.currentThread().getName() + "]");
        return resultat;
    }
}
```

## Serveur — `rmiServer.ConversionServer`

```java
LocateRegistry.createRegistry(1099);                          // i.   activation de l'annuaire
ConversionImpl conversion = new ConversionImpl();             // ii.  création de l'objet distant
Naming.rebind("rmi://localhost:1099/ConversionService", conversion);   // iii. publication
```

`createRegistry` démarre l'annuaire **dans la JVM du serveur**, ce qui évite de lancer l'outil `rmiregistry` à part. Après `rebind`, la méthode `main` se termine, mais la JVM reste active : l'objet exporté continue d'attendre les appels.

## Client — `rmiClient.ConversionClient`

```java
IConversion conversion = (IConversion) Naming.lookup("rmi://" + hote + ":1099/ConversionService");
double resultat = conversion.convertirMontant(500);
System.out.println("500.0 EUR = " + resultat + " TND");
```

Le projet client ne contient **que** `ConversionClient.java`. L'interface `IConversion` vient de `lib/conversionClient.jar` :

```
> jar tf lib/conversionClient.jar
META-INF/
META-INF/MANIFEST.MF
rmiService/IConversion.class
```

## Exécution

Console du serveur :

```
Annuaire RMI demarre sur le port 1099
Objet distant publie sous : rmi://localhost:1099/ConversionService
Serveur de conversion pret (1 EUR = 3.4 TND).
Appel distant convertirMontant(500.0) -> 1700.0 TND   [thread RMI TCP Connection(4)-192.168.1.14]
Appel distant convertirMontant(100.0) -> 340.0 TND   [thread RMI TCP Connection(4)-192.168.1.14]
Appel distant convertirMontant(12.5) -> 42.5 TND   [thread RMI TCP Connection(4)-192.168.1.14]
```

Console du client (`java ... rmiClient.ConversionClient localhost 100 12.5`) :

```
Reference obtenue depuis rmi://localhost:1099/ConversionService
Classe de l'objet obtenu : jdk.proxy1.$Proxy0
500.0 EUR = 1700.0 TND
100.0 EUR = 340.0 TND
12.5 EUR = 42.5 TND
```

**Interprétation :**

- `convertirMontant(500)` renvoie **1700.0 TND** (500 × 3,40). Les montants supplémentaires passés en argument sont convertis de la même façon.
- Le message « Appel distant ... » s'affiche dans la console du **serveur** : le calcul est bien exécuté côté serveur, pas chez le client.
- L'objet obtenu par le client est de classe `jdk.proxy1.$Proxy0`, et non `ConversionImpl`. C'est le **stub**, un proxy dynamique généré qui implémente `IConversion` et transmet chaque appel au serveur. Le client n'a d'ailleurs jamais eu accès à `ConversionImpl`.
- Côté serveur, l'appel est exécuté dans un thread `RMI TCP Connection(...)` créé par RMI. RMI est donc **multi-thread** par nature, comme le serveur du TP3.
- Si le serveur n'est pas lancé, le client obtient : `Connection refused to host: localhost`.

---

# Activité 5-2 : Service de banque avec RMI et JNDI

## Classe métier sérialisable — `metier.Compte`

```java
public class Compte implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int code;
    private double solde;
    private final Date dateCreation;

    public Compte(int code, double solde) {
        this(code, solde, new Date());
    }
    // constructeur complet, getters/setters, toString()
}
```

`Compte` n'est **pas** un objet distant. Il est passé **par valeur** : quand le client appelle `creerCompte(c)`, RMI sérialise le compte, l'envoie, et le serveur en reçoit une **copie**. C'est pourquoi la classe doit implémenter `Serializable` et être présente des deux côtés (elle est dans `banqueClient.jar`).

## Interface distante — `rmiService.IBanque`

```java
public interface IBanque extends Remote {
    String creerCompte(Compte c) throws RemoteException;
    String getInfoCompte(int code) throws RemoteException;
}
```

## Implémentation — `rmiService.BanqueImpl`

```java
public class BanqueImpl extends UnicastRemoteObject implements IBanque {

    private final Map<Integer, Compte> comptes = new ConcurrentHashMap<>();

    @Override
    public String creerCompte(Compte c) throws RemoteException {
        Compte existant = comptes.putIfAbsent(c.getCode(), c);   // vérification + insertion atomiques
        return (existant == null)
                ? "Compte " + c.getCode() + " cree avec un solde initial de " + c.getSolde()
                : "Erreur : un compte avec le code " + c.getCode() + " existe deja";
    }

    @Override
    public String getInfoCompte(int code) throws RemoteException {
        Compte c = comptes.get(code);
        return (c != null) ? c.toString() : "Erreur : aucun compte avec le code " + code;
    }
}
```

Le serveur RMI peut traiter plusieurs appels **en parallèle**, un thread par appel entrant. Avec une `HashMap` et un test « `containsKey` puis `put` », deux clients pourraient créer simultanément le même code. `ConcurrentHashMap.putIfAbsent()` effectue la vérification et l'insertion en **une seule opération atomique**.

## Étapes 2 à 4 : serveur et client avec le registre RMI

```java
// rmiServer.BanqueServer
LocateRegistry.createRegistry(1099);
Naming.rebind("rmi://localhost:1099/BanqueService", new BanqueImpl());
```

```java
// rmiClient.BanqueClient
IBanque banque = (IBanque) Naming.lookup("rmi://" + hote + ":1099/BanqueService");
banque.creerCompte(new Compte(1, 1000));
banque.creerCompte(new Compte(2, 2500.5));
banque.creerCompte(new Compte(1, 50));      // code déjà utilisé
banque.getInfoCompte(1);
banque.getInfoCompte(2);
banque.getInfoCompte(99);                   // code inexistant
```

`banqueClient.jar` ne contient que l'interface et la classe métier, sans `BanqueImpl` :

```
> jar tf lib/banqueClient.jar
META-INF/
META-INF/MANIFEST.MF
rmiService/IBanque.class
metier/Compte.class
```

### Exécution

Console du client (1re exécution) :

```
Reference obtenue depuis rmi://localhost:1099/BanqueService
> creerCompte(new Compte(1, 1000))
  Compte 1 cree avec un solde initial de 1000.0
> creerCompte(new Compte(2, 2500.5))
  Compte 2 cree avec un solde initial de 2500.5
> creerCompte(new Compte(1, 50))
  Erreur : un compte avec le code 1 existe deja
> getInfoCompte(1)
  Compte{code=1, solde=1000.0, dateCreation=18/11/2026 19:25:18}
> getInfoCompte(2)
  Compte{code=2, solde=2500.5, dateCreation=18/11/2026 19:25:18}
> getInfoCompte(99)
  Erreur : aucun compte avec le code 99
```

Console du client (2e exécution, serveur toujours actif) :

```
Reference obtenue depuis rmi://localhost:1099/BanqueService
> creerCompte(new Compte(1, 1000))
  Erreur : un compte avec le code 1 existe deja
> creerCompte(new Compte(2, 2500.5))
  Erreur : un compte avec le code 2 existe deja
...
```

Console du serveur :

```
Annuaire RMI demarre sur le port 1099
Objet distant publie sous : rmi://localhost:1099/BanqueService
Serveur de banque pret.
creerCompte(Compte{code=1, solde=1000.0, dateCreation=18/11/2026 19:25:18}) -> Compte 1 cree avec un solde initial de 1000.0
creerCompte(Compte{code=2, solde=2500.5, dateCreation=18/11/2026 19:25:18}) -> Compte 2 cree avec un solde initial de 2500.5
creerCompte(Compte{code=1, solde=50.0, dateCreation=18/11/2026 19:25:18}) -> Erreur : un compte avec le code 1 existe deja
getInfoCompte(1) -> Compte{code=1, solde=1000.0, dateCreation=18/11/2026 19:25:18}
getInfoCompte(2) -> Compte{code=2, solde=2500.5, dateCreation=18/11/2026 19:25:18}
getInfoCompte(99) -> Erreur : aucun compte avec le code 99
creerCompte(Compte{code=1, solde=1000.0, dateCreation=18/11/2026 19:25:18}) -> Erreur : un compte avec le code 1 existe deja
...
```

**Interprétation :**

- Les comptes 1 et 2 sont créés. La seconde création avec le code 1 (solde 50) est **refusée**, et le compte 1 garde son solde de 1000.
- `getInfoCompte` renvoie les informations complètes, y compris la date de création. Pour un code inconnu (99), elle renvoie un message d'erreur.
- Le serveur affiche le `Compte` reçu en paramètre : l'objet a bien été **sérialisé** chez le client et **reconstruit** sur le serveur.
- Lors de la 2e exécution du client, les comptes existent toujours. L'état est conservé **sur le serveur central**, partagé par tous les clients, et non dans le client qui se termine.

## Étape 5 : Utilisation de JNDI

**JNDI** (*Java Naming and Directory Interface*) est une API **générique** d'accès aux services de nommage et d'annuaire, comme le registre RMI, LDAP ou le DNS. Le code utilise toujours la même interface `Context` (`bind`, `rebind`, `lookup`). Le service réellement utilisé est choisi par **configuration**, sans modifier le code.

### Configuration — `src/jndi.properties` (serveur et client)

```properties
java.naming.factory.initial=com.sun.jndi.rmi.registry.RegistryContextFactory
java.naming.provider.url=rmi://localhost:1099
```

- `java.naming.factory.initial` : la fabrique de contexte, ici celle qui s'appuie sur le **registre RMI** ;
- `java.naming.provider.url` : l'adresse du registre.

`new InitialContext()` lit automatiquement ce fichier quand il se trouve dans le **classpath**, d'où la copie dans `out/`.

### Serveur — `rmiServer.BanqueServerJNDI`

```java
LocateRegistry.createRegistry(1099);       // le registre RMI reste le service d'annuaire sous-jacent
BanqueImpl banque = new BanqueImpl();

Context contexte = new InitialContext();   // configuré par jndi.properties
contexte.rebind("BanqueService", banque);  // au lieu de Naming.rebind("rmi://localhost:1099/BanqueService", ...)
```

### Client — `rmiClient.BanqueClientJNDI`

```java
Context contexte = new InitialContext();
IBanque banque = (IBanque) contexte.lookup("BanqueService");   // au lieu de Naming.lookup(url)
```

Le nom est désormais **relatif** (`BanqueService`). L'adresse du registre ne figure plus dans le code mais dans `jndi.properties`. Pour un serveur distant, il suffit de la remplacer, sans recompiler :

```powershell
java -Djava.naming.provider.url=rmi://192.168.1.14:1099 -cp "out;lib/banqueClient.jar" rmiClient.BanqueClientJNDI
```

### Exécution avec JNDI

Console du serveur :

```
Annuaire RMI demarre sur le port 1099
Contexte JNDI : com.sun.jndi.rmi.registry.RegistryContextFactory -> rmi://localhost:1099
Objet distant enregistre via JNDI sous le nom : BanqueService
Serveur de banque (JNDI) pret.
creerCompte(Compte{code=1, solde=1000.0, dateCreation=18/11/2026 19:25:21}) -> Compte 1 cree avec un solde initial de 1000.0
creerCompte(Compte{code=2, solde=2500.5, dateCreation=18/11/2026 19:25:21}) -> Compte 2 cree avec un solde initial de 2500.5
creerCompte(Compte{code=1, solde=50.0, dateCreation=18/11/2026 19:25:21}) -> Erreur : un compte avec le code 1 existe deja
getInfoCompte(1) -> Compte{code=1, solde=1000.0, dateCreation=18/11/2026 19:25:21}
getInfoCompte(2) -> Compte{code=2, solde=2500.5, dateCreation=18/11/2026 19:25:21}
getInfoCompte(99) -> Erreur : aucun compte avec le code 99
```

Console du client JNDI :

```
Contexte JNDI : rmi://localhost:1099
Reference obtenue via JNDI : BanqueService
> creerCompte(new Compte(1, 1000))
  Compte 1 cree avec un solde initial de 1000.0
> creerCompte(new Compte(2, 2500.5))
  Compte 2 cree avec un solde initial de 2500.5
> creerCompte(new Compte(1, 50))
  Erreur : un compte avec le code 1 existe deja
> getInfoCompte(1)
  Compte{code=1, solde=1000.0, dateCreation=18/11/2026 19:25:21}
> getInfoCompte(2)
  Compte{code=2, solde=2500.5, dateCreation=18/11/2026 19:25:21}
> getInfoCompte(99)
  Erreur : aucun compte avec le code 99
```

Client JNDI lancé **sans** serveur :

```
Contexte JNDI : rmi://localhost:1099
Erreur JNDI : javax.naming.ServiceUnavailableException [Root exception is java.rmi.ConnectException: Connection refused to host: localhost; ...]
```

**Interprétation :**

- Les résultats sont **identiques** à ceux obtenus avec `Naming`. Seule la manière d'enregistrer et de retrouver l'objet a changé.
- Les deux méthodes sont **interopérables**. Le client `BanqueClient` (avec `Naming.lookup`) retrouve sans problème l'objet enregistré par `BanqueServerJNDI`, car derrière JNDI, c'est **le même registre RMI** sur le port 1099.
- En cas d'erreur, JNDI encapsule l'exception RMI d'origine (`ConnectException`) dans sa propre hiérarchie (`NamingException`). Le code client ne dépend ainsi que de l'API JNDI.

---

## Tests en réseau local

Sur la machine serveur, il faut indiquer à RMI l'adresse IP à inscrire dans les stubs. Sinon, le stub peut contenir une adresse injoignable depuis les autres machines :

```powershell
java -Djava.rmi.server.hostname=192.168.1.14 -cp out rmiServer.ConversionServer
```

Sur la machine cliente :

```powershell
java -cp "out;lib/conversionClient.jar" rmiClient.ConversionClient 192.168.1.14
java -Djava.naming.provider.url=rmi://192.168.1.14:1099 -cp "out;lib/banqueClient.jar" rmiClient.BanqueClientJNDI
```

Il faut aussi autoriser Java dans le pare-feu Windows : le port 1099 du registre, plus le port anonyme de l'objet exporté.

---

# Synthèse

| | Sockets (TP1 à TP4) | RMI (TP5) |
|---|---|---|
| Abstraction | Octets, lignes ou objets envoyés dans un flux | **Appel de méthode** sur un objet distant |
| Protocole d'échange | À écrire soi-même (ordre et format des messages) | Défini par l'**interface** Java |
| Sérialisation | Manuelle (`ObjectOutputStream`) | Automatique (paramètres et résultat) |
| Localisation du serveur | Adresse IP + port | **Nom** dans un annuaire (registre RMI / JNDI) |
| Multi-thread côté serveur | À programmer (TP3) | Fourni par RMI |
| Interopérabilité | Tout langage (texte) | Java ↔ Java uniquement |

- Le client ne manipule que l'**interface** (`IConversion`, `IBanque`), fournie par un `.jar` sans l'implémentation. Le serveur peut ainsi changer `ConversionImpl` ou `BanqueImpl` sans que le client soit recompilé.
- Un **objet distant** (`UnicastRemoteObject`) reste sur le serveur et est manipulé à distance via un stub. Un **objet sérialisable** (`Compte`) est copié d'une JVM à l'autre.
- **JNDI** ajoute une couche d'abstraction au-dessus du registre RMI. Le code n'utilise que `InitialContext`, et le service d'annuaire comme son adresse sont choisis par configuration (`jndi.properties` ou `-D...`).
