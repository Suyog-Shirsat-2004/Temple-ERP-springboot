package com.temple.erp.controller.web;

import com.temple.erp.model.Event;
import com.temple.erp.model.Festival;
import com.temple.erp.model.News;
import com.temple.erp.model.Notification;
import com.temple.erp.model.RestaurantMenuItem;
import com.temple.erp.model.Room;
import com.temple.erp.repository.DarshanPassRepository;
import com.temple.erp.repository.DonationRepository;
import com.temple.erp.repository.EventRepository;
import com.temple.erp.repository.FestivalRepository;
import com.temple.erp.repository.NewsRepository;
import com.temple.erp.repository.NotificationRepository;
import com.temple.erp.repository.RestaurantMenuRepository;
import com.temple.erp.repository.RestaurantOrderRepository;
import com.temple.erp.repository.RoomBookingRepository;
import com.temple.erp.repository.RoomRepository;
import com.temple.erp.repository.UserRepository;
import com.temple.erp.service.EmailService;
import com.temple.erp.service.FileStorageService;
import com.temple.erp.service.PdfService;
import com.temple.erp.service.QrCodeService;
import java.time.LocalDate;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequestMapping("/admin")
public class AdminWebController {
    private final UserRepository userRepository;
    private final DarshanPassRepository passRepository;
    private final RoomBookingRepository bookingRepository;
    private final DonationRepository donationRepository;
    private final RestaurantOrderRepository orderRepository;
    private final RestaurantMenuRepository menuRepository;
    private final RoomRepository roomRepository;
    private final FestivalRepository festivalRepository;
    private final EventRepository eventRepository;
    private final NewsRepository newsRepository;
    private final NotificationRepository notificationRepository;
    private final FileStorageService fileStorageService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final QrCodeService qrCodeService;

    private static final List<String> RESTRO_IMAGES = List.of("re1.jpg", "re2.jpg", "rre3.jpg", "re4.jpg", "re5.jpg", "re7.webp");

    public AdminWebController(UserRepository userRepository, DarshanPassRepository passRepository,
                              RoomBookingRepository bookingRepository, DonationRepository donationRepository,
                              RestaurantOrderRepository orderRepository, RestaurantMenuRepository menuRepository, RoomRepository roomRepository,
                              FestivalRepository festivalRepository, EventRepository eventRepository,
                              NewsRepository newsRepository, NotificationRepository notificationRepository, FileStorageService fileStorageService,
                              PdfService pdfService, EmailService emailService, QrCodeService qrCodeService) {
        this.userRepository = userRepository; this.passRepository = passRepository; this.bookingRepository = bookingRepository;
        this.donationRepository = donationRepository; this.orderRepository = orderRepository; this.menuRepository = menuRepository; this.roomRepository = roomRepository;
        this.festivalRepository = festivalRepository; this.eventRepository = eventRepository; this.newsRepository = newsRepository;
        this.notificationRepository = notificationRepository; this.fileStorageService = fileStorageService;
        this.pdfService = pdfService; this.emailService = emailService; this.qrCodeService = qrCodeService;
    }

    private String resolveUserEmail(String userId, String fallbackEmail) {
        // Prefer user's profile email (Gmail/profile), then fallback to entity email
        if (userId != null && !userId.isBlank()) {
            try {
                String profile = userRepository.findById(userId).map(u -> u.getEmail()).filter(e -> e != null && !e.isBlank()).map(String::trim).orElse(null);
                if (profile != null) return profile;
            } catch (Exception ignored) {}
        }
        if (fallbackEmail != null && !fallbackEmail.isBlank()) return fallbackEmail.trim();
        return null;
    }

    private String resolveUserName(String userId, String fallbackName) {
        if (userId != null && !userId.isBlank()) {
            try {
                String profile = userRepository.findById(userId).map(u -> u.getName()).filter(n -> n != null && !n.isBlank()).map(String::trim).orElse(null);
                if (profile != null) return profile;
            } catch (Exception ignored) {}
        }
        if (fallbackName != null && !fallbackName.isBlank()) return fallbackName.trim();
        return null;
    }

    private void notify(String userId, String title, String message, String type) {
        try {
            if (userId == null || userId.isBlank()) return;
            Notification n = new Notification();
            n.setUserId(userId); n.setTitle(title); n.setMessage(message); n.setType(type);
            notificationRepository.save(n);
        } catch (Exception ignored) {}
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var passes = passRepository.findAll(); var bookings = bookingRepository.findAll();
        var donations = donationRepository.findAll(); var orders = orderRepository.findAll();
        long pendingPasses = passes.stream().filter(p -> "pending".equalsIgnoreCase(p.getStatus())).count();
        double donationAmount = donations.stream().filter(d -> d.getPaymentStatus()!=null && (d.getPaymentStatus().equalsIgnoreCase("completed")||d.getPaymentStatus().equalsIgnoreCase("paid")||d.getPaymentStatus().equalsIgnoreCase("Success"))).mapToDouble(d -> d.getAmount()==null?0:d.getAmount()).sum();
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalPasses", passes.size());
        model.addAttribute("totalBookings", bookings.size());
        model.addAttribute("totalDonationsCount", donations.size());
        model.addAttribute("donationAmount", donationAmount);
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("pendingPasses", pendingPasses);
        model.addAttribute("totalPending", pendingPasses);
        model.addAttribute("recentPasses", passes.stream().sorted((a,b)->String.valueOf(b.getId()).compareTo(String.valueOf(a.getId()))).limit(5).toList());
        model.addAttribute("recentBookings", bookings.stream().sorted((a,b)->String.valueOf(b.getId()).compareTo(String.valueOf(a.getId()))).limit(5).toList());
        model.addAttribute("recentDonations", donations.stream().sorted((a,b)->String.valueOf(b.getId()).compareTo(String.valueOf(a.getId()))).limit(5).toList());
        model.addAttribute("recentOrders", orders.stream().sorted((a,b)->String.valueOf(b.getId()).compareTo(String.valueOf(a.getId()))).limit(5).toList());
        return "admin/dashboard";
    }

