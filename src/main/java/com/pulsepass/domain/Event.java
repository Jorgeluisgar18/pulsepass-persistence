package com.pulsepass.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_code", nullable = false, unique = true)
    private String eventCode;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "minimum_age")
    private Integer minimumAge;

    @Column(name = "streaming_url", length = 500)
    private String streamingUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToMany
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    @OneToMany(mappedBy = "event")
    private Set<Ticket> tickets = new HashSet<>();

    protected Event() {
    }

    public Event(
            String eventCode,
            String name,
            String description,
            EventCategory category,
            EventStatus status,
            LocalDateTime eventDate,
            Integer minimumAge,
            Venue venue
    ) {
        this.eventCode = eventCode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.status = status;
        this.eventDate = eventDate;
        this.minimumAge = minimumAge;
        this.venue = venue;
    }

    public void addArtist(Artist artist) {
        this.artists.add(artist);
        artist.getEvents().add(this);
    }

    public Long getId() {
        return id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public Integer getMinimumAge() {
        return minimumAge;
    }

    public String getStreamingUrl() {
        return streamingUrl;
    }

    public Venue getVenue() {
        return venue;
    }

    public Set<Artist> getArtists() {
        return artists;
    }

    public Set<Ticket> getTickets() {
        return tickets;
    }
}