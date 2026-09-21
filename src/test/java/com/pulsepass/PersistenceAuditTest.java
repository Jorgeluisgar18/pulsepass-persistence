package com.pulsepass;

import com.pulsepass.domain.*;
import com.pulsepass.repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
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
class PersistenceAuditTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("pulsepass_audit_test")
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

    @PersistenceContext
    private EntityManager entityManager;


    // ---------------------------------------------------------
    // Venue por codigo + CHECK capacity > 0
    // ---------------------------------------------------------

    @Test
    void shouldFindVenueByCodeAndRejectInvalidCapacity() {

        Venue venue = new Venue(
                "VEN-SMR-01",
                "Marina Convention Center",
                "Santa Marta",
                "Avenida del Libertador",
                5000,
                true
        );

        venueRepository.saveAndFlush(venue);

        Venue storedVenue =
                venueRepository.findByCode("VEN-SMR-01")
                        .orElseThrow();

        assertThat(storedVenue.getCode())
                .isEqualTo("VEN-SMR-01");

        assertThat(storedVenue.getCapacity())
                .isGreaterThan(0);

        Venue invalidVenue = new Venue(
                "VEN-INVALID",
                "Invalid Venue",
                "Santa Marta",
                "Calle 1",
                0,
                true
        );

        assertThatThrownBy(() ->
                venueRepository.saveAndFlush(invalidVenue)
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }


    // ---------------------------------------------------------
    // Solo eventos PUBLISHED
    // ---------------------------------------------------------

    @Test
    void shouldReturnOnlyPublishedEventsOrderedByDate() {

        Venue venue = new Venue(
                "VEN-STATUS",
                "Status Arena",
                "Santa Marta",
                "Carrera 5",
                3000,
                true
        );

        venueRepository.save(venue);

        Event publishedOne = new Event(
                "EVT-PUB-001",
                "Published One",
                "Evento publicado",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2027, 2, 1, 18, 0),
                18,
                venue
        );

        Event publishedTwo = new Event(
                "EVT-PUB-002",
                "Published Two",
                "Segundo evento publicado",
                EventCategory.CULTURE,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2027, 3, 1, 18, 0),
                18,
                venue
        );

        Event draft = new Event(
                "EVT-DRAFT",
                "Draft Event",
                "Evento borrador",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.of(2027, 1, 1, 18, 0),
                18,
                venue
        );

        Event cancelled = new Event(
                "EVT-CANCELLED",
                "Cancelled Event",
                "Evento cancelado",
                EventCategory.MUSIC,
                EventStatus.CANCELLED,
                LocalDateTime.of(2027, 4, 1, 18, 0),
                18,
                venue
        );

        eventRepository.saveAll(
                List.of(
                        publishedOne,
                        publishedTwo,
                        draft,
                        cancelled
                )
        );

        eventRepository.flush();

        List<Event> result =
                eventRepository
                        .findByStatusOrderByEventDateAsc(
                                EventStatus.PUBLISHED
                        );

        assertThat(result)
                .extracting(Event::getEventCode)
                .containsExactly(
                        "EVT-PUB-001",
                        "EVT-PUB-002"
                );

        assertThat(result)
                .extracting(Event::getEventCode)
                .doesNotContain(
                        "EVT-DRAFT",
                        "EVT-CANCELLED"
                );
    }


    // ---------------------------------------------------------
    // User email IgnoreCase + User 1:1 UserProfile
    // ---------------------------------------------------------

    @Test
    void shouldFindUserByEmailAndRejectSecondProfile() {

        User user = new User(
                "andrea.audit",
                "andrea.audit@pulsepass.com",
                true
        );

        userRepository.saveAndFlush(user);

        assertThat(
                userRepository.findByEmailIgnoreCase(
                        "ANDREA.AUDIT@PULSEPASS.COM"
                )
        ).isPresent();

        UserProfile firstProfile = new UserProfile(
                "Andrea",
                "Martinez",
                "3001112233",
                "Santa Marta",
                LocalDate.of(2000, 5, 10)
        );

        user.assignProfile(firstProfile);

        userProfileRepository.saveAndFlush(firstProfile);

        assertThat(
                userProfileRepository.findByUserId(
                        user.getId()
                )
        ).isPresent();

        UserProfile secondProfile = new UserProfile(
                "Andrea",
                "Segundo Perfil",
                "3004445566",
                "Santa Marta",
                LocalDate.of(2000, 5, 10)
        );

        user.assignProfile(secondProfile);

        assertThatThrownBy(() ->
                userProfileRepository.saveAndFlush(
                        secondProfile
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }


    // ---------------------------------------------------------
    // Streaming URL + 3 artistas + busqueda por Solar Beat
    // ---------------------------------------------------------

    @Test
    void shouldPersistStreamingUrlAndFindSolarBeatEventsWithoutDuplicates() {

        Venue venue = new Venue(
                "VEN-ART-AUDIT",
                "Caribbean Arena",
                "Santa Marta",
                "Carrera 1",
                5000,
                true
        );

        venueRepository.save(venue);

        Artist solarBeat =
                artistRepository
                        .findByStageName("Solar Beat")
                        .orElseThrow();

        Artist neonWaves =
                artistRepository
                        .findByStageName("Neon Waves")
                        .orElseThrow();

        Artist caribbeanSound =
                artistRepository
                        .findByStageName("Caribbean Sound")
                        .orElseThrow();

        Event mainEvent = new Event(
                "CMF-AUDIT-2027",
                "Caribbean Music Fest",
                "Festival principal",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2027, 6, 10, 19, 0),
                18,
                venue
        );

        mainEvent.setStreamingUrl(
                "https://stream.pulsepass.com/cmf-2027"
        );

        mainEvent.addArtist(solarBeat);
        mainEvent.addArtist(neonWaves);
        mainEvent.addArtist(caribbeanSound);

        Event secondSolarEvent = new Event(
                "SOLAR-AUDIT-2027",
                "Solar Beat Live",
                "Segundo evento",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2027, 7, 20, 20, 0),
                18,
                venue
        );

        secondSolarEvent.addArtist(solarBeat);

        eventRepository.saveAll(
                List.of(
                        mainEvent,
                        secondSolarEvent
                )
        );

        eventRepository.flush();

        entityManager.clear();

        Event storedEvent =
                eventRepository
                        .findByEventCode(
                                "CMF-AUDIT-2027"
                        )
                        .orElseThrow();

        assertThat(storedEvent.getStreamingUrl())
                .isEqualTo(
                        "https://stream.pulsepass.com/cmf-2027"
                );

        assertThat(storedEvent.getArtists())
                .extracting(Artist::getStageName)
                .containsExactlyInAnyOrder(
                        "Solar Beat",
                        "Neon Waves",
                        "Caribbean Sound"
                );

        List<Event> solarEvents =
                eventRepository
                        .findByArtistStageName(
                                "Solar Beat"
                        );

        assertThat(solarEvents)
                .extracting(Event::getEventCode)
                .containsExactly(
                        "CMF-AUDIT-2027",
                        "SOLAR-AUDIT-2027"
                );

        List<Event> cityAndArtist =
                eventRepository.findByCityAndArtist(
                        "Santa Marta",
                        "Solar Beat"
                );

        assertThat(cityAndArtist)
                .extracting(Event::getEventCode)
                .containsExactly(
                        "CMF-AUDIT-2027",
                        "SOLAR-AUDIT-2027"
                );

        List<Event> recommended =
                eventRepository.findRecommendedEvents(
                        LocalDateTime.of(
                                2027, 1, 1, 0, 0
                        ),
                        "Santa Marta",
                        "solar"
                );

        assertThat(recommended)
                .extracting(Event::getEventCode)
                .containsExactly(
                        "CMF-AUDIT-2027",
                        "SOLAR-AUDIT-2027"
                );
    }


    // ---------------------------------------------------------
    // Tickets por usuario + ticketCode + eventos futuros
    // ---------------------------------------------------------

    @Test
    void shouldFindTicketsByUserAndFutureEvent() {

        Venue venue = new Venue(
                "VEN-FUTURE",
                "Future Arena",
                "Santa Marta",
                "Calle 20",
                2500,
                true
        );

        venueRepository.save(venue);

        Event futureEvent = new Event(
                "EVT-FUTURE",
                "Future Event",
                "Evento futuro",
                EventCategory.ENTERTAINMENT,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2028, 1, 10, 19, 0),
                18,
                venue
        );

        Event pastEvent = new Event(
                "EVT-PAST",
                "Past Event",
                "Evento pasado",
                EventCategory.ENTERTAINMENT,
                EventStatus.FINISHED,
                LocalDateTime.of(2026, 1, 10, 19, 0),
                18,
                venue
        );

        eventRepository.saveAll(
                List.of(
                        futureEvent,
                        pastEvent
                )
        );

        User user = new User(
                "future.user",
                "future.user@pulsepass.com",
                true
        );

        userRepository.save(user);

        Ticket futureTicket = new Ticket(
                "TKT-FUTURE",
                TicketType.GENERAL,
                new BigDecimal("120000.00"),
                TicketStatus.PAID,
                LocalDateTime.of(2027, 1, 1, 10, 0),
                user,
                futureEvent
        );

        Ticket pastTicket = new Ticket(
                "TKT-PAST",
                TicketType.GENERAL,
                new BigDecimal("90000.00"),
                TicketStatus.USED,
                LocalDateTime.of(2025, 12, 1, 10, 0),
                user,
                pastEvent
        );

        ticketRepository.saveAll(
                List.of(
                        futureTicket,
                        pastTicket
                )
        );

        ticketRepository.flush();

        assertThat(
                ticketRepository.findByTicketCode(
                        "TKT-FUTURE"
                )
        ).isPresent();

        List<Ticket> userTickets =
                ticketRepository
                        .findByUserEmailIgnoreCase(
                                "FUTURE.USER@PULSEPASS.COM"
                        );

        assertThat(userTickets)
                .extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder(
                        "TKT-FUTURE",
                        "TKT-PAST"
                );

        List<Ticket> futureTickets =
                ticketRepository
                        .findTicketsForFutureEvents(
                                LocalDateTime.of(
                                        2027, 1, 1, 0, 0
                                )
                        );

        assertThat(futureTickets)
                .extracting(Ticket::getTicketCode)
                .containsExactly(
                        "TKT-FUTURE"
                );
    }
}