CREATE TABLE venues (
                        id BIGSERIAL PRIMARY KEY,
                        code VARCHAR(255) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        city VARCHAR(255) NOT NULL,
                        address VARCHAR(255) NOT NULL,
                        capacity INTEGER NOT NULL,
                        active BOOLEAN NOT NULL,

                        CONSTRAINT chk_venue_capacity
                            CHECK (capacity > 0)
);

CREATE TABLE events (
                        id BIGSERIAL PRIMARY KEY,
                        event_code VARCHAR(255) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        description VARCHAR(1000),
                        category VARCHAR(255) NOT NULL,
                        status VARCHAR(255) NOT NULL,
                        event_date TIMESTAMP NOT NULL,
                        minimum_age INTEGER,
                        venue_id BIGINT NOT NULL,

                        CONSTRAINT fk_event_venue
                            FOREIGN KEY (venue_id)
                                REFERENCES venues(id),

                        CONSTRAINT chk_event_category
                            CHECK (
                                category IN (
                                             'MUSIC',
                                             'SPORTS',
                                             'TECHNOLOGY',
                                             'EDUCATION',
                                             'CULTURE',
                                             'ENTERTAINMENT'
                                    )
                                ),

                        CONSTRAINT chk_event_status
                            CHECK (
                                status IN (
                                           'DRAFT',
                                           'PUBLISHED',
                                           'SOLD_OUT',
                                           'CANCELLED',
                                           'FINISHED'
                                    )
                                )
);

CREATE TABLE artists (
                         id BIGSERIAL PRIMARY KEY,
                         stage_name VARCHAR(255) NOT NULL UNIQUE,
                         country VARCHAR(255),
                         genre VARCHAR(255),
                         active BOOLEAN NOT NULL
);

CREATE TABLE event_artists (
                               event_id BIGINT NOT NULL,
                               artist_id BIGINT NOT NULL,

                               CONSTRAINT pk_event_artists
                                   PRIMARY KEY (event_id, artist_id),

                               CONSTRAINT fk_event_artists_event
                                   FOREIGN KEY (event_id)
                                       REFERENCES events(id),

                               CONSTRAINT fk_event_artists_artist
                                   FOREIGN KEY (artist_id)
                                       REFERENCES artists(id)
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       active BOOLEAN NOT NULL
);

CREATE TABLE user_profiles (
                               id BIGSERIAL PRIMARY KEY,
                               first_name VARCHAR(255) NOT NULL,
                               last_name VARCHAR(255) NOT NULL,
                               phone VARCHAR(255),
                               city VARCHAR(255),
                               birth_date DATE,
                               user_id BIGINT NOT NULL UNIQUE,

                               CONSTRAINT fk_user_profile_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
);

CREATE TABLE tickets (
                         id BIGSERIAL PRIMARY KEY,
                         ticket_code VARCHAR(255) NOT NULL UNIQUE,
                         type VARCHAR(255) NOT NULL,
                         price NUMERIC(12, 2) NOT NULL,
                         status VARCHAR(255) NOT NULL,
                         purchase_date TIMESTAMP NOT NULL,
                         user_id BIGINT NOT NULL,
                         event_id BIGINT NOT NULL,

                         CONSTRAINT fk_ticket_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(id),

                         CONSTRAINT fk_ticket_event
                             FOREIGN KEY (event_id)
                                 REFERENCES events(id),

                         CONSTRAINT chk_ticket_price
                             CHECK (price >= 0),

                         CONSTRAINT chk_ticket_type
                             CHECK (
                                 type IN (
                                          'GENERAL',
                                          'VIP',
                                          'BACKSTAGE',
                                          'STUDENT'
                                     )
                                 ),

                         CONSTRAINT chk_ticket_status
                             CHECK (
                                 status IN (
                                            'RESERVED',
                                            'PAID',
                                            'CANCELLED',
                                            'USED'
                                     )
                                 )
);

CREATE INDEX idx_events_venue
    ON events(venue_id);

CREATE INDEX idx_events_status_date
    ON events(status, event_date);

CREATE INDEX idx_event_artists_artist
    ON event_artists(artist_id);

CREATE INDEX idx_tickets_user
    ON tickets(user_id);

CREATE INDEX idx_tickets_event
    ON tickets(event_id);

CREATE INDEX idx_tickets_event_status
    ON tickets(event_id, status);