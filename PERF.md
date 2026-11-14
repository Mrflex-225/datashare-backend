
# PERF.md — Tests de performance et observabilité

**Projet :** DataShare  
**Backend :** Spring Boot 4.1.1 — Java 21  
**Frontend :** Angular 21  
**Outil :** Grafana k6  
**Environnement :** Windows, backend sur `http://localhost:8080`, PostgreSQL, AWS S3  
**Campagnes :** 6 et 14 novembre 2026  
**Statut :** Campagne terminée — résultats et anomalies documentés

## 1. Objectif et périmètre

Évaluer la rapidité et la fiabilité de l'API de génération de liens de téléchargement à faible charge, sous charge constante et lors de montées progressives.

**Endpoint :** `POST /api/files/download/{token}`  
**Corps JSON :** `{"password": null}`  
**Comportement attendu :** statut HTTP 200 et propriété JSON `downloadUrl` contenant un lien.

Chaque utilisateur virtuel (VU) appelle l'endpoint puis effectue une pause d'une seconde (`sleep(1)`).

La mesure porte sur le **traitement HTTP et la génération d'URL**, et non sur le transfert effectif du fichier depuis S3.

Les résultats caractérisent l'environnement local testé, pas une capacité de production.

Les seuils k6 retenus sont :

| Critère | Seuil de validation |
|---|---|
| Latence HTTP au 95e percentile (p95) | `< 1 000 ms` |
| Taux d'échec des requêtes HTTP | `< 1 %` |
| Réussite des vérifications fonctionnelles | `> 99 %` |
| Erreurs métier (scripts progressifs) | `< 1 %` |

## 2. Méthode et exécution

Deux stratégies de charge ont été appliquées :

- **Charge constante :** nombre de VUs fixé dès le départ pour 60 secondes (100, 250 et 1 000 VUs).
- **Montée progressive :** augmentation par paliers jusqu'à 50 puis 250 VUs, avec maintien d'un palier final et descente contrôlée.

Un diagnostic préalable à une seule requête a validé le fonctionnement de l'endpoint.

Les scripts utilisés sont :

- `performance/download-load.js`
- `performance/download-ramp.js`
- `performance/download-ramp-250.js`

Le token de téléchargement est fourni par la variable d'environnement `DOWNLOAD_TOKEN`, sans être inscrit dans les scripts.

Les synthèses JSON sont produites avec `--summary-export` dans `performance/results/`.

### Commandes utilisées

Depuis la racine du backend :

```powershell
# Diagnostic
k6 run --vus 1 --iterations 1 .\performance\download-load.js

# Montée progressive jusqu'à 50 VUs
k6 run --summary-export=.\performance\results\download-ramp-summary.json .\performance\download-ramp.js

# Charge constante : 100 VUs
k6 run --vus 100 --duration 60s --summary-export=.\performance\results\load-100.json .\performance\download-load.js

# Charge constante : 250 VUs
k6 run --vus 250 --duration 60s --summary-export=.\performance\results\load-250.json .\performance\download-load.js

# Charge constante : 1 000 VUs
k6 run --vus 1000 --duration 60s --summary-export=.\performance\results\load-1000.json .\performance\download-load.js

# Montée progressive jusqu'à 250 VUs
k6 run --summary-export=.\performance\results\load-ramp-250.json .\performance\download-ramp-250.js
```

**Traçabilité :** lors de l'exécution d'origine à 1 000 VUs, l'option d'export désignait accidentellement `load-75.json`. Le nom `load-1000.json` ci-dessus est le nom cohérent de reproduction ; il ne change ni la charge appliquée ni les chiffres observés.

## 3. Résultats des campagnes