    @GetMapping("/users") public String users(Model model) { var u = userRepository.findAll(); u.forEach(x->x.setPassword(null)); model.addAttribute("users", u); return "admin/users"; }
    @GetMapping("/passes")
    public String passes(Model model) {
        var passes = passRepository.findAllByOrderByIdDesc();
        try {
            var userMap = userRepository.findAll().stream()
                    .filter(u -> u != null && u.getId() != null)
                    .collect(java.util.stream.Collectors.toMap(com.temple.erp.model.User::getId, u -> u, (a, b) -> a));
            for (var p : passes) {
                if (p == null) continue;
                com.temple.erp.model.User u = (p.getUserId() != null) ? userMap.get(p.getUserId()) : null;
                if (u != null) {
                    if (isBlank(p.getDevoteeName()) && !isBlank(u.getName())) p.setDevoteeName(u.getName());
                    if (isBlank(p.getPhone()) && !isBlank(u.getMobile())) p.setPhone(u.getMobile());
                    if (isBlank(p.getEmail()) && !isBlank(u.getEmail())) p.setEmail(u.getEmail());
                    // Keep aadhar/gender/address on pass as entered; no overwrite of existing pass details
                }
            }
        } catch (Exception ignored) {}
        model.addAttribute("passes", passes);
        return "admin/passes";
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    @GetMapping("/bookings") public String bookings(Model model) { model.addAttribute("bookings", bookingRepository.findAllByOrderByIdDesc()); return "admin/bookings"; }
    @GetMapping("/donations") public String donations(Model model) { model.addAttribute("donations", donationRepository.findAllByOrderByIdDesc()); return "admin/donations"; }
    @GetMapping("/restaurant") public String restaurant(Model model) {
        var menuItems = menuRepository.findAll();
        java.util.LinkedHashSet<String> categoryOptions = new java.util.LinkedHashSet<>(
                java.util.List.of("Meals", "Prasad", "Thali", "Breakfast", "Lunch", "Dinner", "Snacks", "Beverages", "Sweets", "Special"));
        menuItems.stream()
                .map(RestaurantMenuItem::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .forEach(c -> { if (categoryOptions.stream().noneMatch(o -> o.equalsIgnoreCase(c))) categoryOptions.add(c); });
        java.util.LinkedHashSet<String> itemNameOptions = new java.util.LinkedHashSet<>();
        menuItems.stream()
                .map(RestaurantMenuItem::getName)
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .forEach(n -> { if (itemNameOptions.stream().noneMatch(o -> o.equalsIgnoreCase(n))) itemNameOptions.add(n); });
        java.util.List<String> uploadedRestroImages = new java.util.ArrayList<>();
        try {
            java.nio.file.Path dir = fileStorageService.getRoot().resolve("restaurant");
            if (java.nio.file.Files.isDirectory(dir)) {
                try (var stream = java.nio.file.Files.list(dir)) {
                    stream.filter(java.nio.file.Files::isRegularFile)
                          .map(p -> p.getFileName().toString())
                          .filter(n -> n.matches("(?i).+\\.(jpg|jpeg|png|gif|webp)$"))
                          .sorted(String.CASE_INSENSITIVE_ORDER)
                          .forEach(uploadedRestroImages::add);
                }
            }
        } catch (Exception ignored) {}
        // Any image value used by menu items that isn't a static restro image is treated as uploaded; include if missing
        for (var m : menuItems) {
            String img = m.getImage();
            if (img != null && !img.isBlank() && !RESTRO_IMAGES.contains(img) && !uploadedRestroImages.contains(img)) {
                uploadedRestroImages.add(img);
            }
        }
        model.addAttribute("orders", orderRepository.findAllByOrderByIdDesc());
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("categoryOptions", categoryOptions);
        model.addAttribute("itemNameOptions", itemNameOptions);
        model.addAttribute("restroImageOptions", RESTRO_IMAGES);
        model.addAttribute("uploadedRestroImages", uploadedRestroImages);
        return "admin/restaurant";
    }

    @PostMapping("/restaurant/menu/add")
    public String addMenuItem(@RequestParam String name,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) Double price,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false, name = "image_select") String imageSelect,
                              @RequestParam(required = false, name = "image_file") MultipartFile imageFile,
                              @RequestParam(required = false, defaultValue = "true") String available,
                              RedirectAttributes ra) {
        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("error", "Menu item name is required.");
            return "redirect:/admin/restaurant";
        }
        try {
            RestaurantMenuItem m = new RestaurantMenuItem();
            m.setName(name.trim());
            m.setCategory(category != null ? category.trim() : null);
            m.setPrice(price);
            m.setDescription(description != null ? description.trim() : null);
            m.setAvailable(Boolean.parseBoolean(available) || "on".equalsIgnoreCase(available));
            if (imageFile != null && !imageFile.isEmpty()) {
                m.setImage(fileStorageService.store(imageFile, "restaurant", "menu"));
            } else if (imageSelect != null && !imageSelect.isBlank()) {
                m.setImage(imageSelect.trim());
            }
            menuRepository.save(m);
            ra.addFlashAttribute("success", "Menu item added: " + m.getName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not add menu item. Check image format (jpg, jpeg, png, gif, webp) and details.");
        }
        return "redirect:/admin/restaurant";
    }

