package com.temple.erp.model;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "darshanpasses")
public class DarshanPass {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("pass_id")
    private String passId;

    @Field("user_id")
    private String userId;

    @Field("devotee_name")
    private String devoteeName;

    private String phone;
    private String email;
    private String gender;
    private String address;

    @Field("visit_date")
    private LocalDate visitDate;

    @Field("visit_time")
    private String visitTime;

    @Field("no_of_persons")
    private Integer noOfPersons = 1;

    @Field("pass_type")
    private String passType;

    private Double amount = 0.0;

    @Field("total_amount")
    private Double totalAmount = 0.0;

    @Field("payment_method")
    private String paymentMethod;

    @Field("transaction_id")
    private String transactionId;

    @Field("payment_status")
    private String paymentStatus = "pending";

    @Field("payment_date")
    private Instant paymentDate;

    private String status = "pending";

    @Field("aadhar_number")
    private String aadharNumber;

    @Field("aadhar_card")
    private String aadharCard;

    @Field("special_requests")
    private String specialRequests;

    @Field("created_by")
    private String createdBy;

    @Field("qr_code")
    private String qrCode; // stored QR image filename (permanent)

    @Field("qr_token")
    private String qrToken; // permanent unique verification token

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPassId() { return passId; }
    public void setPassId(String passId) { this.passId = passId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDevoteeName() { return devoteeName; }
    public void setDevoteeName(String devoteeName) { this.devoteeName = devoteeName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public String getVisitTime() { return visitTime; }
    public void setVisitTime(String visitTime) { this.visitTime = visitTime; }
    public Integer getNoOfPersons() { return noOfPersons; }
    public void setNoOfPersons(Integer noOfPersons) { this.noOfPersons = noOfPersons; }
    public String getPassType() { return passType; }
    public void setPassType(String passType) { this.passType = passType; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public Instant getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Instant paymentDate) { this.paymentDate = paymentDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }
    public String getAadharCard() { return aadharCard; }
    public void setAadharCard(String aadharCard) { this.aadharCard = aadharCard; }
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