| Scénario | Durée | Requêtes | Débit moyen | Moyenne | p95 | Erreurs HTTP | Checks réussis | Verdict |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| Diagnostic : 1 VU | 1 itération | 1 | — | 46,82 ms | 46,82 ms¹ | 0 % | 2/2 | Fonctionnel |
| Montée 0 → 50 VUs | 3 min 30 s | 5 069 | 24,09 req/s | 4,67 ms | 10,05 ms | 0 % | 10 138/10 138 | Validé |
| 100 VUs constants | 60 s | 6 000 | 99,40 req/s | 5,06 ms | 10,95 ms | 0 % | 12 000/12 000 | Validé |
| 250 VUs constants | 60 s | 15 000 | 247,33 req/s | 7,12 ms | 35,97 ms | 0,80 % (120) | 29 760/30 000 (99,20 %) | Seuils respectés, erreurs relevées |
| 1 000 VUs constants | 60 s | 59 258 | 971,38 req/s | 11,18 ms | 35,25 ms | 1,26 % (751) | 117 014/118 516 (98,73 %) | Non validé |
| Montée 0 → 250 VUs | 4 min | 37 274 | 154,76 req/s | 7,16 ms | 22,52 ms | 0 % | 74 548/74 548 | Validé |

¹ Une seule requête ne permet pas de calculer un percentile représentatif.

Les deux scripts de montée progressive ont enregistré **0 % d'erreurs métier**.

Toutes les itérations ont été achevées sans interruption.

### Bilan principal

La montée progressive jusqu'à 250 VUs est le scénario le plus complet ayant satisfait tous les critères :

- **37 274 requêtes HTTP**
- **p95 global : 22,52 ms**
- **Taux d'erreur HTTP : 0 %**
- **Taux d'erreur métier : 0 %**
- **Vérifications réussies : 100 %**

Le test à 1 000 VUs n'a pas satisfait les critères de fiabilité. Il est conservé comme résultat négatif.

## 4. Analyse des performances et des anomalies

### 4.1. Temps de réponse et taux d'échec

La latence est restée inférieure au seuil fixé dans toutes les campagnes.

Le p95 n'est toutefois pas suffisant pour évaluer la fiabilité : lors du test à 1 000 VUs, **751 requêtes ont échoué**, malgré un p95 de 35,25 ms.

Le script à 250 VUs constants a enregistré **120 erreurs (0,80 %)**. Les seuils du script restent respectés, mais le résultat n'est pas exempt d'incidents.

En revanche, la montée progressive jusqu'au même nombre de VUs n'a enregistré **aucune erreur**.

Les mesures de p95 des scénarios progressifs regroupent toutes les étapes, de la faible charge au maximum.

Elles ne correspondent pas exclusivement au palier à 250 VUs.

De même, le débit moyen de **154,76 req/s** ne correspond pas au débit mesuré pendant le seul palier final.

### 4.2. Analyse des connexions refusées

Les échecs des scénarios constants à 250 et 1 000 VUs ont été accompagnés du message k6 :

```text
dial tcp 127.0.0.1:8080: connectex:
No connection could be made because the target machine actively refused it.
HTTP status: 0
```

**Diagnostic établi :** les requêtes concernées ont échoué à l'établissement de la connexion TCP vers l'API.

Il ne s'agit pas de réponses HTTP 500 produites par le contrôleur.

Ces refus apparaissent au début des scénarios à démarrage immédiat.

Le test du port 8080 par PowerShell a confirmé sa disponibilité à un autre instant, mais ne permet pas d'expliquer à lui seul ces refus.

**Enseignement opérationnel :** le mode d'application de la charge influe sur les résultats.

Une montée progressive à 250 VUs a été stable, contrairement au démarrage immédiat de 250 VUs.

La cause technique exacte des refus n'est pas démontrée par les données collectées. Aucune attribution arbitraire à Spring Boot, PostgreSQL ou Windows n'est retenue.

Le **seuil maximal de saturation n'a pas été déterminé**.

Le résultat démontré est la conformité du scénario progressif jusqu'à 250 VUs et la non-conformité du scénario constant à 1 000 VUs, dans cet environnement.

### 4.3. Observations Spring Boot, Hibernate et PostgreSQL

Les logs Hibernate contiennent des requêtes répétées :

```sql
select ... from files where download_token=?
```

Elles correspondent à la recherche du fichier pour chaque génération de lien.

Ce comportement est cohérent avec le grand nombre d'appels k6.

Aucune mesure de plan d'exécution SQL ni de saturation du pool n'a été fournie : ces lignes ne permettent donc pas d'identifier PostgreSQL comme cause des refus TCP.

