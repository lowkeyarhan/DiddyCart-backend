package com.diddycart.dto.address;

import com.diddycart.enums.AddressLabel;
import lombok.Data;

@Data
public class AddressResponse {
    private Long id;
    private AddressLabel label;
    private String street;
    private String landmark;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String phone;
    private String alternatePhone;
}