    @PostMapping("/restaurant/menu/{id}/update")
    public String updateMenuItem(@PathVariable String id,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) Double price,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false, name = "image_select") String imageSelect,
                                 @RequestParam(required = false, name = "image_file") MultipartFile imageFile,
                                 @RequestParam(required = false) String available,
                                 RedirectAttributes ra) {
        try {
            RestaurantMenuItem m = menuRepository.findById(id).orElse(null);
            if (m == null) {
                ra.addFlashAttribute("error", "Menu item not found.");
                return "redirect:/admin/restaurant";
            }
            if (name != null && !name.isBlank()) m.setName(name.trim());
            m.setCategory(category != null ? category.trim() : m.getCategory());
            m.setPrice(price);
            m.setDescription(description != null ? description.trim() : m.getDescription());
            m.setAvailable(available != null && ("true".equalsIgnoreCase(available) || "on".equalsIgnoreCase(available) || "yes".equalsIgnoreCase(available)));
            if (imageFile != null && !imageFile.isEmpty()) {
                m.setImage(fileStorageService.store(imageFile, "restaurant", "menu"));
            } else if (imageSelect != null && !imageSelect.isBlank()) {
                m.setImage(imageSelect.trim());
            }
            menuRepository.save(m);
            ra.addFlashAttribute("success", "Menu item updated: " + m.getName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not update menu item. Check image format and details.");
        }
        return "redirect:/admin/restaurant";
    }

    @PostMapping("/restaurant/menu/{id}/delete")
    public String deleteMenuItem(@PathVariable String id, RedirectAttributes ra) {
        if (menuRepository.existsById(id)) {
            menuRepository.deleteById(id);
            ra.addFlashAttribute("success", "Menu item deleted.");
        } else {
            ra.addFlashAttribute("error", "Menu item not found.");
        }
        return "redirect:/admin/restaurant";
    }

    @PostMapping("/restaurant/image/upload")
    public String uploadRestaurantImage(@RequestParam(name = "image_file") MultipartFile imageFile, RedirectAttributes ra) {
        try {
            if (imageFile == null || imageFile.isEmpty()) {
                ra.addFlashAttribute("error", "Please choose an image to upload.");
                return "redirect:/admin/restaurant";
            }
            String stored = fileStorageService.store(imageFile, "restaurant", "restro");
            ra.addFlashAttribute("success", "Image added: " + stored + ". It is now available in image selection.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not upload image. Use jpg, jpeg, png, gif, or webp.");
        }
        return "redirect:/admin/restaurant";
    }
    @GetMapping("/rooms") public String rooms(Model model) {
        var rooms = roomRepository.findAll();
        java.util.LinkedHashSet<String> roomTypeOptions = new java.util.LinkedHashSet<>(
                java.util.List.of("Standard", "Deluxe", "AC", "Non-AC", "Suite", "Dormitory", "Family", "Cottage"));
        rooms.stream().map(Room::getRoomType).filter(t -> t != null && !t.isBlank()).map(String::trim)
                .forEach(t -> { if (roomTypeOptions.stream().noneMatch(o -> o.equalsIgnoreCase(t))) roomTypeOptions.add(t); });
        java.util.LinkedHashSet<String> roomNameOptions = new java.util.LinkedHashSet<>();
        rooms.stream().map(Room::getRoomName).filter(n -> n != null && !n.isBlank()).map(String::trim)
                .forEach(n -> { if (roomNameOptions.stream().noneMatch(o -> o.equalsIgnoreCase(n))) roomNameOptions.add(n); });
        java.util.List<String> roomStatusOptions = java.util.List.of("available", "booked", "maintenance");
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypeOptions", roomTypeOptions);
        model.addAttribute("roomNameOptions", roomNameOptions);
        model.addAttribute("roomStatusOptions", roomStatusOptions);
        model.addAttribute("capacityOptions", java.util.List.of(2, 4, 6));
        return "admin/rooms";
    }

    @PostMapping("/rooms/add")
    public String addRoom(@RequestParam String room_number,
                          @RequestParam(required = false) String room_name,
                          @RequestParam(required = false) String room_type,
                          @RequestParam(required = false) Double price_per_day,
                          @RequestParam(required = false) Integer capacity,
                          @RequestParam(required = false, defaultValue = "available") String status,
                          @RequestParam(required = false) String amenities,
                          @RequestParam(required = false) String description,
                          RedirectAttributes ra) {
        if (room_number == null || room_number.isBlank()) {
            ra.addFlashAttribute("error", "Room number is required.");
            return "redirect:/admin/rooms";
        }
        if (capacity == null || !(capacity == 2 || capacity == 4 || capacity == 6)) {
            ra.addFlashAttribute("error", "Room capacity is required and must be 2, 4, or 6 persons.");
            return "redirect:/admin/rooms";
        }
        Room r = new Room();
        r.setRoomNumber(room_number.trim());
        r.setRoomName(room_name);
        r.setRoomType(room_type);
        r.setPricePerDay(price_per_day);
        r.setCapacity(capacity);
        r.setStatus(status == null || status.isBlank() ? "available" : status.trim());
        r.setAmenities(amenities);
        r.setDescription(description);
        roomRepository.save(r);
        ra.addFlashAttribute("success", "Room added successfully: " + r.getRoomNumber() + " (capacity " + capacity + " persons)");
        return "redirect:/admin/rooms";
    }

    @PostMapping("/rooms/{id}/update")
    public String updateRoom(@PathVariable String id,
                             @RequestParam String room_number,
                             @RequestParam(required = false) String room_name,
                             @RequestParam(required = false) String room_type,
                             @RequestParam(required = false) Double price_per_day,
                             @RequestParam(required = false) Integer capacity,
                             @RequestParam(required = false, defaultValue = "available") String status,
                             @RequestParam(required = false) String amenities,
                             @RequestParam(required = false) String description,
                             RedirectAttributes ra) {
        Room r = roomRepository.findById(id).orElse(null);
        if (r == null) { ra.addFlashAttribute("error", "Room not found."); return "redirect:/admin/rooms"; }
        if (room_number == null || room_number.isBlank()) { ra.addFlashAttribute("error", "Room number is required."); return "redirect:/admin/rooms"; }
        if (capacity == null || !(capacity == 2 || capacity == 4 || capacity == 6)) {
            ra.addFlashAttribute("error", "Room capacity is required and must be 2, 4, or 6 persons.");
            return "redirect:/admin/rooms";
        }
        r.setRoomNumber(room_number.trim());
        r.setRoomName(room_name != null ? room_name.trim() : null);
        r.setRoomType(room_type != null ? room_type.trim() : null);
        r.setPricePerDay(price_per_day);
        r.setCapacity(capacity);
        r.setStatus(status == null || status.isBlank() ? "available" : status.trim());
        r.setAmenities(amenities != null ? amenities.trim() : null);
        r.setDescription(description != null ? description.trim() : null);
        roomRepository.save(r);
        ra.addFlashAttribute("success", "Room updated: " + r.getRoomNumber());
        return "redirect:/admin/rooms";
    }

    @PostMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable String id, RedirectAttributes ra) {
        if (roomRepository.existsById(id)) {
            roomRepository.deleteById(id);
            ra.addFlashAttribute("success", "Room deleted.");
        } else {
            ra.addFlashAttribute("error", "Room not found.");
        }
        return "redirect:/admin/rooms";
    }

    @PostMapping("/restaurant/{id}/approve")
    public String approveRestaurantOrder(@PathVariable String id, RedirectAttributes ra) {
        final String[] emailTo = {null};
        final String[] tokenRef = {id};
        orderRepository.findById(id).ifPresent(o -> {
            o.setStatus("approved");
            if (o.getPaymentStatus() == null || o.getPaymentStatus().isBlank() || "pending".equalsIgnoreCase(o.getPaymentStatus())) {
                o.setPaymentStatus("paid");
                o.setPaymentDate(java.time.Instant.now());
            }
            orderRepository.save(o);
            tokenRef[0] = o.getOrderId() != null ? o.getOrderId() : o.getId();
            notify(o.getUserId(), "Restaurant Order Approved", "Your order " + tokenRef[0] + " has been approved by admin. Your token is now available.", "restaurant");
            emailTo[0] = resolveUserEmail(o.getUserId(), null);
            String name = resolveUserName(o.getUserId(), o.getName());
            if (emailTo[0] != null && !emailTo[0].isBlank()) {
                final String to = emailTo[0];
                final com.temple.erp.model.RestaurantOrder orderForMail = o;
                final String nameForMail = name;
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        byte[] pdf = pdfService.generateRestaurantTokenPdf(orderForMail, nameForMail);
                        boolean sent = emailService.sendPdf(to,
                                "Temple ERP - Restaurant Order Token " + tokenRef[0] + " (Approved)",
                                "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour restaurant order " + tokenRef[0] + " has been approved by admin. Your token PDF is attached.\n\n- Temple ERP",
                                pdf, "restaurant-token-" + tokenRef[0] + ".pdf");
                        if (!sent) org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Restaurant approve email not sent for {} to {}: {}", tokenRef[0], to, emailService.statusHint(to));
                    } catch (Exception ex) {
                        org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Background restaurant approve email failed for {}: {}", tokenRef[0], ex.getMessage());
                    }
                });
            }
        });
        String msg = "Order approved. Token is now available for the user.";
        if (emailTo[0] != null && !emailTo[0].isBlank()) msg += " Token PDF will be emailed to " + emailTo[0] + " shortly.";
        else msg += " PDF email not sent (" + emailService.statusHint(emailTo[0]) + ").";
        ra.addFlashAttribute("success", msg);
        return "redirect:/admin/restaurant";
    }

    @PostMapping("/restaurant/{id}/reject")
    public String rejectRestaurantOrder(@PathVariable String id, RedirectAttributes ra) {
        orderRepository.findById(id).ifPresent(o -> {
            o.setStatus("rejected");
            orderRepository.save(o);
            notify(o.getUserId(), "Restaurant Order Rejected", "Your order " + (o.getOrderId() != null ? o.getOrderId() : o.getId()) + " was rejected by admin. Please contact administration.", "restaurant");
        });
        ra.addFlashAttribute("success", "Order rejected. Status updated.");
        return "redirect:/admin/restaurant";
    }

    @PostMapping("/restaurant/{id}/status")
    public String updateRestaurantStatus(@PathVariable String id, @RequestParam String status,
                                         @RequestParam(required = false) String payment_status,
                                         RedirectAttributes ra) {
        final String[] emailTo = {null};
        final boolean[] willEmail = {false};
        orderRepository.findById(id).ifPresent(o -> {
            o.setStatus(status);
            if (payment_status != null && !payment_status.isBlank()) o.setPaymentStatus(payment_status);
            boolean approving = status != null && ("approved".equalsIgnoreCase(status) || "confirmed".equalsIgnoreCase(status));
            if (approving && (o.getPaymentStatus() == null || o.getPaymentStatus().isBlank() || "pending".equalsIgnoreCase(o.getPaymentStatus()))) {
                o.setPaymentStatus("paid");
                o.setPaymentDate(java.time.Instant.now());
            }
            orderRepository.save(o);
            String tokenId = o.getOrderId() != null ? o.getOrderId() : o.getId();
            notify(o.getUserId(), "Restaurant Order Update", "Your order " + tokenId + " status: " + status + (o.getPaymentStatus() != null ? ", payment: " + o.getPaymentStatus() : "") + ".", "restaurant");
            if (approving) {
                emailTo[0] = resolveUserEmail(o.getUserId(), null);
                String name = resolveUserName(o.getUserId(), o.getName());
                if (emailTo[0] != null && !emailTo[0].isBlank()) {
                    willEmail[0] = true;
                    final String to = emailTo[0];
                    final com.temple.erp.model.RestaurantOrder orderForMail = o;
                    final String nameForMail = name;
                    final String tokenForMail = tokenId;
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            byte[] pdf = pdfService.generateRestaurantTokenPdf(orderForMail, nameForMail);
                            boolean sent = emailService.sendPdf(to,
                                    "Temple ERP - Restaurant Order Token " + tokenForMail + " (" + status + ")",
                                    "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour restaurant order " + tokenForMail + " status is now " + status + ". Your token PDF is attached.\n\n- Temple ERP",
                                    pdf, "restaurant-token-" + tokenForMail + ".pdf");
                            if (!sent) org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Restaurant status email not sent for {} to {}: {}", tokenForMail, to, emailService.statusHint(to));
                        } catch (Exception ex) {
                            org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Background restaurant status email failed for {}: {}", tokenForMail, ex.getMessage());
                        }
                    });
                }
            }
        });
        String msg = "Order updated (status/payment).";
        if (status != null && ("approved".equalsIgnoreCase(status) || "confirmed".equalsIgnoreCase(status))) {
            if (willEmail[0]) msg += " Token PDF will be emailed to " + emailTo[0] + " shortly.";
            else msg += " PDF email not sent (" + emailService.statusHint(emailTo[0]) + ").";
        }
        ra.addFlashAttribute("success", msg);
        return "redirect:/admin/restaurant";
    }
    @GetMapping("/devotees") public String devotees(Model model) { var u = userRepository.findAll(); u.forEach(x->x.setPassword(null)); model.addAttribute("devotees", u); return "admin/devotees"; }
    @GetMapping("/reports")
    public String reports(Model model) {
        var donations = donationRepository.findAll();
        var orders = orderRepository.findAll();
        var passes = passRepository.findAll();
        double donationTotal = donations.stream().filter(d -> d.getPaymentStatus() != null && (d.getPaymentStatus().equalsIgnoreCase("completed") || d.getPaymentStatus().equalsIgnoreCase("paid") || d.getPaymentStatus().equalsIgnoreCase("Success"))).mapToDouble(d -> d.getAmount() == null ? 0 : d.getAmount()).sum();
        double restaurantRevenue = orders.stream().filter(o -> "paid".equalsIgnoreCase(o.getPaymentStatus())).mapToDouble(o -> o.getTotalAmount() == null ? 0 : o.getTotalAmount()).sum();
        double passRevenue = passes.stream().filter(p -> "paid".equalsIgnoreCase(p.getPaymentStatus())).mapToDouble(p -> p.getTotalAmount() != null ? p.getTotalAmount() : (p.getAmount() == null ? 0 : p.getAmount())).sum();
        model.addAttribute("donationTotal", donationTotal);
        model.addAttribute("restaurantRevenue", restaurantRevenue);
        model.addAttribute("passRevenue", passRevenue);
        model.addAttribute("grandTotal", donationTotal + restaurantRevenue + passRevenue);
        model.addAttribute("donationsCount", donations.size());
        model.addAttribute("ordersCount", orders.size());
        model.addAttribute("passesCount", passes.size());
        return "admin/reports";
    }

    private void addContentImageOptions(org.springframework.ui.Model model, String type) {
        java.util.LinkedHashSet<String> staticImages = new java.util.LinkedHashSet<>();
        try {
            // Discover actual available images from static/images/img (classpath or filesystem)
            java.util.List<java.nio.file.Path> candidates = new java.util.ArrayList<>();
            try { candidates.add(java.nio.file.Paths.get("src/main/resources/static/images/img")); } catch (Exception ignored) {}
            try { candidates.add(java.nio.file.Paths.get("target/classes/static/images/img")); } catch (Exception ignored) {}
            try {
                var res = new org.springframework.core.io.ClassPathResource("static/images/img");
                if (res.exists()) candidates.add(res.getFile().toPath());
            } catch (Exception ignored) {}
            for (java.nio.file.Path dir : candidates) {
                if (dir == null) continue;
                try {
                    if (java.nio.file.Files.isDirectory(dir)) {
                        try (var stream = java.nio.file.Files.list(dir)) {
                            stream.filter(java.nio.file.Files::isRegularFile)
                                  .map(p -> p.getFileName().toString())
                                  .filter(n -> n.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp)$"))
                                  .sorted(String.CASE_INSENSITIVE_ORDER)
                                  .forEach(staticImages::add);
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        // Fallback if discovery fails
        if (staticImages.isEmpty()) {
            staticImages.addAll(java.util.List.of(
                "banner.jpg", "Trimbakeshwar-Jyotirling.jpg", "Brahmagiri.jpg", "kushavart1.jpg", "Tryambakeshvara1.jpg",
                "mahashivratra.jpg", "chaitra.jpg", "trimbak.jpg", "garv-griha.jpg", "stairways.jpg"
            ));
        }
        java.util.LinkedHashSet<String> uploaded = new java.util.LinkedHashSet<>();
        for (String folder : java.util.List.of("content", "festivals", "events", "news")) {
            try {
                java.nio.file.Path dir = fileStorageService.getRoot().resolve(folder);
                if (java.nio.file.Files.isDirectory(dir)) {
                    try (var stream = java.nio.file.Files.list(dir)) {
                        stream.filter(java.nio.file.Files::isRegularFile)
                              .map(p -> folder + "/" + p.getFileName().toString())
                              .filter(n -> n.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp)$"))
                              .sorted(String.CASE_INSENSITIVE_ORDER)
                              .forEach(uploaded::add);
                    }
                }
            } catch (Exception ignored) {}
        }
        model.addAttribute("staticContentImages", new java.util.ArrayList<>(staticImages));
        model.addAttribute("uploadedContentImages", new java.util.ArrayList<>(uploaded));
        model.addAttribute("contentType", type);
    }

    private String resolveContentImage(MultipartFile imageFile, String imageSelect, String imageUrl, String uploadFolder) throws Exception {
        if (imageFile != null && !imageFile.isEmpty()) {
            String stored = fileStorageService.store(imageFile, uploadFolder, uploadFolder);
            if (stored != null && !stored.isBlank()) return "/uploads/" + uploadFolder + "/" + stored;
        }
        if (imageSelect != null && !imageSelect.isBlank()) {
            String sel = imageSelect.trim();
            if (sel.startsWith("/") || sel.startsWith("http")) return sel;
            if (sel.contains("/")) return "/uploads/" + sel; // e.g. content/abc.jpg
            return "/images/img/" + sel; // static
        }
        if (imageUrl != null && !imageUrl.isBlank()) return imageUrl.trim();
        return null;
    }

    @GetMapping("/festivals")
    public String festivals(Model model) {
        model.addAttribute("festivals", festivalRepository.findAllByOrderByEventDateDesc());
        addContentImageOptions(model, "festivals");
        return "admin/festivals";
    }
    @PostMapping("/festivals/add")
    public String addFestival(@RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String event_date,
                              @RequestParam(required = false) String event_time,
                              @RequestParam(required = false, defaultValue = "active") String status,
                              @RequestParam(required = false) String image,
                              @RequestParam(required = false) String image_select,
                              @RequestParam(required = false, name = "image_file") MultipartFile imageFile,
                              RedirectAttributes ra) throws Exception {
        if (name == null || name.isBlank()) { ra.addFlashAttribute("error", "Festival name is required."); return "redirect:/admin/festivals"; }
        Festival f = new Festival();
        f.setName(name.trim());
        f.setDescription(description);
        f.setEventTime(event_time);
        f.setStatus(status == null || status.isBlank() ? "active" : status.trim());
        f.setImage(resolveContentImage(imageFile, image_select, image, "festivals"));
        try { if (event_date != null && !event_date.isBlank()) f.setEventDate(LocalDate.parse(event_date)); } catch (Exception ignored) {}
        festivalRepository.save(f);
        ra.addFlashAttribute("success", "Festival added: " + f.getName());
        return "redirect:/admin/festivals";
    }
    @PostMapping("/festivals/image/upload")
    public String uploadFestivalImage(@RequestParam(name = "image_file") MultipartFile imageFile, RedirectAttributes ra) {
        try {
            if (imageFile == null || imageFile.isEmpty()) { ra.addFlashAttribute("error", "Please choose an image to upload."); return "redirect:/admin/festivals"; }
            String stored = fileStorageService.store(imageFile, "content", "content");
            ra.addFlashAttribute("success", "Image added (available on all content): " + stored);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not upload image. Use jpg, jpeg, png, gif, or webp.");
        }
        return "redirect:/admin/festivals";
    }
    @GetMapping("/festivals/{id}/update")
    public String updateFestivalPage(@PathVariable String id, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Use the Update form on the Festivals page to edit all details, then click Save Update.");
        return "redirect:/admin/festivals";
    }

    @PostMapping("/festivals/{id}/update")
    public String updateFestival(@PathVariable String id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String event_date,
                                 @RequestParam(required = false) String event_time,
                                 @RequestParam(required = false, defaultValue = "active") String status,
                                 @RequestParam(required = false) String image,
                                 @RequestParam(required = false) String image_select,
                                 @RequestParam(required = false, name = "image_file") MultipartFile imageFile,
                                 RedirectAttributes ra) throws Exception {
        Festival f = festivalRepository.findById(id).orElse(null);
        if (f == null) { ra.addFlashAttribute("error", "Festival not found."); return "redirect:/admin/festivals"; }
        if (name == null || name.isBlank()) { ra.addFlashAttribute("error", "Festival name is required."); return "redirect:/admin/festivals"; }
        f.setName(name.trim());
        f.setDescription(description != null ? description.trim() : null);
        f.setEventTime(event_time != null ? event_time.trim() : null);
        f.setStatus(status == null || status.isBlank() ? "active" : status.trim());
        String resolved = resolveContentImage(imageFile, image_select, image, "festivals");
        if (resolved != null && !resolved.isBlank()) f.setImage(resolved);
        try { f.setEventDate((event_date != null && !event_date.isBlank()) ? LocalDate.parse(event_date) : null); } catch (Exception ignored) {}
        festivalRepository.save(f);
        ra.addFlashAttribute("success", "Festival updated: " + f.getName());
        return "redirect:/admin/festivals";
    }

    @PostMapping("/festivals/{id}/delete")
    public String deleteFestival(@PathVariable String id, RedirectAttributes ra) {
        if (festivalRepository.existsById(id)) { festivalRepository.deleteById(id); ra.addFlashAttribute("success", "Festival deleted."); }
        else { ra.addFlashAttribute("error", "Festival not found."); }
        return "redirect:/admin/festivals";
    }

    @GetMapping("/events")
    public String events(Model model) {
        model.addAttribute("events", eventRepository.findAllByOrderByEventDateDesc());
        addContentImageOptions(model, "events");
        return "admin/events";
    }
    @PostMapping("/events/add")
    public String addEvent(@RequestParam String title,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) String event_date,
                           @RequestParam(required = false) String event_time,
                           @RequestParam(required = false, defaultValue = "upcoming") String status,
                           @RequestParam(required = false) String image,
                           @RequestParam(required = false) String image_select,
                           @RequestParam(required = false, name = "image_file") MultipartFile imageFile,
                           RedirectAttributes ra) throws Exception {
        if (title == null || title.isBlank()) { ra.addFlashAttribute("error", "Event title is required."); return "redirect:/admin/events"; }
        Event e = new Event();
        e.setTitle(title.trim());
        e.setDescription(description);
        e.setEventTime(event_time);
        e.setStatus(status == null || status.isBlank() ? "upcoming" : status.trim());
        e.setImage(resolveContentImage(imageFile, image_select, image, "events"));
        try { if (event_date != null && !event_date.isBlank()) e.setEventDate(LocalDate.parse(event_date)); } catch (Exception ignored) {}
        eventRepository.save(e);
        ra.addFlashAttribute("success", "Event added: " + e.getTitle());
        return "redirect:/admin/events";
    }
    @PostMapping("/events/image/upload")
    public String uploadEventImage(@RequestParam(name = "image_file") MultipartFile imageFile, RedirectAttributes ra) {
        try {
            if (imageFile == null || imageFile.isEmpty()) { ra.addFlashAttribute("error", "Please choose an image to upload."); return "redirect:/admin/events"; }
            String stored = fileStorageService.store(imageFile, "content", "content");
            ra.addFlashAttribute("success", "Image added (available on all content): " + stored);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not upload image. Use jpg, jpeg, png, gif, or webp.");
        }
        return "redirect:/admin/events";
    }
    @GetMapping("/events/{id}/update")
    public String updateEventPage(@PathVariable String id, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Use the Update form on the Events page to edit all details, then click Save Update.");
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/update")
    public String updateEvent(@PathVariable String id,
                              @RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String event_date,
                              @RequestParam(required = false) String event_time,
                              @RequestParam(required = false, defaultValue = "upcoming") String status,
                              @RequestParam(required = false) String image,
                              @RequestParam(required = false) String image_select,
                              @RequestParam(required = false, name = "image_file") MultipartFile imageFile,
                              RedirectAttributes ra) throws Exception {
        Event e = eventRepository.findById(id).orElse(null);
        if (e == null) { ra.addFlashAttribute("error", "Event not found."); return "redirect:/admin/events"; }
        if (title == null || title.isBlank()) { ra.addFlashAttribute("error", "Event title is required."); return "redirect:/admin/events"; }
        e.setTitle(title.trim());
        e.setDescription(description != null ? description.trim() : null);
        e.setEventTime(event_time != null ? event_time.trim() : null);
        e.setStatus(status == null || status.isBlank() ? "upcoming" : status.trim());
        String resolved = resolveContentImage(imageFile, image_select, image, "events");
        if (resolved != null && !resolved.isBlank()) e.setImage(resolved);
        try { e.setEventDate((event_date != null && !event_date.isBlank()) ? LocalDate.parse(event_date) : null); } catch (Exception ignored) {}
        eventRepository.save(e);
        ra.addFlashAttribute("success", "Event updated: " + e.getTitle());
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable String id, RedirectAttributes ra) {
        if (eventRepository.existsById(id)) { eventRepository.deleteById(id); ra.addFlashAttribute("success", "Event deleted."); }
        else { ra.addFlashAttribute("error", "Event not found."); }
        return "redirect:/admin/events";
    }

    @GetMapping("/news")
    public String news(Model model) {
        model.addAttribute("news", newsRepository.findAll());
        addContentImageOptions(model, "news");
        return "admin/news";
    }
    @PostMapping("/news/add")
    public String addNews(@RequestParam String title,
                          @RequestParam(required = false) String content,
                          @RequestParam(required = false, defaultValue = "active") String status,
                          @RequestParam(required = false) String image,
                          @RequestParam(required = false) String image_select,
                          @RequestParam(required = false, name = "image_file") MultipartFile imageFile,
                          RedirectAttributes ra) throws Exception {
        if (title == null || title.isBlank()) { ra.addFlashAttribute("error", "News title is required."); return "redirect:/admin/news"; }
        News n = new News();
        n.setTitle(title.trim());
        n.setContent(content);
        n.setStatus(status == null || status.isBlank() ? "active" : status.trim());
        n.setImage(resolveContentImage(imageFile, image_select, image, "news"));
        n.setPublishedAt(java.time.Instant.now());
        newsRepository.save(n);
        ra.addFlashAttribute("success", "News published: " + n.getTitle());
        return "redirect:/admin/news";
    }
    @PostMapping("/news/image/upload")
    public String uploadNewsImage(@RequestParam(name = "image_file") MultipartFile imageFile, RedirectAttributes ra) {
        try {
            if (imageFile == null || imageFile.isEmpty()) { ra.addFlashAttribute("error", "Please choose an image to upload."); return "redirect:/admin/news"; }
            String stored = fileStorageService.store(imageFile, "content", "content");
            ra.addFlashAttribute("success", "Image added (available on all content): " + stored);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not upload image. Use jpg, jpeg, png, gif, or webp.");
        }
        return "redirect:/admin/news";
    }
    @GetMapping("/news/{id}/update")
    public String updateNewsPage(@PathVariable String id, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Use the Update form on the News page to edit all details, then click Save Update.");
        return "redirect:/admin/news";
    }

    @PostMapping("/news/{id}/update")
    public String updateNews(@PathVariable String id,
                             @RequestParam String title,
                             @RequestParam(required = false) String content,
                             @RequestParam(required = false, defaultValue = "active") String status,
                             @RequestParam(required = false) String image,
                             @RequestParam(required = false) String image_select,
                             @RequestParam(required = false, name = "image_file") MultipartFile imageFile,
                             RedirectAttributes ra) throws Exception {
        News n = newsRepository.findById(id).orElse(null);
        if (n == null) { ra.addFlashAttribute("error", "News not found."); return "redirect:/admin/news"; }
        if (title == null || title.isBlank()) { ra.addFlashAttribute("error", "News title is required."); return "redirect:/admin/news"; }
        n.setTitle(title.trim());
        n.setContent(content != null ? content.trim() : null);
        n.setStatus(status == null || status.isBlank() ? "active" : status.trim());
        String resolved = resolveContentImage(imageFile, image_select, image, "news");
        if (resolved != null && !resolved.isBlank()) n.setImage(resolved);
        newsRepository.save(n);
        ra.addFlashAttribute("success", "News updated: " + n.getTitle());
        return "redirect:/admin/news";
    }

    @PostMapping("/news/{id}/delete")
    public String deleteNews(@PathVariable String id, RedirectAttributes ra) {
        if (newsRepository.existsById(id)) { newsRepository.deleteById(id); ra.addFlashAttribute("success", "News deleted."); }
        else { ra.addFlashAttribute("error", "News not found."); }
        return "redirect:/admin/news";
    }

    @PostMapping("/passes/{id}/approve")
    public String approve(@PathVariable String id, HttpServletRequest request, RedirectAttributes ra) {
        final String[] emailTo = {null};
        final String[] passRef = {id};
        String baseUrl = "";
        try { baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(); } catch (Exception ignored) {}
        final String finalBaseUrl = baseUrl;
        passRepository.findById(id).ifPresent(p -> {
            p.setStatus("approved");
            boolean freeByTypeOrMethod = (p.getPaymentStatus() != null && java.util.Set.of("not_required", "free", "waived", "na", "n/a").contains(p.getPaymentStatus().toLowerCase()))
                    || (p.getPaymentMethod() != null && (p.getPaymentMethod().toLowerCase().contains("trust") || p.getPaymentMethod().toLowerCase().contains("free")));
            // Normalize total for non-free passes (amount per person * persons)
            if (!freeByTypeOrMethod && p.getAmount() != null && p.getNoOfPersons() != null && p.getNoOfPersons() > 0) {
                double calc = p.getAmount() * p.getNoOfPersons();
                if (p.getTotalAmount() == null || Math.abs(p.getTotalAmount() - calc) > 0.001) p.setTotalAmount(calc);
            }
            boolean freeOrNoPayment = freeByTypeOrMethod || (p.getTotalAmount() != null && p.getTotalAmount() <= 0.0);
            if (freeOrNoPayment) {
                if (p.getPaymentStatus() == null || p.getPaymentStatus().isBlank() || "pending".equalsIgnoreCase(p.getPaymentStatus())) {
                    p.setPaymentStatus("not_required");
                }
            } else {
                p.setPaymentStatus("paid");
                p.setPaymentDate(java.time.Instant.now());
            }
            try { qrCodeService.ensurePassQr(p, finalBaseUrl); } catch (Exception ignored) {}
            passRepository.save(p);
            passRef[0] = p.getPassId() != null ? p.getPassId() : p.getId();
            notify(p.getUserId(), "Pass Approved", "Your Darshan Pass " + (p.getPassId() != null ? p.getPassId() : "") + " has been approved (formality checkup completed).", "pass");
            emailTo[0] = resolveUserEmail(p.getUserId(), p.getEmail());
            String name = resolveUserName(p.getUserId(), p.getDevoteeName());
            String profilePic = null;
            try { if (p.getUserId() != null) profilePic = userRepository.findById(p.getUserId()).map(com.temple.erp.model.User::getProfilePic).orElse(null); } catch (Exception ignored) {}
            if (emailTo[0] != null && !emailTo[0].isBlank()) {
                final String to = emailTo[0];
                final String nameForMail = name;
                final String profilePicForMail = profilePic;
                final com.temple.erp.model.DarshanPass passForMail = p;
                final String passIdForMail = passRef[0];
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        byte[] pdf = pdfService.generatePassPdf(passForMail, nameForMail, profilePicForMail);
                        boolean sent = emailService.sendPdf(to,
                                "Temple ERP - Darshan Pass " + passIdForMail + " (Approved)",
                                "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour Darshan Pass " + passIdForMail + " has been approved by admin. Your pass PDF is attached.\n\n- Temple ERP",
                                pdf, "Temple Darshan Pass - original pass.pdf");
                        if (!sent) org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Pass approve email not sent for {} to {}: {}", passIdForMail, to, emailService.statusHint(to));
                    } catch (Exception ex) {
                        org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Background pass approve email failed for {}: {}", passIdForMail, ex.getMessage());
                    }
                });
            }
        });
        String msg = "Pass approved (formality checkup completed).";
        if (emailTo[0] != null && !emailTo[0].isBlank()) msg += " Pass PDF will be emailed to " + emailTo[0] + " shortly.";
        else msg += " PDF email not sent (" + emailService.statusHint(emailTo[0]) + ").";
        ra.addFlashAttribute("success", msg);
        return "redirect:/admin/passes";
    }
    @PostMapping("/passes/{id}/reject")
    public String reject(@PathVariable String id, RedirectAttributes ra) {
        passRepository.findById(id).ifPresent(p -> {
            p.setStatus("rejected"); passRepository.save(p);
            notify(p.getUserId(), "Pass Rejected", "Your Darshan Pass " + (p.getPassId() != null ? p.getPassId() : "") + " was rejected. Please contact administration for details.", "pass");
        });
        ra.addFlashAttribute("success", "Pass rejected"); return "redirect:/admin/passes";
    }

    private String sendPassPdfInternal(com.temple.erp.model.DarshanPass p) {
        String passRef = p.getPassId() != null ? p.getPassId() : p.getId();
        String emailTo = resolveUserEmail(p.getUserId(), p.getEmail());
        try {
            if (p.getStatus() != null && "approved".equalsIgnoreCase(p.getStatus())) {
                try { String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(); qrCodeService.ensurePassQr(p, baseUrl); passRepository.save(p); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        String name = resolveUserName(p.getUserId(), p.getDevoteeName());
        String profilePic = null;
        try { if (p.getUserId() != null) profilePic = userRepository.findById(p.getUserId()).map(com.temple.erp.model.User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        if (emailTo == null || emailTo.isBlank()) {
            return "Pass PDF for " + passRef + " not sent (" + emailService.statusHint(emailTo) + "). Ensure the user profile/email is set and Admin Email Settings are configured.";
        }
        final String to = emailTo;
        final String nameForMail = name;
        final String profilePicForMail = profilePic;
        final String passRefForMail = passRef;
        final com.temple.erp.model.DarshanPass passForMail = p;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                byte[] pdf = pdfService.generatePassPdf(passForMail, nameForMail, profilePicForMail);
                String status = passForMail.getStatus() != null ? passForMail.getStatus() : "pending";
                boolean sent = emailService.sendPdf(to,
                        "Temple ERP - Darshan Pass " + passRefForMail + " (" + status + ")",
                        "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour Darshan Pass " + passRefForMail + " PDF is attached (status: " + status + ").\n\n- Temple ERP",
                        pdf, "Temple Darshan Pass - original pass.pdf");
                if (!sent) org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Pass PDF email not sent for {} to {}: {}", passRefForMail, to, emailService.statusHint(to));
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Background pass PDF email failed for {}: {}", passRefForMail, ex.getMessage());
            }
        });
        return "Pass PDF for " + passRef + " will be emailed to " + to + " shortly.";
    }

    @PostMapping("/passes/{id}/send-pdf")
    public String sendPassPdf(@PathVariable String id, RedirectAttributes ra) {
        passRepository.findById(id).ifPresentOrElse(p -> ra.addFlashAttribute("success", sendPassPdfInternal(p)),
                () -> ra.addFlashAttribute("error", "Pass not found."));
        return "redirect:/admin/passes";
    }

    @GetMapping("/passes/send-pdf")
    public String sendPassPdfByPassId(@RequestParam(required = false) String passId, RedirectAttributes ra) {
        if (passId == null || passId.isBlank()) {
            ra.addFlashAttribute("error", "Pass ID is required (e.g. ?passId=DSP...).");
            return "redirect:/admin/passes";
        }
        passRepository.findByPassId(passId.trim()).ifPresentOrElse(p -> ra.addFlashAttribute("success", sendPassPdfInternal(p)),
                () -> ra.addFlashAttribute("error", "Pass not found: " + passId.trim()));
        return "redirect:/admin/passes";
    }
    @PostMapping("/bookings/{id}/confirm")
    public String confirmBooking(@PathVariable String id, RedirectAttributes ra) {
        final String[] emailTo = {null};
        final String[] tokenRef = {id};
        bookingRepository.findById(id).ifPresent(b -> {
            b.setBookingStatus("Approved");
            if (b.getPaymentStatus() == null || b.getPaymentStatus().isBlank() || "pending".equalsIgnoreCase(b.getPaymentStatus())) {
                b.setPaymentStatus("Paid");
                b.setPaymentDate(java.time.Instant.now());
            }
            bookingRepository.save(b);
            tokenRef[0] = b.getBookingId() != null ? b.getBookingId() : b.getId();
            notify(b.getUserId(), "Room Booking Approved", "Your room booking " + tokenRef[0] + " has been approved by admin. Your token is now available.", "booking");
            emailTo[0] = resolveUserEmail(b.getUserId(), b.getEmail());
            String name = resolveUserName(b.getUserId(), b.getGuestName());
            String profilePic = null;
            try { if (b.getUserId() != null) profilePic = userRepository.findById(b.getUserId()).map(com.temple.erp.model.User::getProfilePic).orElse(null); } catch (Exception ignored) {}
            if (emailTo[0] != null && !emailTo[0].isBlank()) {
                final String to = emailTo[0];
                final String nameForMail = name;
                final String profilePicForMail = profilePic;
                final com.temple.erp.model.RoomBooking bookingForMail = b;
                final String tokenForMail = tokenRef[0];
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        byte[] pdf = pdfService.generateBookingTokenPdf(bookingForMail, nameForMail, profilePicForMail);
                        boolean sent = emailService.sendPdf(to,
                                "Temple ERP - Room Booking Token " + tokenForMail + " (Approved)",
                                "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour room booking " + tokenForMail + " has been approved by admin. Your token PDF is attached.\n\n- Temple ERP",
                                pdf, "room-booking-token-" + tokenForMail + ".pdf");
                        if (!sent) org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Booking approve email not sent for {} to {}: {}", tokenForMail, to, emailService.statusHint(to));
                    } catch (Exception ex) {
                        org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Background booking approve email failed for {}: {}", tokenForMail, ex.getMessage());
                    }
                });
            }
        });
        String msg = "Booking approved. Status updated and token is now available for the user.";
        if (emailTo[0] != null && !emailTo[0].isBlank()) msg += " Token PDF will be emailed to " + emailTo[0] + " shortly.";
        else msg += " PDF email not sent (" + emailService.statusHint(emailTo[0]) + ").";
        ra.addFlashAttribute("success", msg);
        return "redirect:/admin/bookings";
    }

    private String sendBookingTokenPdfInternal(com.temple.erp.model.RoomBooking b) {
        String tokenRef = b.getBookingId() != null ? b.getBookingId() : b.getId();
        String emailTo = resolveUserEmail(b.getUserId(), b.getEmail());
        String name = resolveUserName(b.getUserId(), b.getGuestName());
        String profilePic = null;
        try { if (b.getUserId() != null) profilePic = userRepository.findById(b.getUserId()).map(com.temple.erp.model.User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        if (emailTo == null || emailTo.isBlank()) {
            return "Booking token PDF for " + tokenRef + " not sent (" + emailService.statusHint(emailTo) + "). Ensure the user profile/email is set and Admin Email Settings are configured.";
        }
        final String to = emailTo;
        final String nameForMail = name;
        final String profilePicForMail = profilePic;
        final String tokenRefForMail = tokenRef;
        final com.temple.erp.model.RoomBooking bookingForMail = b;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                byte[] pdf = pdfService.generateBookingTokenPdf(bookingForMail, nameForMail, profilePicForMail);
                String status = bookingForMail.getBookingStatus() != null ? bookingForMail.getBookingStatus() : "pending";
                boolean sent = emailService.sendPdf(to,
                        "Temple ERP - Room Booking Token " + tokenRefForMail + " (" + status + ")",
                        "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour room booking token " + tokenRefForMail + " PDF is attached (status: " + status + ").\n\n- Temple ERP",
                        pdf, "room-booking-token-" + tokenRefForMail + ".pdf");
                if (!sent) org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Booking token email not sent for {} to {}: {}", tokenRefForMail, to, emailService.statusHint(to));
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Background booking token email failed for {}: {}", tokenRefForMail, ex.getMessage());
            }
        });
        return "Booking token PDF for " + tokenRef + " will be emailed to " + to + " shortly.";
    }

    @PostMapping("/bookings/{id}/send-pdf")
    public String sendBookingTokenPdf(@PathVariable String id, RedirectAttributes ra) {
        bookingRepository.findById(id).ifPresentOrElse(
                b -> ra.addFlashAttribute("success", sendBookingTokenPdfInternal(b)),
                () -> ra.addFlashAttribute("error", "Booking not found."));
        return "redirect:/admin/bookings";
    }

    @GetMapping("/bookings/{id}/send-pdf")
    public String sendBookingTokenPdfGet(@PathVariable String id, RedirectAttributes ra) {
        return sendBookingTokenPdf(id, ra);
    }

    private String sendRestaurantTokenPdfInternal(com.temple.erp.model.RestaurantOrder o) {
        String tokenRef = o.getOrderId() != null ? o.getOrderId() : o.getId();
        String emailTo = resolveUserEmail(o.getUserId(), null);
        String name = resolveUserName(o.getUserId(), o.getName());
        String profilePic = null;
        try { if (o.getUserId() != null) profilePic = userRepository.findById(o.getUserId()).map(com.temple.erp.model.User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        if (emailTo == null || emailTo.isBlank()) {
            return "Restaurant token PDF for " + tokenRef + " not sent (" + emailService.statusHint(emailTo) + "). Ensure the user profile/email is set and Admin Email Settings are configured.";
        }
        final String to = emailTo;
        final String nameForMail = name;
        final String profilePicForMail = profilePic;
        final String tokenRefForMail = tokenRef;
        final com.temple.erp.model.RestaurantOrder orderForMail = o;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                byte[] pdf = pdfService.generateRestaurantTokenPdf(orderForMail, nameForMail, profilePicForMail);
                String status = orderForMail.getStatus() != null ? orderForMail.getStatus() : "pending";
                boolean sent = emailService.sendPdf(to,
                        "Temple ERP - Restaurant Order Token " + tokenRefForMail + " (" + status + ")",
                        "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour restaurant order token " + tokenRefForMail + " PDF is attached (status: " + status + ").\n\n- Temple ERP",
                        pdf, "restaurant-token-" + tokenRefForMail + ".pdf");
                if (!sent) org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Restaurant token email not sent for {} to {}: {}", tokenRefForMail, to, emailService.statusHint(to));
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Background restaurant token email failed for {}: {}", tokenRefForMail, ex.getMessage());
            }
        });
        return "Restaurant token PDF for " + tokenRef + " will be emailed to " + to + " shortly.";
    }

    @PostMapping("/restaurant/{id}/send-pdf")
    public String sendRestaurantTokenPdf(@PathVariable String id, RedirectAttributes ra) {
        orderRepository.findById(id).ifPresentOrElse(
                o -> ra.addFlashAttribute("success", sendRestaurantTokenPdfInternal(o)),
                () -> ra.addFlashAttribute("error", "Order not found."));
        return "redirect:/admin/restaurant";
    }

    @GetMapping("/restaurant/{id}/send-pdf")
    public String sendRestaurantTokenPdfGet(@PathVariable String id, RedirectAttributes ra) {
        return sendRestaurantTokenPdf(id, ra);
    }

    private String sendDonationReceiptPdfInternal(com.temple.erp.model.Donation d) {
        String receiptRef = d.getReceiptNo() != null ? d.getReceiptNo() : d.getId();
        String emailTo = resolveUserEmail(d.getUserId(), d.getEmail());
        String name = resolveUserName(d.getUserId(), d.getName());
        String profilePic = null;
        try { if (d.getUserId() != null) profilePic = userRepository.findById(d.getUserId()).map(com.temple.erp.model.User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        if (emailTo == null || emailTo.isBlank()) {
            return "Donation receipt PDF for " + receiptRef + " not sent (" + emailService.statusHint(emailTo) + "). Ensure the donor/user email is set and Admin Email Settings are configured.";
        }
        final String to = emailTo;
        final String nameForMail = name;
        final String profilePicForMail = profilePic;
        final String receiptRefForMail = receiptRef;
        final com.temple.erp.model.Donation donationForMail = d;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                byte[] pdf = pdfService.generateDonationReceiptPdf(donationForMail, nameForMail, profilePicForMail);
                String status = donationForMail.getPaymentStatus() != null ? donationForMail.getPaymentStatus() : "pending";
                boolean sent = emailService.sendPdf(to,
                        "Temple ERP - Donation Receipt " + receiptRefForMail + " (" + status + ")",
                        "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour donation receipt " + receiptRefForMail + " PDF is attached (status: " + status + ").\n\n- Temple ERP",
                        pdf, "donation-receipt-" + receiptRefForMail + ".pdf");
                if (!sent) org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Donation receipt email not sent for {} to {}: {}", receiptRefForMail, to, emailService.statusHint(to));
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(AdminWebController.class).warn("Background donation receipt email failed for {}: {}", receiptRefForMail, ex.getMessage());
            }
        });
        return "Donation receipt PDF for " + receiptRef + " will be emailed to " + to + " shortly.";
    }

    @PostMapping("/donations/{id}/send-pdf")
    public String sendDonationReceiptPdf(@PathVariable String id, RedirectAttributes ra) {
        donationRepository.findById(id).ifPresentOrElse(
                d -> ra.addFlashAttribute("success", sendDonationReceiptPdfInternal(d)),
                () -> ra.addFlashAttribute("error", "Donation not found."));
        return "redirect:/admin/donations";
    }

    @GetMapping("/donations/{id}/send-pdf")
    public String sendDonationReceiptPdfGet(@PathVariable String id, RedirectAttributes ra) {
        return sendDonationReceiptPdf(id, ra);
    }

    @PostMapping("/bookings/{id}/reject")
    public String rejectBooking(@PathVariable String id, RedirectAttributes ra) {
        bookingRepository.findById(id).ifPresent(b -> {
            b.setBookingStatus("Rejected");
            bookingRepository.save(b);
            notify(b.getUserId(), "Room Booking Rejected", "Your room booking " + (b.getBookingId() != null ? b.getBookingId() : b.getId()) + " was rejected by admin. Please contact administration for details.", "booking");
        });
        ra.addFlashAttribute("success", "Booking rejected. Status updated."); return "redirect:/admin/bookings";
    }

    @GetMapping("/mail-settings")
    public String mailSettings(Model model) {
        com.temple.erp.model.MailSetting ms = emailService.getSettingsForAdmin();
        model.addAttribute("mailSetting", ms);
        model.addAttribute("activePage", "mail-settings");
        model.addAttribute("mailEnabled", ms != null && ms.isEnabled());
        return "admin/mail-settings";
    }

    @PostMapping("/mail-settings")
    public String saveMailSettings(@RequestParam(required = false, defaultValue = "false") boolean enabled,
                                   @RequestParam(required = false) String fromEmail,
                                   @RequestParam(required = false) String host,
                                   @RequestParam(required = false) Integer port,
                                   @RequestParam(required = false) String username,
                                   @RequestParam(required = false) String password,
                                   @RequestParam(required = false) String testEmail,
                                   @RequestParam(required = false) String action,
                                   RedirectAttributes ra) {
        try {
            com.temple.erp.model.MailSetting ms = new com.temple.erp.model.MailSetting();
            ms.setEnabled(enabled);
            ms.setFromEmail(fromEmail);
            ms.setHost(host);
            ms.setPort(port);
            ms.setUsername(username);
            ms.setPassword(password); // blank = keep existing
            String adminName = "admin";
            try {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getName() != null) adminName = auth.getName();
            } catch (Exception ignored) {}
            emailService.saveSettings(ms, adminName);
            if ("test".equalsIgnoreCase(action)) {
                String to = (testEmail != null && !testEmail.isBlank()) ? testEmail.trim()
                        : (username != null && !username.isBlank() ? username.trim() : null);
                String result = emailService.testSend(to);
                if (result != null && result.toLowerCase().contains("success")) ra.addFlashAttribute("success", "Settings saved. " + result);
                else ra.addFlashAttribute("error", "Settings saved, but test failed: " + result);
            } else {
                ra.addFlashAttribute("success", "Email settings saved. PDF pass/token emails will send directly when enabled and configured (no restart required).");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not save email settings: " + e.getMessage());
        }
        return "redirect:/admin/mail-settings";
    }
}
