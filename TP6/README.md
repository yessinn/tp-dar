# TP6 — Communications client-serveur via CORBA : service Banque

| | |
|---|---|
| **Matière** | Développement d'applications réparties |
| **Auditoire** | LSI 3 — Faculté des Sciences de Sfax |
| **Année universitaire** | 2026 – 2027 |
| **Étudiant(e)** | _Nom Prénom à compléter_ |

Application répartie **CORBA** dans laquelle un serveur gère l'ensemble des comptes bancaires. Un compte est défini par un **code** et un **solde**. Les clients peuvent :

- créer un compte avec une somme initiale ;
- verser une somme sur un compte ;
- retirer une somme d'un compte ;
- consulter un compte ;
- consulter la liste de tous les comptes ;
- convertir un montant d'euros (€) en dinars tunisiens (DT).

---

## Structure

```
TP6/
├── README.md
├── TP6_CorbaBanque_Server/
│   └── src/
│       ├── Banque.idl                 # interface du service (IDL)
│       ├── jndi.properties            # adresse du service de nommage
│       ├── corbaBanque/               # code GENERE par idlj -fall (stub, squelette, helpers...)
│       ├── service/BanqueImpl.java    # servant : code metier
│       └── corbaServer/BanqueServer.java
└── TP6_CorbaBanque_Client/
    └── src/
        ├── Banque.idl                 # copie de l'IDL du serveur
        ├── jndi.properties            # copie
        ├── corbaBanque/               # code GENERE par idlj -fall
        └── corbaClient/BanqueClient.java
```

## Environnement : JDK 8 obligatoire

CORBA (`org.omg.*`) et ses outils (`idlj`, `tnameserv`, `orbd`) ont été **retirés de Java à partir de la version 11**. Le TP se compile et s'exécute donc avec le **JDK 8**, ici `C:\Program Files\Java\jdk1.8.0_202`. Avec le JDK 21 utilisé pour les TP précédents, `org.omg.CORBA` est introuvable.

```powershell
$J8 = "C:\Program Files\Java\jdk1.8.0_202\bin"
& "$J8\java" -version      # java version "1.8.0_202"
```

## Compilation et exécution

```powershell
# 1. Compilation de l'IDL (dans le src de CHAQUE projet)
cd TP6_CorbaBanque_Server\src;  & "$J8\idlj" -fall -v Banque.idl
cd TP6_CorbaBanque_Client\src;  & "$J8\idlj" -fall Banque.idl

# 2. Compilation Java (dans chaque projet) + copie de jndi.properties dans le classpath
mkdir out
& "$J8\javac" -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
copy src\jndi.properties out\

# 3. Exécution, dans cet ordre et dans trois terminaux
& "$J8\tnameserv" -ORBInitialPort 900                                         # a. service de nommage
cd TP6_CorbaBanque_Server;  & "$J8\java" -cp out corbaServer.BanqueServer     # b. serveur
cd TP6_CorbaBanque_Client;  & "$J8\java" -cp out corbaClient.BanqueClient     # c. client
```

---

## Principe de CORBA

```
        CLIENT (Java)                                              SERVEUR (Java)
 ┌───────────────────────┐                                ┌──────────────────────────────┐
 │ BanqueClient          │                                │ BanqueImpl (servant)         │
 │   stub.verser(250,1)  │                                │   verser(250, 1) { ... }     │
 │          │            │                                │          ▲                   │
 │ _IBanqueRemoteStub    │                                │ IBanqueRemotePOA (squelette) │
 │   (stub, généré)      │                                │          ▲                   │
 │          │            │                                │         POA                  │
 │         ORB ──────────┼────── requête IIOP (TCP) ─────►│         ORB                  │
 └───────────────────────┘                                └──────────────────────────────┘
            │  lookup("BanqueService")                       │  rebind("BanqueService", ref)
            └────────────►  Service de nommage (tnameserv, port 900)  ◄────────┘
```

