package kr.bb.order.dto;

import java.sql.Date;

public interface WeeklySalesDto {
    Date getDate();
    Long getTotalSales();
}