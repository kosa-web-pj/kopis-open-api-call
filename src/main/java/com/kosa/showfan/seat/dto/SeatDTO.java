package com.kosa.showfan.seat.dto;

import lombok.Data;

@Data
public class SeatDTO {
    private Long seatId;
    private String showId;
    private String seatName;
    private Integer seatPrice;
}