CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description VARCHAR(1000),
                       status VARCHAR(20) NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       due_date DATE,
                       user_id BIGINT NOT NULL,
                       CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id)
);
