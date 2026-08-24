-- 种子数据：三张优惠券模板（联调用）——两张满减 + 一张折扣（9折最高抵20元，最低消费50元）
INSERT INTO coupon (name, type, face_amount, min_amount, discount_rate, max_deduct_amount,
                    total_count, taken_count, expire_time, status) VALUES
('满100减10', 'FULL_CUT', 1000, 10000, 0, 0, 100, 0, '2099-12-31 23:59:59', 1),
('满50减5',   'FULL_CUT', 500,  5000,  0, 0, 100, 0, '2099-12-31 23:59:59', 1),
('9折券·最高抵20', 'DISCOUNT', 0, 5000, 90, 2000, 100, 0, '2099-12-31 23:59:59', 1);
