INSERT INTO order_group (order_group_id, user_id, created_at, updated_at)
VALUES ('그룹주문id', 1, '2023-12-20 00:00:00', '2023-12-20 00:00:00'),
       ('그룹주문id2', 1, '2023-12-18 00:00:00', '2023-12-18 00:00:00'),
       ('그룹주문id3', 1, '2023-12-17 00:00:00', '2023-12-17 00:00:00'),
       ('그룹주문id4', 1, '2023-12-16 00:00:00', '2023-12-16 00:00:00');

INSERT INTO order_delivery (order_delivery_id, order_group_id, store_id, delivery_id,
                            order_delivery_total_amount,
                            order_delivery_coupon_amount, order_delivery_status, created_at,
                            updated_at)
VALUES ('가게주문id', '그룹주문id', 1, 1, 39800, 0, 'PENDING', '2023-12-20 00:00:00',
        '2023-12-20 00:00:00'),
       ('가게주문id2', '그룹주문id2', 1, 1, 39800, 0, 'PENDING', '2023-12-18 00:00:00',
        '2023-12-18 00:00:00'),
       ('가게주문id3', '그룹주문id3', 1, 1, 39800, 0, 'PENDING', '2023-12-17 00:00:00',
        '2023-12-17 00:00:00'),
       ('가게주문id4', '그룹주문id4', 1, 1, 39800, 0, 'PENDING', '2023-12-16 00:00:00',
        '2023-12-16 00:00:00');

INSERT INTO order_delivery_product (order_product_id, product_id, order_product_price,
                                    order_product_quantity, review_status, card_status,
                                    order_delivery_id, created_at, updated_at)
VALUES (1, '꽃id-1', 39800, 1, 'DISABLED', 'ABLE', '가게주문id', '2023-12-20 00:00:00',
        '2023-12-20 00:00:00'),
       (2, '꽃id-2', 7500, 1, 'DONE', 'DISABLED', '가게주문id', '2023-12-20 00:00:00',
        '2023-12-20 00:00:00'),
       (3, '꽃id-3', 7500, 1, 'DONE', 'DISABLED', '가게주문id2', '2023-12-18 00:00:00',
        '2023-12-18 00:00:00'),
       (4, '꽃id-3', 7500, 1, 'DONE', 'DISABLED', '가게주문id3', '2023-12-17 00:00:00',
        '2023-12-17 00:00:00'),
       (5, '꽃id-3', 7500, 1, 'DONE', 'DISABLED', '가게주문id4', '2023-12-16 00:00:00',
        '2023-12-16 00:00:00');