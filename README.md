# kc-pg-provider

**Keycloak User Storage SPI provider backed by PostgreSQL.**

Lets Keycloak serve users from your own Postgres schema: lookup, registration, credential
validation and user queries run against a custom `users` table instead of Keycloak's
built-in store. Useful when an existing user database must stay the source of truth — or when
user data must live in a schema you control.

Built as a reference implementation while designing enterprise identity integration (AD/LDAP,
federated stores) for [Atrium](https://atrium-ai.ru), an on-premise LLM platform.

## How it works

Keycloak discovers the provider via standard SPI registration
(`META-INF/services/org.keycloak.storage.UserStorageProviderFactory`).
`KcUserStorageProvider` implements:

| Interface | Responsibility |
|---|---|
| `UserLookupProvider` | find users by id, username, email |
| `UserRegistrationProvider` | add / remove users in the external store |
| `UserQueryProvider` | search users (name, email, search patterns) |
| `CredentialInputValidator` / `CredentialInputUpdater` | password validation and updates against the external store |
| `OnUserCache` | participates in Keycloak's user cache invalidation |

Users are exposed to Keycloak through `UserAdapter` over the JPA entity `KcUserEntity`.
The provider talks to its own JPA connection (`user-store`) pointing at Postgres — it never
touches Keycloak's internal database.

```
Keycloak ──SPI──▶ KcUserStorageProvider ──JPA (user-store)──▶ Postgres: users
```

## Quickstart

```bash
# build the provider jar, build the Keycloak image, start it
./sh/run
```

or step by step:

```bash
mvn clean install -DskipTests
docker build -f docker/Dockerfile -t kc-pg-provider .
docker run -p 8443:8443 kc-pg-provider start-dev
```

Create the table in Postgres (see `sql/script.sql`):

```sql
create table if not exists users(
  id       uuid not null primary key,
  username varchar(255),
  password varchar(255)
);
```

Then in the Keycloak admin console: **Realm settings → User federation → Add provider →
kc-pg-provider**. The provider appears in the user-federation list (see
`docs/images/providers.png`).

## Layout

- `src/main/java/io/shvyrev/providers` — provider and its factory
- `src/main/java/io/shvyrev/model` — JPA entity and Keycloak `UserModel` adapter
- `sql/script.sql` — minimal schema
- `docker/Dockerfile` — Keycloak image with the provider baked in
- `sh/run`, `sh/pg` — build/run helper scripts

## Requirements

Git · Java 17+ · Maven 3.8+ · Docker
