package com.temple.erp.controller.api;

import com.temple.erp.model.Event;
import com.temple.erp.model.Festival;
import com.temple.erp.model.News;
import com.temple.erp.model.TempleUpdate;
import com.temple.erp.repository.EventRepository;
import com.temple.erp.repository.FestivalRepository;
import com.temple.erp.repository.NewsRepository;
import com.temple.erp.repository.TempleUpdateRepository;
import com.temple.erp.security.AuthPrincipal;
import com.temple.erp.service.FileStorageService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/content")
public class ContentApiController {
    private final FestivalRepository festivalRepository;
    private final EventRepository eventRepository;
    private final NewsRepository newsRepository;
    private final TempleUpdateRepository templeUpdateRepository;
    private final FileStorageService fileStorageService;

    public ContentApiController(FestivalRepository festivalRepository, EventRepository eventRepository,
                                NewsRepository newsRepository, TempleUpdateRepository templeUpdateRepository,
                                FileStorageService fileStorageService) {
        this.festivalRepository = festivalRepository;
        this.eventRepository = eventRepository;
        this.newsRepository = newsRepository;
        this.templeUpdateRepository = templeUpdateRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/festivals")
    public ResponseEntity<Map<String, Object>> festivals() {
        List<Festival> all = festivalRepository.findAllByOrderByEventDateDesc();
        LocalDate today = LocalDate.now();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("upcoming_festivals", all.stream().filter(f -> f.getEventDate() != null && f.getEventDate().isAfter(today) && !"inactive".equalsIgnoreCase(f.getStatus())).collect(Collectors.toList()));
        res.put("ongoing_festivals", all.stream().filter(f -> f.getEventDate() != null && f.getEventDate().isEqual(today)).collect(Collectors.toList()));
        res.put("completed_festivals", all.stream().filter(f -> f.getEventDate() == null || !f.getEventDate().isAfter(today) || "inactive".equalsIgnoreCase(f.getStatus())).collect(Collectors.toList()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> events() {
        List<Event> all = eventRepository.findAllByOrderByEventDateDesc();
        LocalDate today = LocalDate.now();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("upcoming_events", all.stream().filter(e -> !"completed".equalsIgnoreCase(e.getStatus()) && (e.getEventDate() == null || e.getEventDate().isAfter(today))).collect(Collectors.toList()));
        res.put("ongoing_events", all.stream().filter(e -> e.getEventDate() != null && e.getEventDate().isEqual(today) && !"completed".equalsIgnoreCase(e.getStatus())).collect(Collectors.toList()));
        res.put("completed_events", all.stream().filter(e -> "completed".equalsIgnoreCase(e.getStatus())).collect(Collectors.toList()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/news")
    public ResponseEntity<Map<String, Object>> news() {
        Map<String, Object> res = ApiUtil.body(true);
        res.put("news", newsRepository.findByStatusOrderByPublishedAtDesc("active"));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/temple-updates")
    public ResponseEntity<Map<String, Object>> templeUpdates(@RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String type) {
        List<TempleUpdate> data;
        if (status != null && type != null) data = templeUpdateRepository.findByStatusAndUpdateTypeOrderByCreatedAtDesc(status, type);
        else if (status != null) data = templeUpdateRepository.findByStatusOrderByCreatedAtDesc(status);
        else if (type != null) data = templeUpdateRepository.findByUpdateTypeOrderByCreatedAtDesc(type);
        else data = templeUpdateRepository.findAllByOrderByCreatedAtDesc();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("data", data);
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/temple-updates", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createTempleUpdate(Authentication auth,
            @RequestParam String title, @RequestParam(required = false) String description,
            @RequestParam(required = false) String short_description, @RequestParam(required = false) String update_type,
            @RequestParam(required = false) String event_date, @RequestParam(required = false) String event_end_date,
            @RequestParam(required = false) String is_featured, @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile image) throws Exception {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        TempleUpdate u = new TempleUpdate();
        u.setTitle(title); u.setDescription(description);
        u.setShortDescription(short_description != null ? short_description : (description != null && description.length() > 100 ? description.substring(0, 100) : description));
        u.setUpdateType(update_type);
        if (event_date != null && !event_date.isBlank()) u.setEventDate(LocalDate.parse(event_date.substring(0, 10)));
        if (event_end_date != null && !event_end_date.isBlank()) u.setEventEndDate(LocalDate.parse(event_end_date.substring(0, 10)));
        if (image != null && !image.isEmpty()) u.setImage(fileStorageService.store(image, "passes", "update"));
        u.setIsFeatured("true".equalsIgnoreCase(is_featured) || "1".equals(is_featured));
        u.setStatus("active".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status) || status == null ? "active" : "inactive");
        u.setCreatedBy(p.getId());
        templeUpdateRepository.save(u);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("message", "Update added successfully"); res.put("update", u);
        return ResponseEntity.status(201).body(res);
    }

    @PutMapping("/temple-updates/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable String id) {
        TempleUpdate u = templeUpdateRepository.findById(id).orElse(null);
        if (u == null) return ApiUtil.fail(404, "Update not found");
        u.setStatus("active".equalsIgnoreCase(u.getStatus()) ? "inactive" : "active");
        templeUpdateRepository.save(u);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("new_status", u.getStatus());
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/temple-updates/{id}")
    public ResponseEntity<Map<String, Object>> deleteUpdate(@PathVariable String id) {
        if (!templeUpdateRepository.existsById(id)) return ApiUtil.fail(404, "Update not found");
        templeUpdateRepository.deleteById(id);
        return ApiUtil.ok("Update deleted successfully");
    }

    // Simple admin CRUD for festivals/events/news (minimal, matches README intent)
    @PostMapping(value = "/festivals", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createFestival(@RequestParam String name, @RequestParam(required = false) String description,
            @RequestParam(required = false) String event_date, @RequestParam(required = false) String event_time,
            @RequestParam(required = false) MultipartFile image) throws Exception {
        Festival f = new Festival(); f.setName(name); f.setDescription(description);
        if (event_date != null && !event_date.isBlank()) f.setEventDate(LocalDate.parse(event_date.substring(0,10)));
        f.setEventTime(event_time);
        if (image != null && !image.isEmpty()) f.setImage(fileStorageService.store(image, "passes", "festival"));
        festivalRepository.save(f);
        Map<String, Object> res = ApiUtil.body(true); res.put("message", "Festival added"); res.put("festival", f);
        return ResponseEntity.status(201).body(res);
    }
    @DeleteMapping("/festivals/{id}")
    public ResponseEntity<Map<String, Object>> deleteFestival(@PathVariable String id) { festivalRepository.deleteById(id); return ApiUtil.ok("Festival deleted"); }

    @PostMapping(value = "/events", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createEvent(@RequestParam String title, @RequestParam(required = false) String description,
            @RequestParam(required = false) String event_date, @RequestParam(required = false) String event_time,
            @RequestParam(required = false) MultipartFile image) throws Exception {
        Event e = new Event(); e.setTitle(title); e.setDescription(description);
        if (event_date != null && !event_date.isBlank()) e.setEventDate(LocalDate.parse(event_date.substring(0,10)));
        e.setEventTime(event_time);
        if (image != null && !image.isEmpty()) e.setImage(fileStorageService.store(image, "passes", "event"));
        eventRepository.save(e);
        Map<String, Object> res = ApiUtil.body(true); res.put("message", "Event added"); res.put("event", e);
        return ResponseEntity.status(201).body(res);
    }
    @DeleteMapping("/events/{id}")
    public ResponseEntity<Map<String, Object>> deleteEvent(@PathVariable String id) { eventRepository.deleteById(id); return ApiUtil.ok("Event deleted"); }

    @PostMapping(value = "/news", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createNews(@RequestParam String title, @RequestParam(required = false) String content,
            @RequestParam(required = false) MultipartFile image) throws Exception {
        News n = new News(); n.setTitle(title); n.setContent(content);
        if (image != null && !image.isEmpty()) n.setImage(fileStorageService.store(image, "passes", "news"));
        newsRepository.save(n);
        Map<String, Object> res = ApiUtil.body(true); res.put("message", "News added"); res.put("news", n);
        return ResponseEntity.status(201).body(res);
    }
    @DeleteMapping("/news/{id}")
    public ResponseEntity<Map<String, Object>> deleteNews(@PathVariable String id) { newsRepository.deleteById(id); return ApiUtil.ok("News deleted"); }
}
