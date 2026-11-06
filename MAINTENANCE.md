# MAINTENANCE.md — Guide de maintenance

**Projet :** DataShare  
**Frontend :** Angular 21  
**Backend :** Spring Boot 4.1.1 — Java 21  
**Base de données :** PostgreSQL  
**Stockage :** AWS S3  
**Date :** 6 novembre 2026

---

## 1. Objectif

Ce document décrit les procédures nécessaires à la maintenance de DataShare.

Il a pour objectif de faciliter :

- Le démarrage et l'arrêt des services.
- La vérification du bon fonctionnement de l'application.
- L'identification et la résolution des anomalies.
- La mise à jour des dépendances.
- L'exécution des tests automatisés.
- La consultation des logs.
- La sauvegarde et la restauration des données.
- La prévention des régressions.

## 2. Architecture technique

DataShare repose sur une architecture frontend/backend.

| Composant | Technologie | Responsabilité |
|---|---|---|
| Frontend | Angular 21 | Interface utilisateur |
| Backend | Spring Boot 4.1.1 | API REST et logique métier |
| Base de données | PostgreSQL | Métadonnées des utilisateurs et fichiers |
| Stockage | AWS S3 | Conservation des fichiers |
| Authentification | JWT | Gestion des accès |
| Build backend | Maven | Compilation et dépendances |
| Tests backend | JUnit, Mockito, JaCoCo | Validation et couverture |
| Tests frontend | Jest, Cypress | Validation des composants et parcours |

## 3. Prérequis

Les outils suivants sont nécessaires pour le développement et la maintenance :

- Java 21.
- Node.js compatible avec Angular 21.
- npm.
- Docker Desktop.
- Git.
- Maven Wrapper fourni avec le backend.
- Accès à PostgreSQL.
- Configuration des accès AWS S3.

Les variables d'environnement nécessaires doivent être configurées avant le démarrage.

Les secrets ne doivent pas être enregistrés dans le dépôt Git.

## 4. Démarrage des services

### 4.1. Base de données

Si PostgreSQL est exécuté avec Docker Compose :

```powershell
docker compose up -d
```

Cette commande doit être lancée depuis le dossier contenant le fichier Compose approprié.

Pour vérifier les conteneurs :

```powershell
docker ps
```

### 4.2. Backend

Dans le dossier `datashare-backend` :

```powershell
./mvnw spring-boot:run
```

Vérifier dans les logs que l'application démarre sans erreur.

### 4.3. Frontend

Dans le dossier `datashare-frontend` :

```powershell
npm install
npm start
```

Le frontend est généralement accessible à l'adresse :

```text
http://localhost:4200
```

Les ports peuvent varier selon la configuration du projet.

## 5. Vérifications après démarrage

Après le démarrage, contrôler :

1. L'accessibilité de l'interface Angular.
2. Le fonctionnement de l'inscription et de la connexion.
3. L'accès aux endpoints du backend.
4. La connexion à PostgreSQL.
5. Le téléversement d'un fichier de test.
6. La génération d'un lien de partage.
7. Le téléchargement du fichier.
8. La consultation de l'historique.
9. La suppression du fichier de test.

Cette vérification constitue un test de bon fonctionnement après démarrage.

## 6. Maintenance des dépendances

### 6.1. Frontend

Vérifier les versions installées :

```powershell
ng version
```

Analyser les dépendances :

```powershell
npm audit
npm audit --omit=dev
```

Appliquer les corrections compatibles :

```powershell
npm audit fix
```

Toute mise à jour importante doit être réalisée dans une branche dédiée et suivie d'une exécution des tests.

Les changements majeurs ne doivent pas être appliqués automatiquement sans vérifier leur compatibilité.

### 6.2. Backend

Vérifier la version Java et Maven :

```powershell
java -version
./mvnw -version
```

Compiler et tester :

```powershell
./mvnw clean verify
```

Les mises à jour du backend doivent tenir compte de la compatibilité entre Spring Boot, Spring Security, les bibliothèques JWT et le SDK AWS.

## 7. Procédure de correction d'une anomalie

Lorsqu'une anomalie est détectée, appliquer les étapes suivantes.

### Étape 1 — Reproduire l'erreur

Identifier les conditions nécessaires à son apparition :

- Fonctionnalité concernée.
- Actions effectuées.
- Résultat attendu.
- Résultat obtenu.
- Message d'erreur.
- Environnement utilisé.

### Étape 2 — Consulter les logs

Pour le frontend, examiner la console du navigateur et les requêtes réseau.

Pour le backend, consulter les logs Spring Boot.

Si l'application est lancée dans Docker :

```powershell
docker logs NOM_DU_CONTENEUR
```

### Étape 3 — Identifier la cause

Déterminer si l'anomalie provient :

- De l'interface Angular.
- D'une requête HTTP incorrecte.
- D'une règle métier backend.
- De Spring Security.
- De PostgreSQL.
- D'AWS S3.
- D'une configuration ou d'une dépendance.

### Étape 4 — Corriger le problème

Créer une branche Git dédiée si nécessaire.

Effectuer une modification ciblée, sans introduire de changements indépendants de l'anomalie.

### Étape 5 — Vérifier la correction

Relancer les tests de la partie modifiée ainsi que les tests susceptibles d'être affectés.

### Étape 6 — Documenter

Enregistrer :

- La cause identifiée.
- La correction apportée.
- Les tests effectués.
- Le résultat obtenu.
- Le commit correspondant.

