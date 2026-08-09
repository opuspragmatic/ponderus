-- "user" est un mot reserve en PostgreSQL : la table s'appelle "users".
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    firebase_uid  VARCHAR(128) NOT NULL UNIQUE,
    plan          VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE projects (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    elim_threshold  INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE options (
    id          UUID PRIMARY KEY,
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    position    INTEGER NOT NULL
);

CREATE TABLE criteria (
    id          UUID PRIMARY KEY,
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    label       VARCHAR(255) NOT NULL,
    weight      INTEGER NOT NULL,
    elim        BOOLEAN NOT NULL DEFAULT false,
    position    INTEGER NOT NULL
);

-- Table pivot : c'est elle qui permet de passer de 2 a N options.
-- value est nullable pour distinguer "pas encore note" de "note a 0".
CREATE TABLE scores (
    id            UUID PRIMARY KEY,
    criterion_id  UUID NOT NULL REFERENCES criteria(id) ON DELETE CASCADE,
    option_id     UUID NOT NULL REFERENCES options(id) ON DELETE CASCADE,
    value         NUMERIC(3,1),
    note          TEXT,
    UNIQUE (criterion_id, option_id)
);

CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_options_project_id ON options(project_id);
CREATE INDEX idx_criteria_project_id ON criteria(project_id);
CREATE INDEX idx_scores_criterion_id ON scores(criterion_id);
CREATE INDEX idx_scores_option_id ON scores(option_id);
