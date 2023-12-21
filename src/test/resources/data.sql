INSERT INTO order_group (order_group_id, user_id, created_at, updated_at)
VALUES ('그룹주문id', 1, '2021-01-01 00:00:00', '2021-01-01 00:00:00');
--        ('group2', 2, '2021-02-01 00:00:00', '2021-02-01 00:00:00');

INSERT INTO order_delivery (order_delivery_id, order_group_id, store_id, delivery_id,
                            order_delivery_total_amount,
                            order_delivery_coupon_amount, order_delivery_status, created_at,
                            updated_at)
VALUES ('가게주문id', '그룹주문id', 1, 1, 39800, 0, 'PENDING', '2021-01-01 00:00:00',
        '2021-01-01 00:00:00');
--        ('delivery2', 'group1', 1, 101, 15000, 750, 'PENDING', '2021-01-01 00:00:00',
--         '2021-01-01 00:00:00');

INSERT INTO order_delivery_product (order_product_id, product_id, order_product_price,
                                    order_product_quantity, review_status, card_status,
                                    order_delivery_id, created_at, updated_at)
VALUES (1, '꽃id-1', 39800, 1, 'DISABLED', 'ABLE', '가게주문id', '2021-01-01 00:00:00',
        '2021-01-01 00:00:00'),
       (2, '꽃id-2', 7500, 1, 'DONE', 'DISABLED', '가게주문id', '2021-01-01 00:00:00',
        '2021-01-01 00:00:00');