## 8. Tests de non-régression

### 8.1. Backend

```powershell
./mvnw clean verify
```

Résultats de référence du 6 novembre 2026 :

- 66 tests réussis.
- Couverture des instructions : 91 %.
- Couverture des branches : 80 %.

### 8.2. Frontend

```powershell
npm run build
npx jest --coverage --runInBand
npx cypress run --browser chrome
```

Résultats de référence :

- 90 tests Jest réussis.
- 23 tests Cypress réussis.
- Couverture des instructions : 90,64 %.
- Couverture des branches : 76,36 %.

Les résultats de référence doivent être comparés aux nouvelles exécutions pour détecter les régressions.

## 9. Maintenance de PostgreSQL

Les principales opérations comprennent :

- Vérifier que le service est disponible.
- Surveiller les erreurs de connexion.
- Contrôler l'espace disque.
- Vérifier la cohérence des données.
- Effectuer des sauvegardes régulières.

### Sauvegarde

La commande PostgreSQL `pg_dump` peut être utilisée :

```powershell
pg_dump -U UTILISATEUR -d NOM_BASE -F c -f sauvegarde.dump
```

La commande doit être exécutée dans un environnement disposant des outils PostgreSQL et d'un accès autorisé à la base.

### Restauration

```powershell
pg_restore -U UTILISATEUR -d NOM_BASE -c sauvegarde.dump
```

**Attention :** l'option `-c` supprime les objets existants avant restauration. Elle doit être utilisée uniquement sur la base cible prévue et après vérification de la sauvegarde.

Une restauration doit d'abord être testée dans un environnement isolé.

Les procédures ci-dessus sont des commandes de référence : leur exécution réelle n'a pas été confirmée.

## 10. Maintenance du stockage AWS S3

La maintenance du stockage comprend :

- La vérification des permissions IAM.
- Le contrôle de l'accès public aux objets.
- La surveillance des erreurs de téléversement.
- La vérification des liens présignés.
- La gestion des fichiers expirés.
- La surveillance de l'utilisation de l'espace de stockage.

Les suppressions de fichiers doivent être cohérentes entre les métadonnées stockées dans PostgreSQL et les objets conservés dans S3.

Les sauvegardes PostgreSQL ne contiennent pas les fichiers stockés dans S3. Une stratégie de protection des objets doit donc être définie séparément.

Les opérations de maintenance ne doivent pas exposer les fichiers des utilisateurs ni leurs informations sensibles.

## 11. Gestion des logs

Les logs permettent d'identifier les événements importants de l'application.

Lorsqu'ils sont configurés en JSON structuré, ils facilitent leur traitement automatisé.

Les informations utiles comprennent :

- L'horodatage.
- Le niveau de log.
- Le composant concerné.
- La description de l'événement.
- Les détails techniques d'une éventuelle erreur.

Les mots de passe, tokens JWT et clés d'accès ne doivent pas apparaître dans les journaux.

Les logs doivent être surveillés après les déploiements et pendant les opérations de maintenance.

## 12. Gestion des versions avec Git

Les modifications sont réalisées dans des branches dédiées.

Procédure recommandée :

1. Mettre à jour la branche de référence.
2. Créer une branche pour la correction ou l'évolution.
3. Effectuer les modifications.
4. Exécuter les tests.
5. Vérifier les fichiers modifiés.
6. Enregistrer le commit.
7. Soumettre les modifications pour intégration.
8. Fusionner après validation.

Cette organisation permet de conserver un historique des corrections et de limiter les risques de régression.

## 13. Procédure de retour arrière

En cas de dysfonctionnement important après une mise à jour :

1. Identifier la version précédemment fonctionnelle.
2. Vérifier les modifications apportées à la base de données.
3. Sauvegarder les données actuelles si nécessaire.
4. Restaurer ou redéployer la version précédente de l'application.
5. Appliquer les procédures de restauration de données uniquement lorsque cela est indispensable.
6. Vérifier le fonctionnement des parcours critiques.

Le retour arrière du code ne restaure pas automatiquement les données PostgreSQL ni les objets S3.

La stratégie exacte dépend de l'environnement de déploiement et doit être validée avant utilisation en production.

## 14. Plan de maintenance préventive

| Fréquence proposée | Opération |
|---|---|
| Après chaque modification | Compilation et tests concernés |
| Avant chaque fusion | Exécution des tests de non-régression |
| Avant chaque déploiement | Vérification de la configuration et des secrets |
| Chaque semaine | Consultation des erreurs applicatives |
| Chaque mois | Vérification des dépendances et audits de sécurité |
| Régulièrement | Sauvegarde et contrôle de PostgreSQL |
| Après un incident | Analyse, correction, tests et documentation |

Ces fréquences constituent un plan de maintenance proposé et non un calendrier d'opérations déjà exécutées.

## 15. Conclusion

La maintenance de DataShare repose sur une combinaison de contrôles techniques, de tests automatisés et de procédures documentées.

L'utilisation de Git permet de suivre les évolutions du code, tandis que Maven, Jest et Cypress facilitent la validation des corrections.

La surveillance des logs, la gestion des dépendances, les sauvegardes PostgreSQL et la protection des fichiers S3 constituent les principales opérations de maintenance.

Ce guide fournit une base pour assurer la stabilité, la sécurité et l'évolution de DataShare tout en limitant les risques de régression.