# PERF.md — Tests de performance et observabilité

**Projet :** DataShare  
**Backend :** Spring Boot 4.1.1 — Java 21  
**Frontend :** Angular 21  
**Outil de test :** Grafana k6  
**Date :** 6 novembre 2026

---

## 1. Objectif

L'objectif est d'évaluer les performances d'un endpoint critique du backend DataShare à l'aide de Grafana k6.

Les indicateurs étudiés sont :

- Le temps de réponse moyen.
- Le temps de réponse au 95e percentile (p95).
- Le taux d'erreur HTTP.
- Le nombre de requêtes exécutées.
- Le débit en requêtes par seconde.
- Le nombre d'utilisateurs virtuels.
- Le taux de réussite des vérifications fonctionnelles.

L'étude comprend également une démarche de mise en place de logs structurés pour faciliter l'analyse des événements applicatifs.

## 2. Environnement de test

| Élément | Configuration |
|---|---|
| Application | DataShare |
| Backend | Spring Boot 4.1.1 |
| Java | 21 |
| Base de données | PostgreSQL |
| Stockage | AWS S3 |
| Outil | Grafana k6 |
| Environnement | Local — Windows |
| Adresse backend | `http://localhost:8080` |

Les résultats obtenus dans cet environnement ne permettent pas de déduire directement les performances de l'application en production.

## 3. Endpoint testé

Le test porte sur l'endpoint de génération du lien de téléchargement d'un fichier partagé.

**Endpoint :**

```http
POST /api/files/download/{token}
```

**Corps de la requête :**

```json
{
  "password": null
}
```

**Résultat attendu :**

- Une réponse HTTP 200.
- Un objet JSON contenant une URL de téléchargement.

Le scénario mesure le traitement de la requête par le backend. Il ne mesure pas le transfert du fichier depuis AWS S3 vers l'utilisateur.

## 4. Configuration du test k6

Le script est enregistré dans :

```text
performance/download-load.js
```

Le test de diagnostic est exécuté avec un utilisateur virtuel et une itération.

**Commande utilisée :**

```powershell
k6 run --vus 1 --iterations 1 .\performance\download-load.js
```

Les seuils définis dans le script sont :

| Indicateur | Objectif |
|---|---|
| Temps de réponse p95 | Inférieur à 1 000 ms |
| Taux d'erreur HTTP | Inférieur à 1 % |
| Vérifications réussies | Supérieures à 99 % |

Ces seuils sont des objectifs de contrôle et non des garanties de performance.

## 5. Résultats du test

### 5.1. Résultats obtenus

| Indicateur | Résultat |
|---|---:|
| Utilisateurs virtuels | 1 |
| Nombre de requêtes HTTP | 1 |
| Code de réponse | HTTP 200 |
| Temps de réponse moyen | 46,82 ms |
| Temps de réponse p95 | 46,82 ms |
| Taux d'erreur HTTP | 0 % |
| Vérifications réussies | 100 % (2/2) |
| Durée totale de l'itération | 1,06 s |

Les deux vérifications fonctionnelles ont réussi :

1. Le serveur renvoie une réponse HTTP 200.
2. La réponse contient une propriété `downloadUrl`.

### 5.2. Analyse des résultats

Le test confirme que l'endpoint est accessible et qu'il retourne la réponse attendue dans les conditions de l'exécution.

Le temps de réponse mesuré est de **46,82 ms**, inférieur à l'objectif de 1 000 ms fixé pour le p95.

Le taux d'erreur HTTP est de **0 %**, et les vérifications fonctionnelles ont toutes réussi.

Cependant, le test ne contient qu'une seule requête. Le p95 est donc égal au temps de cette requête et ne constitue pas une mesure représentative des performances sous charge.

Le débit calculé sur cette exécution est d'environ 0,94 requête par seconde, mais cette valeur dépend fortement de l'unique itération et de la pause incluse dans le scénario.

**Résultat : test de diagnostic réussi. Les performances sous charge restent à mesurer.**

## 6. Logs structurés et observabilité

### 6.1. Objectif

La journalisation permet de suivre les événements applicatifs et de faciliter l'identification des anomalies.

Les événements à surveiller concernent notamment :

- Les erreurs HTTP.
- Les exceptions du backend.
- Les problèmes de connexion à PostgreSQL.
- Les erreurs de communication avec AWS S3.
- Les erreurs d'authentification.
- Les problèmes liés aux liens de téléchargement.

### 6.2. Configuration des logs structurés

Spring Boot prend en charge la journalisation structurée au format JSON ECS.

La configuration suivante peut être ajoutée dans `application.properties` :

```properties
logging.structured.format.console=ecs
logging.level.root=INFO
logging.level.com.datashare.datashare_backend=INFO
```

Elle permet de produire des événements structurés contenant notamment l'horodatage, le niveau de gravité, le logger et le message.

**Statut : configuration proposée, activation effective à confirmer dans les logs d'exécution.**

### 6.3. Analyse des métriques et des logs

Les métriques k6 indiquent que la requête de diagnostic a reçu une réponse HTTP 200 en 46,82 ms, sans échec.

Une analyse complémentaire des logs du backend peut permettre de vérifier l'absence d'exception au moment de la requête et d'identifier les éventuels problèmes de communication avec les services externes.

Les logs doivent être consultés sur la même période que les tests afin de rapprocher les événements applicatifs des résultats k6.

Aucun extrait de logs structurés n'a encore été relevé pour cette campagne.

### 6.4. Protection des informations sensibles

Les logs ne doivent jamais exposer les mots de passe, les tokens JWT, les clés AWS ou les URL présignées complètes.

## 7. Limites et améliorations

Le test réalisé a permis de valider le fonctionnement de l'endpoint avec une requête isolée.

Il ne permet cependant pas d'évaluer :

- Les performances sous plusieurs utilisateurs simultanés.
- La capacité maximale du backend.
- Les performances d'un téléchargement réel depuis AWS S3.
- Le comportement lors d'une charge prolongée.
- Les performances en environnement de production.

Un test de charge supplémentaire utilisant plusieurs utilisateurs virtuels et un fichier de test valide permettrait de compléter cette évaluation.

## 8. Conclusion

Le test réalisé avec Grafana k6 sur l'endpoint `POST /api/files/download/{token}` a obtenu une réponse HTTP 200 en **46,82 ms**, avec un taux d'erreur nul et 100 % de vérifications réussies.

Ce résultat confirme le fonctionnement de l'endpoint pour une requête isolée et permet d'établir une première mesure de référence.

Toutefois, une seule requête ne suffit pas pour démontrer les performances du backend sous charge ni pour évaluer de manière représentative le p95.

La journalisation structurée et de nouvelles mesures avec plusieurs utilisateurs virtuels constituent les prochaines étapes pour compléter l'analyse des performances et de l'observabilité de DataShare.