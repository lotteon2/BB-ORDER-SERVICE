INSERT INTO order_group (order_group_id, user_id, created_at, updated_at)
VALUES ('그룹주문id', 1, DATEADD('DAY', -4, NOW()), DATEADD('DAY', -4, NOW())),
       ('그룹주문id2', 1, DATEADD('DAY', -3, NOW()), DATEADD('DAY', -3, NOW())),
       ('그룹주문id3', 1, DATEADD('DAY', -2, NOW()), DATEADD('DAY', -2, NOW())),
       ('그룹주문id4', 1, DATEADD('DAY', -1, NOW()), DATEADD('DAY', -1, NOW()));

INSERT INTO order_delivery (order_delivery_id, order_group_id, store_id, delivery_id,
                            order_delivery_total_amount,
                            order_delivery_coupon_amount, order_delivery_status, created_at,
                            updated_at)
VALUES ('가게주문id', '그룹주문id', 1, 1, 39800, 0, 'PENDING', DATEADD('DAY', -4, NOW()),
        DATEADD('DAY', -4, NOW())),
       ('가게주문id2', '그룹주문id2', 1, 1, 39800, 0, 'PENDING', DATEADD('DAY', -3, NOW()),
        DATEADD('DAY', -3, NOW())),
       ('가게주문id3', '그룹주문id3', 1, 1, 39800, 0, 'PENDING', DATEADD('DAY', -2, NOW()),
        DATEADD('DAY', -2, NOW())),
       ('가게주문id4', '그룹주문id4', 1, 1, 39800, 0, 'PENDING', DATEADD('DAY', -1, NOW()),
        DATEADD('DAY', -1, NOW()));

INSERT INTO order_delivery_product (order_product_id, product_id, order_product_price,
                                    order_product_quantity, review_status, card_status,
                                    order_delivery_id, created_at, updated_at)
VALUES (1, '꽃id-1', 39800, 1, 'DISABLED', 'ABLE', '가게주문id', DATEADD('DAY', -4, NOW()),
        DATEADD('DAY', -4, NOW())),
       (2, '꽃id-2', 7500, 1, 'DONE', 'DISABLED', '가게주문id', DATEADD('DAY', -4, NOW()),
        DATEADD('DAY', -4, NOW())),
       (3, '꽃id-3', 7500, 1, 'DONE', 'DISABLED', '가게주문id2', DATEADD('DAY', -3, NOW()),
        DATEADD('DAY', -3, NOW())),
       (4, '꽃id-3', 7500, 1, 'DONE', 'DISABLED', '가게주문id3', DATEADD('DAY', -2, NOW()),
        DATEADD('DAY', -2, NOW())),
       (5, '꽃id-3', 7500, 1, 'DONE', 'DISABLED', '가게주문id4', DATEADD('DAY', -1, NOW()),
        DATEADD('DAY', -1, NOW()));

INSERT INTO order_subscription (order_subscription_id, created_at, is_deleted, updated_at, delivery_day, delivery_id, payment_date, phone_number, product_name, product_price, store_id, subscription_product_id, user_id)
VALUES
    ('주문_구독_id_1', DATEADD('DAY', -4, NOW()), 0, DATEADD('DAY', -4, NOW()), DATEADD('DAY', 1, NOW()), 1, DATEADD('DAY', 30, NOW()), '010-1234-5678', '상품 1', 10000, 1, 'A123', 1),
    ('주문_구독_id_2', DATEADD('DAY', -3, NOW()), 0, DATEADD('DAY', -3, NOW()), DATEADD('DAY', 0, NOW()), 2, DATEADD('DAY', 30, NOW()), '010-2345-6789', '상품 2', 20000, 2, 'B456', 2);