- **IDL** (*Interface Definition Language*) : le contrat du service, écrit dans un langage **neutre**. Un client C++ ou Python pourrait utiliser le même `Banque.idl`. C'est la grande différence avec RMI, qui est réservé à Java.
- **`idlj`** traduit l'IDL en Java. Il génère le **stub** côté client et le **squelette** (`IBanqueRemotePOA`) côté serveur.
- **ORB** (*Object Request Broker*) : le bus qui achemine les requêtes entre client et serveur avec le protocole **IIOP**.
- **POA** (*Portable Object Adapter*) : il relie les requêtes reçues par l'ORB au **servant** (l'objet Java qui contient le code métier).
- **Service de nommage** (`tnameserv`) : l'annuaire qui associe un nom à la référence de l'objet (IOR). On y accède ici via **JNDI**.

---

# Étape 2 : Développement du serveur

## Interface IDL — `Banque.idl`

```idl
module corbaBanque {

    struct Compte {
        long  code;
        float solde;
    };

    typedef sequence<Compte> tabComptes;

    exception OperationRefusee {
        string message;
    };

    interface IBanqueRemote {
        void       creerCompte(in Compte cpte)          raises (OperationRefusee);
        void       verser(in float mt, in long code)    raises (OperationRefusee);
        void       retirer(in float mt, in long code)   raises (OperationRefusee);
        Compte     getCompte(in long code)              raises (OperationRefusee);
        tabComptes getComptes();
        double     conversion(in float mt);
    };
};
```

| IDL | Java généré |
|---|---|
| `module corbaBanque` | package `corbaBanque` |
| `struct Compte` | classe `Compte` avec des champs publics `code` et `solde` (passée **par valeur**) |
| `long` / `float` / `double` | `int` / `float` / `double` (un `long` IDL fait 32 bits) |
| `typedef sequence<Compte> tabComptes` | `Compte[]` |
| `exception OperationRefusee` | classe `OperationRefusee extends org.omg.CORBA.UserException` |
| `in` | paramètre transmis du client vers le serveur |

L'exception `OperationRefusee` complète l'énoncé. Elle permet au serveur de signaler proprement au client un code déjà utilisé, un compte inexistant ou un solde insuffisant, au lieu d'ignorer l'opération sans rien dire.

## Compilation IDL : `idlj -fall -v Banque.idl`

```
Generating corbaBanque
done   -   corbaBanque
```

L'option `-fall` génère à la fois les fichiers **client** et **serveur** :

| Fichier généré | Rôle |
|---|---|
| `IBanqueRemoteOperations.java` | Interface Java des 6 méthodes (ce que le servant implémente) |
| `IBanqueRemote.java` | Interface CORBA complète (`Operations` + `org.omg.CORBA.Object`) |
| `IBanqueRemotePOA.java` | **Squelette** serveur : décode les requêtes et appelle le servant |
| `_IBanqueRemoteStub.java` | **Stub** client : encode les appels en requêtes IIOP |
| `IBanqueRemoteHelper.java` | `narrow()` : conversion d'une référence générique en `IBanqueRemote` |
| `Compte.java`, `CompteHelper`, `CompteHolder` | La structure et son (dé)codage |
| `tabComptesHelper`, `tabComptesHolder` | Le tableau de comptes |
| `OperationRefusee*.java` | L'exception et son (dé)codage |

## Servant — `service.BanqueImpl`

