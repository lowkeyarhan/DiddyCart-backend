package com.diddycart.dto.address;

import com.diddycart.enums.AddressLabel;
import lombok.Data;
import java.io.Serializable;

@Data
public class AddressSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private AddressLabel label;
    private String city;
    private String state;
    private String country;
    private String pincode;
}