Le traitement planifié des fichiers expirés a également été observé :

```text
Expired files cleanup: found=3, deleted=3, failed=0
Expired files deleted: 3
```

Lors du passage suivant, les logs ont indiqué :

```text
Expired files cleanup: found=0, deleted=0, failed=0
Expired files deleted: 0
```

Ces résultats confirment que le service a déclaré ces opérations sans erreur.

Ils ne constituent pas une mesure de performance de l'endpoint de téléchargement.

### 4.4. Avertissements HikariCP

Deux avertissements figurent dans les logs :

```text
Failed to validate connection
(This connection has been closed.)
```

```text
Thread starvation or clock leap detected
(housekeeper delta=6m24s...)
```

Le premier indique qu'une connexion PostgreSQL présente dans le pool n'était plus utilisable.

Le second signale un retard du thread de maintenance.

Les extraits ne montrent pas que ces événements ont causé les échecs k6.

Aucun réglage arbitraire de `maxLifetime` ou de la taille du pool n'a été appliqué sur cette seule base.

## 5. Observabilité et protection des journaux

La campagne a été documentée à partir :

- Des métriques Grafana k6.
- Des exports JSON des résultats.
- Des logs Spring Boot.
- Des requêtes Hibernate.
- Des avertissements HikariCP.

Les journaux observés sont au format texte.

L'utilisation de journaux ECS/JSON n'est pas revendiquée dans ce bilan.

Les données disponibles ont permis de distinguer les réponses applicatives réussies des erreurs réseau, de relever les requêtes SQL et d'observer le job de nettoyage.

### Protection des informations sensibles

Les logs k6 en cas d'échec réseau incluent parfois le token dans l'URL.

Les sorties brutes ne doivent donc pas être publiées sans masquage.

Aucun secret AWS, JWT, mot de passe ou lien présigné complet ne doit être ajouté au dépôt.

## 6. Synthèse et décision

| Point évalué | Résultat |
|---|---|
| Réponse fonctionnelle de l'endpoint | Validée avec 1 requête de diagnostic |
| Charge progressive jusqu'à 50 VUs | Validée, 0 % d'erreur |
| Charge constante à 100 VUs | Validée, 0 % d'erreur |
| Charge constante à 250 VUs | Seuils respectés, 120 refus de connexion |
| Charge constante à 1 000 VUs | Non validée, 751 échecs (1,26 %) |
| Charge progressive jusqu'à 250 VUs | Validée, 37 274 requêtes, 0 % d'erreur |
| Comportement du backend dans les logs | Requêtes Hibernate, job planifié et avertissements HikariCP analysés |

### Conclusion

Sur le poste Windows utilisé, DataShare a satisfait les objectifs fixés au cours d'une montée progressive jusqu'à **250 utilisateurs virtuels**, avec un **p95 global de 22,52 ms** et **aucun échec sur 37 274 requêtes**.

Le test à **1 000 utilisateurs virtuels lancés immédiatement** a échoué au critère de fiabilité avec **751 requêtes échouées**, soit **1,26 %**.

Le mode de démarrage de la charge constitue donc un facteur observable de variation, sans preuve d'une cause unique des refus.

La campagne de performance est **clôturée** avec ses réussites et ses anomalies.

Ce bilan ne revendique ni transfert réel de fichiers S3, ni capacité maximale de production, ni seuil de saturation exact : ces éléments sont extérieurs au périmètre des mesures effectuées.

## 7. Pièces de preuve

Les scripts de reproduction et les exports k6 se trouvent dans le dossier `performance/` et son sous-dossier `results/`.

La preuve principale de la première montée est :

`performance/results/download-ramp-summary.json`

Les autres campagnes ont été constatées dans les sorties console k6, avec option `--summary-export` vers :

- `performance/results/load-100.json`
- `performance/results/load-250.json`
- `performance/results/load-ramp-250.json`

La campagne à 1 000 VUs a initialement exporté vers `load-75.json` en raison d'une erreur de nommage du fichier. Ce nom ne correspond pas à la charge réellement exécutée.

Les résultats de ce document proviennent des exécutions rapportées. Aucun résultat supplémentaire n'a été extrapolé.
