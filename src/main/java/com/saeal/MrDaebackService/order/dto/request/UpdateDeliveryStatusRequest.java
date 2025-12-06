package com.saeal.MrDaebackService.order.dto.request;

import com.saeal.MrDaebackService.order.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryStatusRequest {
    @NotNull
    private DeliveryStatus deliveryStatus;
}