```java
public class BanqueImpl extends IBanqueRemotePOA {

    public static final double TAUX_EUR_DT = 3.40;
    private final Map<Integer, Compte> comptes = new TreeMap<>();

    @Override
    public synchronized void creerCompte(Compte cpte) throws OperationRefusee {
        if (comptes.containsKey(cpte.code))
            refuser("creerCompte", "un compte avec le code " + cpte.code + " existe deja");
        if (cpte.solde < 0)
            refuser("creerCompte", "le solde initial ne peut pas etre negatif");
        comptes.put(cpte.code, new Compte(cpte.code, cpte.solde));
    }

    @Override
    public synchronized void verser(float mt, int code) throws OperationRefusee {
        Compte c = trouver("verser", code);                 // refuse si code inconnu
        if (mt <= 0) refuser("verser", "le montant doit etre positif");
        c.solde += mt;
    }

    @Override
    public synchronized void retirer(float mt, int code) throws OperationRefusee {
        Compte c = trouver("retirer", code);
        if (mt <= 0) refuser("retirer", "le montant doit etre positif");
        if (mt > c.solde) refuser("retirer", "solde insuffisant sur le compte " + code + " ...");
        c.solde -= mt;
    }

    @Override
    public synchronized Compte getCompte(int code) throws OperationRefusee {
        Compte c = trouver("getCompte", code);
        return new Compte(c.code, c.solde);
    }

    @Override
    public synchronized Compte[] getComptes() {
        // copie de toutes les valeurs dans un tableau
    }

    @Override
    public double conversion(float mt) {
        return mt * TAUX_EUR_DT;
    }

    private void refuser(String operation, String message) throws OperationRefusee {
        throw new OperationRefusee(message);
    }
}
```

- `BanqueImpl` hérite du squelette `IBanqueRemotePOA` et implémente les méthodes de `IBanqueRemoteOperations`.
- L'ORB peut traiter plusieurs requêtes en parallèle. Les méthodes qui lisent ou modifient les comptes sont donc `synchronized`, ce qui évite par exemple deux retraits simultanés qui passeraient tous les deux sous le solde.
- `getCompte` et `getComptes` renvoient des **copies**, car une `struct` CORBA voyage par valeur.

## Configuration JNDI — `jndi.properties`

```properties
java.naming.factory.initial=com.sun.jndi.cosnaming.CNCtxFactory
java.naming.provider.url=iiop://localhost:900
```

`CNCtxFactory` est la fabrique JNDI pour le **COS Naming** (le service de nommage CORBA), joignable en IIOP sur le port 900 où écoute `tnameserv`.

## Serveur — `corbaServer.BanqueServer`

```java
// a. initialisation de l'ORB
ORB orb = ORB.init(args, null);

// b. adaptateur d'objets : RootPOA + activation du POAManager
POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
rootPOA.the_POAManager().activate();

// c. annuaire JNDI (jndi.properties)
Context contexte = new InitialContext();

// d. création du servant
BanqueImpl servant = new BanqueImpl();

// e. enregistrement : référence CORBA du servant, publiée dans l'annuaire
org.omg.CORBA.Object reference = rootPOA.servant_to_reference(servant);
contexte.rebind("BanqueService", reference);

// f. attente des requêtes
orb.run();
```

- Le **POAManager** doit être **activé**, sinon le POA met les requêtes en attente sans jamais les traiter.
- `servant_to_reference` active le servant dans le POA et renvoie sa **référence d'objet** (IOR). L'IOR contient notamment l'adresse IP, le port et l'identifiant de l'objet. C'est cette référence qui est publiée sous le nom `BanqueService`.
- `orb.run()` est **bloquant** : le serveur reste en attente des requêtes.

---

# Étape 3 : Développement du client

Le projet client reçoit une copie de `Banque.idl` et de `jndi.properties`, puis l'IDL est recompilé avec `idlj -fall`. Le client n'a besoin que du stub, de l'interface et des helpers, et **pas** de `BanqueImpl`.

```java
// a. propriétés de l'annuaire JNDI (jndi.properties)
Context contexte = new InitialContext();

// b. référence de l'objet distant
Object ref = contexte.lookup("BanqueService");

// c. conversion vers IBanqueRemote : création du stub
IBanqueRemote stub = IBanqueRemoteHelper.narrow((org.omg.CORBA.Object) ref);

// d. appels distants
stub.creerCompte(new Compte(1, 1000f));
stub.verser(250f, 1);
stub.retirer(200f, 2);
Compte c = stub.getCompte(1);
Compte[] tous = stub.getComptes();
double dt = stub.conversion(500f);
```

