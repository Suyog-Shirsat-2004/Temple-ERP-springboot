package com.temple.erp.model;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "roombookings")
public class RoomBooking {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("booking_id")
    private String bookingId;

    @Field("user_id")
    private String userId;

    @Field("room_id")
    private String roomId;

    @Field("guest_name")
    private String guestName;
    private String phone;
    private String email;

    @Field("check_in")
    private LocalDate checkIn;

    @Field("check_out")
    private LocalDate checkOut;

    @Field("room_type")
    private String roomType;

    @Field("no_of_rooms")
    private Integer noOfRooms = 1;

    @Field("no_of_guests")
    private Integer noOfGuests;

    @Field("total_amount")
    private Double totalAmount;

    @Field("payment_status")
    private String paymentStatus = "Pending";

    @Field("booking_status")
    private String bookingStatus = "Pending";

    @Field("payment_method")
    private String paymentMethod;

    @Field("payment_date")
    private Instant paymentDate;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public Integer getNoOfRooms() { return noOfRooms; }
    public void setNoOfRooms(Integer noOfRooms) { this.noOfRooms = noOfRooms; }
    public Integer getNoOfGuests() { return noOfGuests; }
    public void setNoOfGuests(Integer noOfGuests) { this.noOfGuests = noOfGuests; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Instant getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Instant paymentDate) { this.paymentDate = paymentDate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
