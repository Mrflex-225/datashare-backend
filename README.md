# DataShare — Backend

API REST de **DataShare**, application de partage de fichiers. Le backend prend en charge l'authentification, les métadonnées et l'historique des fichiers, la génération des liens de téléchargement, ainsi que l'intégration avec **PostgreSQL** et **AWS S3**.

> Ce dépôt contient le **backend Spring Boot** et la documentation technique transversale. L'interface Angular est disponible dans un dépôt séparé.

## Fonctionnalités

- Inscription et connexion avec authentification JWT.
- Téléversement de fichiers vers le stockage S3.
- Génération de liens de partage, avec expiration et protection par mot de passe lorsque configurées.
- Consultation de l'historique et suppression de fichiers.
- Téléchargement par lien partagé (génération d'une URL présignée).

## Architecture et technologies

| Élément | Technologie |
| --- | --- |
| Backend | Spring Boot 4.1.1 |
| Langage | Java 21 |
| Build | Maven Wrapper (`mvnw`, `mvnw.cmd`) |
| Base de données | PostgreSQL 16 |
| Persistance | Spring Data JPA |
| Authentification | Spring Security, JWT |
| Stockage des fichiers | AWS S3 (`S3Client`, `S3Presigner`) |
| Base de données locale | Docker / Docker Compose, selon les fichiers du dépôt |
| Tests | JUnit 5, Mockito, MockMvc, JaCoCo |
| Performance | Grafana k6 |

**Flux simplifié :** Angular → API Spring Boot → PostgreSQL (métadonnées) et AWS S3 (contenu des fichiers).

## Prérequis

- **JDK 21** installé (`java -version`).
- **Docker Desktop avec Docker Compose** pour utiliser PostgreSQL en conteneur, si le `compose.yaml` / `docker-compose.yml` du projet le prévoit.
- Accès à un **bucket AWS S3 de test** et identifiants appropriés, ou une configuration de stockage compatible effectivement implémentée.
- Git pour cloner le dépôt.

## Installation

```bash
git clone https://github.com/Mrflex-225/datashare-backend.git
cd datashare-backend
```

### 1. Configurer l'environnement

Ne pas committer de secrets. Définir les variables correspondant **exactement** aux paramètres utilisés dans `src/main/resources/application.properties` (ou le fichier de configuration réellement employé).

| Paramètre à renseigner | Rôle |
| --- | --- |
| URL JDBC PostgreSQL | Adresse de la base (`jdbc:postgresql://...`) |
| Utilisateur et mot de passe PostgreSQL | Authentification à la base |
| Région AWS | Région du bucket |
| Nom du bucket S3 | Destination des fichiers |
| Identifiants AWS | Accès à S3 via la chaîne d'identifiants AWS |
| Secret JWT | Signature et validation des tokens |

**Exemple indicatif de variables Spring**, à adapter à la configuration réelle :

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/<NOM_BASE>
DB_USER=<UTILISATEUR>
DB_PASSWORD=<MOT_DE_PASSE>
JWT_SECRET=<SECRET_JWT_SECRET>
AWS_REGION=<REGION_AWS>
AWS_ACCESS_KEY_ID=<CLE_AWS_DE_TEST>
AWS_SECRET_ACCESS_KEY=<SECRET_AWS_DE_TEST>
```

**Attention :** un fichier `.env` n'est pas chargé automatiquement par Spring Boot. Exporter les variables dans le terminal ou utiliser le mécanisme de configuration effectivement présent dans le projet. Le nom de variable du bucket et celui du secret JWT doivent être vérifiés dans le code.

### 2. Démarrer PostgreSQL

Dans le dossier contenant le fichier Compose :

```bash
docker compose up -d
```

Vérifier que le conteneur est prêt avant de démarrer l'API. Si PostgreSQL est installé autrement, utiliser la configuration correspondante.

### 3. Démarrer le backend

**Windows PowerShell :**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS :**

```bash
./mvnw spring-boot:run
```

Par défaut, l'API est utilisée sur **http://localhost:8080**, sauf changement de `server.port`.

## Compilation et tests

**Windows PowerShell :**

```powershell
.\mvnw.cmd clean verify
```

**Linux / macOS :**

```bash
./mvnw clean verify
```

## Principaux endpoints

Ces routes sont documentées à partir des fonctionnalités travaillées dans le projet. **Vérifier le mapping exact dans les contrôleurs avant publication.**

| Fonction | Méthode / route |
| --- | --- |
| Inscription | `POST /api/register` |
| Téléversement de fichier | `POST /api/files` |
| Génération du lien de téléchargement | `POST /api/files/download/{token}` |
| Historique et suppression | Voir les mappings du contrôleur de fichiers |
| Connexion | Voir le contrôleur d'authentification |

Le téléversement utilise `multipart/form-data` et nécessite une authentification JWT. L'endpoint de téléchargement partagé accepte notamment un corps JSON contenant `password` si le lien est protégé.


## Test de performance

Le scénario k6 de génération de lien se trouve dans `performance/download-load.js` (selon l'emplacement retenu dans le dépôt).

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:DOWNLOAD_TOKEN = "<TOKEN_DE_TEST_VALIDE>"
k6 run --vus 1 --iterations 1 .\performance\download-load.js
```

## Documentation qualité et maintenance

| Fichier | Contenu |
| --- | --- |
| [`TESTING.md`](TESTING.md) | Stratégie et résultats des tests |
| [`SECURITY.md`](SECURITY.md) | Sécurité, audits et limites |
| [`PERF.md`](PERF.md) | Tests k6 et observabilité |
| [`MAINTENANCE.md`](MAINTENANCE.md) | Mise à jour, sauvegarde et restauration |

**Dépôt frontend :** `https://github.com/Mrflex-225/datashare-frontend.git`