`lookup` renvoie une référence CORBA **générique** (`org.omg.CORBA.Object`). `IBanqueRemoteHelper.narrow()` vérifie auprès de l'objet qu'il est bien du type `IDL:corbaBanque/IBanqueRemote:1.0`, puis construit le stub typé `_IBanqueRemoteStub`. Un simple cast Java ne suffirait pas.

Chaque appel susceptible d'échouer est entouré d'un `try / catch (OperationRefusee e)` pour afficher le message renvoyé par le serveur (`e.message`).

---

# Étape 4 : Exécution

## 1. Service de nommage

```
> tnameserv -ORBInitialPort 900
Initial Naming Context:
IOR:000000000000002b49444c3a6f6d672e6f72672f436f734e616d696e672f4e616d696e67436f6e746578744578743a312e30...
TransientNameServer: setting port for initial object references to: 900
Ready.
```

## 2. Serveur

```
ORB initialise.
RootPOA recupere et POAManager active.
Annuaire JNDI : iiop://localhost:900
Reference publiee sous le nom : BanqueService
IOR : IOR:000000000000002249444c3a636f72626142616e7175652f4942616e...
Serveur CORBA pret, en attente de requetes...
```

## 3. Client

```
Annuaire JNDI : iiop://localhost:900
Stub obtenu : corbaBanque._IBanqueRemoteStub
--- creerCompte ---
creerCompte(1, 1000) -> OK
creerCompte(2, 500) -> OK
creerCompte(3, 2500) -> OK
creerCompte(1, 50)  [code deja utilise] -> refuse : un compte avec le code 1 existe deja
--- verser ---
verser(250, 1) -> OK
verser(100, 99) [compte inexistant] -> refuse : aucun compte avec le code 99
--- retirer ---
retirer(200, 2) -> OK
retirer(1000, 2) [solde insuffisant] -> refuse : solde insuffisant sur le compte 2 (solde 300.0, retrait demande 1000.0)
--- getCompte ---
   Compte{code=1, solde=1250.0}
getCompte(1) -> OK
getCompte(99) [compte inexistant] -> refuse : aucun compte avec le code 99
--- getComptes ---
getComptes() -> 3 compte(s) :
   Compte{code=1, solde=1250.0}
   Compte{code=2, solde=300.0}
   Compte{code=3, solde=2500.0}
--- conversion ---
conversion(500 EUR) -> 1700.0 DT
conversion(1250.0 EUR, solde du compte 1) -> 4250.0 DT
```

Suite de la console du serveur, pendant l'exécution du client :

```
[serveur] creerCompte(code=1, solde=1000.0) -> OK
[serveur] creerCompte(code=2, solde=500.0) -> OK
[serveur] creerCompte(code=3, solde=2500.0) -> OK
[serveur] creerCompte -> REFUSE : un compte avec le code 1 existe deja
[serveur] verser(250.0, code=1) -> nouveau solde 1250.0
[serveur] verser -> REFUSE : aucun compte avec le code 99
[serveur] retirer(200.0, code=2) -> nouveau solde 300.0
[serveur] retirer -> REFUSE : solde insuffisant sur le compte 2 (solde 300.0, retrait demande 1000.0)
[serveur] getCompte(1) -> solde 1250.0
[serveur] getCompte -> REFUSE : aucun compte avec le code 99
[serveur] getComptes() -> 3 compte(s)
[serveur] conversion(500.0 EUR) -> 1700.0 DT
[serveur] conversion(1250.0 EUR) -> 4250.0 DT
```

**Interprétation :**

