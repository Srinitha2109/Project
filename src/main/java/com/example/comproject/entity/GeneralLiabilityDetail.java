package com.example.comproject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "gl_application_details")
public class GeneralLiabilityDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Premises
    private Boolean customersVisitPremises;
    private String dailyVisitorsRange; // Less than 50, Between 50 and 200, More than 200
    private Boolean hasParkingLot;

    // Products
    private Boolean sellPhysicalProducts;
    private Boolean productsContactFood;
    private Boolean productsForChildren;

    // Work at Customer Sites
    private Boolean workAtCustomerSites;
    private Boolean workRemainsAfterCompletion;
    private Boolean useSubcontractors;

    // Claims History
    private Boolean hasPriorClaims;
    private Integer claimsCount;
    private BigDecimal totalClaimsAmount;

    // Coverage Selection
    private String coverageType; // Bodily Injury Only, Property Damage Only, Both
    private BigDecimal perOccurrenceLimit;
    private Boolean includeProductsCompletedOps;
    private Boolean includeAdvertisingInjury;

    @OneToOne(mappedBy = "glDetail")
    private PolicyApplication policyApplication;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Boolean getCustomersVisitPremises() { return customersVisitPremises; }
    public void setCustomersVisitPremises(Boolean customersVisitPremises) { this.customersVisitPremises = customersVisitPremises; }
    public String getDailyVisitorsRange() { return dailyVisitorsRange; }
    public void setDailyVisitorsRange(String dailyVisitorsRange) { this.dailyVisitorsRange = dailyVisitorsRange; }
    public Boolean getHasParkingLot() { return hasParkingLot; }
    public void setHasParkingLot(Boolean hasParkingLot) { this.hasParkingLot = hasParkingLot; }
    public Boolean getSellPhysicalProducts() { return sellPhysicalProducts; }
    public void setSellPhysicalProducts(Boolean sellPhysicalProducts) { this.sellPhysicalProducts = sellPhysicalProducts; }
    public Boolean getProductsContactFood() { return productsContactFood; }
    public void setProductsContactFood(Boolean productsContactFood) { this.productsContactFood = productsContactFood; }
    public Boolean getProductsForChildren() { return productsForChildren; }
    public void setProductsForChildren(Boolean productsForChildren) { this.productsForChildren = productsForChildren; }
    public Boolean getWorkAtCustomerSites() { return workAtCustomerSites; }
    public void setWorkAtCustomerSites(Boolean workAtCustomerSites) { this.workAtCustomerSites = workAtCustomerSites; }
    public Boolean getWorkRemainsAfterCompletion() { return workRemainsAfterCompletion; }
    public void setWorkRemainsAfterCompletion(Boolean workRemainsAfterCompletion) { this.workRemainsAfterCompletion = workRemainsAfterCompletion; }
    public Boolean getUseSubcontractors() { return useSubcontractors; }
    public void setUseSubcontractors(Boolean useSubcontractors) { this.useSubcontractors = useSubcontractors; }
    public Boolean getHasPriorClaims() { return hasPriorClaims; }
    public void setHasPriorClaims(Boolean hasPriorClaims) { this.hasPriorClaims = hasPriorClaims; }
    public Integer getClaimsCount() { return claimsCount; }
    public void setClaimsCount(Integer claimsCount) { this.claimsCount = claimsCount; }
    public BigDecimal getTotalClaimsAmount() { return totalClaimsAmount; }
    public void setTotalClaimsAmount(BigDecimal totalClaimsAmount) { this.totalClaimsAmount = totalClaimsAmount; }
    public String getCoverageType() { return coverageType; }
    public void setCoverageType(String coverageType) { this.coverageType = coverageType; }
    public BigDecimal getPerOccurrenceLimit() { return perOccurrenceLimit; }
    public void setPerOccurrenceLimit(BigDecimal perOccurrenceLimit) { this.perOccurrenceLimit = perOccurrenceLimit; }
    public Boolean getIncludeProductsCompletedOps() { return includeProductsCompletedOps; }
    public void setIncludeProductsCompletedOps(Boolean includeProductsCompletedOps) { this.includeProductsCompletedOps = includeProductsCompletedOps; }
    public Boolean getIncludeAdvertisingInjury() { return includeAdvertisingInjury; }
    public void setIncludeAdvertisingInjury(Boolean includeAdvertisingInjury) { this.includeAdvertisingInjury = includeAdvertisingInjury; }
    public PolicyApplication getPolicyApplication() { return policyApplication; }
    public void setPolicyApplication(PolicyApplication policyApplication) { this.policyApplication = policyApplication; }
}
