package com.pulsepass;

import com.pulsepass.domain.*;
import com.pulsepass.repository.*;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    // ---------------------------------------------------------
    // Flyway V1, V2 y V3
    // ---------------------------------------------------------

    @Test
    void shouldApplyAllFlywayMigrations() {

        Integer migrations = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = true
                """,
                Integer.class
        );

        assertThat(migrations).isEqualTo(3);

        List<Artist> artists = artistRepository.findAll();

        assertThat(artists)
                .extracting(Artist::getStageName)
                .contains(
                        "Solar Beat",
                        "Neon Waves",
                        "Caribbean Sound",
                        "Ocean Drive",
                        "Digital Pulse"
                );

        Integer streamingColumn = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'events'
                  AND column_name = 'streaming_url'
                """,
                Integer.class
        );

        assertThat(streamingColumn).isEqualTo(1);
    }


    // ---------------------------------------------------------
    // Venue 1:N Event
    // Query Methods de Event
    // ---------------------------------------------------------

    @Test
    void shouldPersistVenueWithEventsAndResolveQueryMethods() {

        Venue venue = new Venue(
                "VEN-SMR-01",
                "Marina Convention Center",
                "Santa Marta",
                "Santa Marta",
                5000,
                true
        );

        venueRepository.save(venue);

        Event firstEvent = new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical del Caribe",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 10, 19, 0),
                18,
                venue
        );

        Event secondEvent = new Event(
                "TECH-2027",
                "Tech Caribbean 2027",
                "Evento de tecnologia",
                EventCategory.TECHNOLOGY,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2027, 2, 20, 9, 0),
                16,
                venue
        );

        eventRepository.saveAll(
                List.of(firstEvent, secondEvent)
        );

        eventRepository.flush();

        assertThat(
                eventRepository.findByEventCode("CMF-2026")
        ).isPresent();

        List<Event> venueEvents =
                eventRepository.findByVenueCode("VEN-SMR-01");

        assertThat(venueEvents).hasSize(2);

        List<Event> published =
                eventRepository.findByStatusOrderByEventDateAsc(
                        EventStatus.PUBLISHED
                );

        assertThat(published)
                .extracting(Event::getEventCode)
                .containsSubsequence(
                        "CMF-2026",
                        "TECH-2027"
                );
    }


    // ---------------------------------------------------------
    // Event N:M Artist
    // JPQL por artista, ciudad y recomendados
    // ---------------------------------------------------------

    @Test
    void shouldPersistEventArtistsAndResolveJpqlQueries() {

        Venue venue = new Venue(
                "VEN-ART-01",
                "Pulse Arena",
                "Santa Marta",
                "Carrera 1",
                4000,
                true
        );

        venueRepository.save(venue);

        Artist solarBeat = artistRepository
                .findByStageName("Solar Beat")
                .orElseThrow();

        Artist neonWaves = artistRepository
                .findByStageName("Neon Waves")
                .orElseThrow();

        Event event = new Event(
                "ART-2027",
                "Pulse Music Night",
                "Evento con artistas invitados",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2027, 3, 15, 20, 0),
                18,
                venue
        );

        event.addArtist(solarBeat);
        event.addArtist(neonWaves);

        eventRepository.saveAndFlush(event);

        assertThat(event.getArtists())
                .extracting(Artist::getStageName)
                .containsExactlyInAnyOrder(
                        "Solar Beat",
                        "Neon Waves"
                );

        List<Event> byArtist =
                eventRepository.findByArtistStageName(
                        "Solar Beat"
                );

        assertThat(byArtist)
                .extracting(Event::getEventCode)
                .contains("ART-2027");

        List<Event> byCityAndArtist =
                eventRepository.findByCityAndArtist(
                        "Santa Marta",
                        "Solar Beat"
                );

        assertThat(byCityAndArtist)
                .extracting(Event::getEventCode)
                .contains("ART-2027");

        List<Event> recommended =
                eventRepository.findRecommendedEvents(
                        LocalDateTime.of(2027, 1, 1, 0, 0),
                        "Santa Marta",
                        "Solar"
                );

        assertThat(recommended)
                .extracting(Event::getEventCode)
                .contains("ART-2027");
    }


    // ---------------------------------------------------------
    // User 1:1 UserProfile
    // ---------------------------------------------------------

    @Test
    void shouldPersistUserWithOneProfile() {

        User user = new User(
                "andrea",
                "andrea@pulsepass.com",
                true
        );

        userRepository.save(user);

        UserProfile profile = new UserProfile(
                "Andrea",
                "Martinez",
                "3001234567",
                "Santa Marta",
                LocalDate.of(2000, 5, 10)
        );

        user.assignProfile(profile);

        userProfileRepository.saveAndFlush(profile);

        assertThat(profile.getId()).isNotNull();

        assertThat(
                userProfileRepository.findByUserId(
                        user.getId()
                )
        ).isPresent();

        assertThat(profile.getUser())
                .isSameAs(user);

        assertThat(user.getProfile())
                .isSameAs(profile);
    }


    // ---------------------------------------------------------
    // Ticket -> User
    // Ticket -> Event
    // Consultas de tickets y COUNT
    // ---------------------------------------------------------

    @Test
    void shouldPersistTicketsAndResolveTicketQueries() {

        Venue venue = new Venue(
                "VEN-TKT-01",
                "Marina Convention Center",
                "Santa Marta",
                "Avenida del Libertador",
                5000,
                true
        );

        venueRepository.save(venue);

        Event event = new Event(
                "CMF-TKT-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 19, 0),
                18,
                venue
        );

        eventRepository.save(event);

        User andrea = new User(
                "andrea.ticket",
                "andrea.ticket@pulsepass.com",
                true
        );

        User carlos = new User(
                "carlos.ticket",
                "carlos.ticket@pulsepass.com",
                true
        );

        User laura = new User(
                "laura.ticket",
                "laura.ticket@pulsepass.com",
                true
        );

        User miguel = new User(
                "miguel.ticket",
                "miguel.ticket@pulsepass.com",
                true
        );

        userRepository.saveAll(
                List.of(
                        andrea,
                        carlos,
                        laura,
                        miguel
                )
        );

        Ticket andreaTicket = new Ticket(
                "TKT-001",
                TicketType.VIP,
                new BigDecimal("250000.00"),
                TicketStatus.PAID,
                LocalDateTime.of(2026, 9, 1, 10, 0),
                andrea,
                event
        );

        Ticket carlosTicket = new Ticket(
                "TKT-002",
                TicketType.GENERAL,
                new BigDecimal("120000.00"),
                TicketStatus.PAID,
                LocalDateTime.of(2026, 9, 2, 11, 0),
                carlos,
                event
        );

        Ticket lauraTicket = new Ticket(
                "TKT-003",
                TicketType.GENERAL,
                new BigDecimal("120000.00"),
                TicketStatus.RESERVED,
                LocalDateTime.of(2026, 9, 3, 12, 0),
                laura,
                event
        );

        Ticket miguelTicket = new Ticket(
                "TKT-004",
                TicketType.VIP,
                new BigDecimal("250000.00"),
                TicketStatus.CANCELLED,
                LocalDateTime.of(2026, 9, 4, 13, 0),
                miguel,
                event
        );

        ticketRepository.saveAll(
                List.of(
                        andreaTicket,
                        carlosTicket,
                        lauraTicket,
                        miguelTicket
                )
        );

        ticketRepository.flush();

        List<Ticket> andreaPaidTickets =
                ticketRepository
                        .findByUserEmailIgnoreCaseAndStatus(
                                "ANDREA.TICKET@PULSEPASS.COM",
                                TicketStatus.PAID
                        );

        assertThat(andreaPaidTickets)
                .extracting(Ticket::getTicketCode)
                .containsExactly("TKT-001");

        List<Ticket> paidEventTickets =
                ticketRepository
                        .findByEventEventCodeAndStatus(
                                "CMF-TKT-2026",
                                TicketStatus.PAID
                        );

        assertThat(paidEventTickets)
                .extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder(
                        "TKT-001",
                        "TKT-002"
                );

        long paidCount =
                ticketRepository
                        .countPaidTicketsByEventCode(
                                "CMF-TKT-2026"
                        );

        assertThat(paidCount).isEqualTo(2);

        assertThat(andreaTicket.getUser())
                .isEqualTo(andrea);

        assertThat(andreaTicket.getEvent())
                .isEqualTo(event);
    }


    // ---------------------------------------------------------
    // Constraint UNIQUE real en PostgreSQL
    // ---------------------------------------------------------

    @Test
    void shouldRejectDuplicatedTicketCode() {

        Venue venue = new Venue(
                "VEN-UNIQUE",
                "Unique Arena",
                "Santa Marta",
                "Calle 10",
                1000,
                true
        );

        venueRepository.save(venue);

        Event event = new Event(
                "EVT-UNIQUE",
                "Unique Event",
                "Evento para validar constraint",
                EventCategory.CULTURE,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2027, 5, 1, 18, 0),
                0,
                venue
        );

        eventRepository.save(event);

        User firstUser = new User(
                "unique.user1",
                "unique1@pulsepass.com",
                true
        );

        User secondUser = new User(
                "unique.user2",
                "unique2@pulsepass.com",
                true
        );

        userRepository.saveAll(
                List.of(firstUser, secondUser)
        );

        Ticket firstTicket = new Ticket(
                "TKT-DUPLICATED",
                TicketType.GENERAL,
                new BigDecimal("100000.00"),
                TicketStatus.PAID,
                LocalDateTime.now(),
                firstUser,
                event
        );

        Ticket duplicatedTicket = new Ticket(
                "TKT-DUPLICATED",
                TicketType.VIP,
                new BigDecimal("200000.00"),
                TicketStatus.PAID,
                LocalDateTime.now(),
                secondUser,
                event
        );

        ticketRepository.saveAndFlush(firstTicket);

        assertThatThrownBy(() -> {
            ticketRepository.save(duplicatedTicket);
            ticketRepository.flush();
        }).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
}