- **Les six méthodes** fonctionnent à distance. Le client obtient un objet de classe `_IBanqueRemoteStub` (le stub généré par `idlj`), et chaque appel est exécuté par le servant, comme le montrent les lignes `[serveur]`.
- **Calculs** : compte 1 : 1000 + 250 = **1250** ; compte 2 : 500 − 200 = **300** ; compte 3 inchangé à **2500**. La conversion donne 500 € × 3,40 = **1700 DT**.
- **Cas d'erreur** : code déjà utilisé, compte inexistant et solde insuffisant sont détectés par le serveur. Ils sont transmis au client sous forme d'**exception IDL** `OperationRefusee`, avec son message, puis affichés par le client. Le solde du compte 2 reste à 300 après le retrait refusé.
- `getComptes` renvoie la `sequence<Compte>` IDL sous forme de **tableau Java** `Compte[]`, trié par code.

## Deuxième exécution du client (serveur toujours actif)

```
--- creerCompte ---
creerCompte(1, 1000) -> refuse : un compte avec le code 1 existe deja
creerCompte(2, 500) -> refuse : un compte avec le code 2 existe deja
creerCompte(3, 2500) -> refuse : un compte avec le code 3 existe deja
...
```

Côté serveur :

```
[serveur] verser(250.0, code=1) -> nouveau solde 1500.0
[serveur] retirer(200.0, code=2) -> nouveau solde 100.0
[serveur] retirer -> REFUSE : solde insuffisant sur le compte 2 (solde 100.0, retrait demande 1000.0)
```

**Interprétation :** les comptes sont conservés **sur le serveur** entre deux clients. La création est refusée, et les versements et retraits s'appliquent aux soldes laissés par le premier client (1250 + 250 = 1500 ; 300 − 200 = 100).

## Ordre de lancement

Si le service de nommage n'est pas lancé, le client (comme le serveur) échoue dès l'accès à l'annuaire :

```
WARNING: "IOP00410201: (COMM_FAILURE) Connection failure: socketType: IIOP_CLEAR_TEXT; hostname: localhost; port: 900"
org.omg.CORBA.COMM_FAILURE:   vmcid: SUN  minor code: 201  completed: No
```

D'où l'ordre imposé : **service de nommage → serveur → client**. Le serveur doit publier sa référence avant que le client ne la cherche.

---

# Synthèse

| | RMI (TP5) | CORBA (TP6) |
|---|---|---|
| Définition du contrat | Interface Java (`extends Remote`) | Fichier **IDL**, neutre vis-à-vis du langage |
| Génération du code | Automatique (proxy dynamique) | `idlj` génère le stub, le squelette et les helpers |
| Langages | Java ↔ Java uniquement | **Multi-langages** (Java, C++, Python...) |
| Protocole | JRMP | **IIOP** |
| Côté serveur | `UnicastRemoteObject` | Servant + **POA** + ORB |
| Annuaire | RMI registry (port 1099) | Service de nommage COS Naming (`tnameserv`, port 900) |
| Accès à l'annuaire | `Naming` ou JNDI (`RegistryContextFactory`) | JNDI (`CNCtxFactory`) |
| Conversion de la référence | Cast Java | `IBanqueRemoteHelper.narrow()` |
| Erreurs métier | Exceptions Java | `exception` IDL → `UserException` |
| Disponibilité dans le JDK | Toujours présent | **Retiré depuis Java 11** (JDK 8 nécessaire ici) |

- CORBA sépare clairement le **contrat** (IDL), le **transport** (ORB / IIOP), l'**adaptation** vers l'objet (POA) et le **code métier** (servant). Le client ne connaît que l'IDL et le stub généré.
- Le service de nommage découple client et serveur : le client ne connaît que le **nom** `BanqueService`, et l'IOR (adresse, port, identifiant de l'objet) est obtenue dynamiquement.
- Comme pour RMI, le serveur traite les requêtes de plusieurs clients en parallèle. L'état partagé (les comptes) doit donc être **synchronisé**.
- CORBA a été retiré des versions récentes de Java. Aujourd'hui, on lui préfère des technologies comme **REST**, **gRPC** (qui garde l'idée d'un contrat IDL avec Protocol Buffers) ou des middlewares orientés messages (JMS / ActiveMQ, chapitre 4 du cours).
